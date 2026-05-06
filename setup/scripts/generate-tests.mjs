#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";

const [rootDir, policyPath, resultPath, reportPath] = process.argv.slice(2);

if (!rootDir || !policyPath || !resultPath || !reportPath) {
  console.error("Usage: generate-tests.mjs <rootDir> <policyPath> <resultPath> <reportPath>");
  process.exit(1);
}

const brunoGeneratedDir = path.join(rootDir, "tests/bruno/api/generated");
const brunoCollectionDir = path.join(rootDir, "tests/bruno/api");
const k6GeneratedDir = path.join(rootDir, "tests/k6/generated");
const k6EnvPath = path.join(rootDir, "tests/k6/env.js");
const k6ReportPath = path.join(rootDir, "tests/k6/report.js");
const defaultOpenCollection = `info:
  name: API Test Collection
  type: collection
  version: 1
`;

function parseScalar(value) {
  const trimmed = value.trim();

  if (trimmed === "true") return true;
  if (trimmed === "false") return false;
  if (/^-?\d+$/.test(trimmed)) return Number(trimmed);
  if (
    (trimmed.startsWith('"') && trimmed.endsWith('"')) ||
    (trimmed.startsWith("'") && trimmed.endsWith("'"))
  ) {
    try {
      return JSON.parse(trimmed);
    } catch {
      return trimmed.slice(1, -1);
    }
  }

  return trimmed;
}

function parsePolicyEndpoints(policyText) {
  const endpointsMatch = policyText.match(/^endpoints:\s*(?:\[\])?\s*$/m);
  if (!endpointsMatch) return [];

  const lines = policyText.slice(endpointsMatch.index + endpointsMatch[0].length).split(/\r?\n/);
  const endpoints = [];
  let current = null;
  let inSource = false;

  for (const line of lines) {
    if (/^\S/.test(line)) break;

    const startMatch = line.match(/^  -\s+([A-Za-z][A-Za-z0-9_-]*):\s*(.*)$/);
    if (startMatch) {
      if (current) endpoints.push(current);
      current = {};
      inSource = false;
      current[startMatch[1]] = parseScalar(startMatch[2]);
      continue;
    }

    if (!current) continue;

    const keyMatch = line.match(/^    ([A-Za-z][A-Za-z0-9_-]*):\s*(.*)$/);
    if (keyMatch) {
      const [, key, value] = keyMatch;
      if (key === "source" && value === "") {
        current.source = {};
        inSource = true;
      } else {
        current[key] = parseScalar(value);
        inSource = false;
      }
      continue;
    }

    const sourceMatch = line.match(/^      ([A-Za-z][A-Za-z0-9_-]*):\s*(.*)$/);
    if (sourceMatch && inSource) {
      current.source ??= {};
      current.source[sourceMatch[1]] = parseScalar(sourceMatch[2]);
    }
  }

  if (current) endpoints.push(current);
  return endpoints.filter((endpoint) => endpoint.method && endpoint.path && !endpoint.stale);
}

function ensureGeneratedPath(targetPath, ownerDir) {
  const resolvedTarget = path.resolve(targetPath);
  const resolvedOwner = path.resolve(ownerDir);

  if (!resolvedTarget.startsWith(`${resolvedOwner}${path.sep}`) && resolvedTarget !== resolvedOwner) {
    throw new Error(`Refusing to write outside generated directory: ${targetPath}`);
  }
}

function resetDir(dirPath) {
  ensureGeneratedPath(dirPath, path.dirname(dirPath));
  fs.rmSync(dirPath, { recursive: true, force: true });
  fs.mkdirSync(dirPath, { recursive: true });
}

function writeFile(filePath, content, ownerDir) {
  ensureGeneratedPath(filePath, ownerDir);
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, content);
}

function writeIfMissing(filePath, content) {
  if (fs.existsSync(filePath)) return;

  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, content);
}

function removeDefaultLegacyOpenCollection() {
  const legacyPath = path.join(brunoCollectionDir, "opencollection.yml");
  if (!fs.existsSync(legacyPath)) return;

  const current = fs.readFileSync(legacyPath, "utf8").trim();
  if (current === defaultOpenCollection.trim()) {
    fs.rmSync(legacyPath);
    return;
  }

  throw new Error("Refusing to generate .bru requests while a custom opencollection.yml exists. Move or migrate it before generating Bruno native requests.");
}

function ensureBrunoNativeCollection() {
  removeDefaultLegacyOpenCollection();

  writeIfMissing(path.join(brunoCollectionDir, "bruno.json"), `${JSON.stringify({
    version: "1",
    name: "API Test Collection",
    type: "collection",
    ignore: ["node_modules", ".git"],
  }, null, 2)}\n`);

  writeIfMissing(path.join(brunoCollectionDir, "collection.bru"), `meta {
  type: collection
}

auth {
  mode: none
}
`);
}

function slugFor(endpoint) {
  const cleanedPath = String(endpoint.path)
    .replace(/[{}]/g, "")
    .replace(/[^A-Za-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .toLowerCase();

  return `${String(endpoint.method).toLowerCase()}-${cleanedPath || "root"}`;
}

function brunoPath(endpointPath) {
  return String(endpointPath).replace(/\{([^}]+)\}/g, "{{$1}}");
}

function titleFor(endpoint) {
  return `${String(endpoint.method).toUpperCase()} ${endpoint.path}`;
}

function brunoFolderFor(endpoint) {
  if (endpoint.class === "smoke" && endpoint.reviewRequired === false) return "smoke";
  if (endpoint.class === "scenario") return "scenario";
  return "draft";
}

function tagsFor(endpoint, folder) {
  const tags = ["generated"];

  if (folder === "smoke") tags.push("smoke");
  if (folder === "scenario") tags.push("scenario", "draft");
  if (folder === "draft") {
    tags.push("draft");
    if (endpoint.class === "smoke") tags.push("smoke-candidate");
  }

  if (endpoint.reviewRequired) tags.push("requires-review");
  return tags;
}

function renderBruTags(tags) {
  if (tags.length === 0) return "";

  return `  tags: [
${tags.map((tag) => `    ${tag}`).join("\n")}
  ]
`;
}

function renderBrunoRequest(endpoint, seq, folder) {
  const method = String(endpoint.method).toLowerCase();
  const tags = tagsFor(endpoint, folder);
  const docs = [
    "Managed by setup-agent from tests/testing-policy.yml.",
    `Policy class: ${endpoint.class}`,
    `Confidence: ${endpoint.confidence}`,
    `Review required: ${endpoint.reviewRequired ? "true" : "false"}`,
    `Reason: ${endpoint.reason || ""}`,
    "Generated requests use auth: none to keep smoke execution stable. Move protected requests to manual/ and configure auth there.",
  ].join("\n");

  return `meta {
  name: ${titleFor(endpoint)}
  type: http
  seq: ${seq}
${renderBruTags(tags)}}

${method} {
  url: {{baseUrl}}${brunoPath(endpoint.path)}
  body: none
  auth: none
}

tests {
  test("returns a 2xx or 3xx response", function() {
    expect(res.getStatus()).to.be.within(200, 399);
  });
}

settings {
  encodeUrl: true
  timeout: 3000
  followRedirects: true
}

docs {
${docs.split("\n").map((line) => `  ${line}`).join("\n")}
}
`;
}

function renderFolder(name, seq) {
  return `meta {
  name: ${name}
  type: folder
  seq: ${seq}
}
`;
}

function isK6SafeSmoke(endpoint) {
  return endpoint.class === "smoke" &&
    endpoint.reviewRequired === false &&
    String(endpoint.method).toUpperCase() === "GET" &&
    !String(endpoint.path).includes("{");
}

function renderK6Smoke(endpoints) {
  const endpointArray = endpoints.map((endpoint) => {
    return `  { name: ${JSON.stringify(titleFor(endpoint))}, method: ${JSON.stringify(endpoint.method)}, path: ${JSON.stringify(endpoint.path)} }`;
  }).join(",\n");

  return `import { BASE_URL, VUS_COUNT, ITERATIONS } from "../env.js";
import { createPerformanceSummary } from "../report.js";
import http from "k6/http";
import { check, sleep } from "k6";

const SMOKE_ENDPOINTS = [
${endpointArray}
];

const ENDPOINT_THRESHOLDS = {};
for (const endpoint of SMOKE_ENDPOINTS) {
  ENDPOINT_THRESHOLDS[\`http_req_duration{name:\${endpoint.name}}\`] = ["p(95)<1000"];
}

export const options = {
  vus: VUS_COUNT,
  iterations: ITERATIONS,
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<1000"],
    ...ENDPOINT_THRESHOLDS,
  },
};

if (!BASE_URL) {
  throw new Error("BASE_URL is empty. Set tests/k6/env.js or run with BASE_URL=http://localhost:8080 k6 run tests/k6/generated/smoke.js");
}

if (SMOKE_ENDPOINTS.length === 0) {
  throw new Error("No k6-safe smoke endpoints were generated from tests/testing-policy.yml");
}

export default function () {
  const endpoint = SMOKE_ENDPOINTS[__ITER % SMOKE_ENDPOINTS.length];
  const res = http.request(endpoint.method, \`\${BASE_URL}\${endpoint.path}\`, null, {
    tags: { name: endpoint.name },
  });

  check(res, {
    [\`\${endpoint.name} returns 2xx or 3xx\`]: (r) => r.status >= 200 && r.status < 400,
  });

  sleep(1);
}

export function handleSummary(data) {
  return createPerformanceSummary(data, {
    purpose: "generated smoke",
    script: "tests/k6/generated/smoke.js",
  });
}
`;
}

function renderK6Env() {
  return `const defaults = {
  baseUrl: "http://localhost:8080",
  smokePath: "/",
  vusCount: 1,
  iterations: 1,
  targetName: "local",
  scenarioName: "smoke",
  reportDir: "docs/performance",
  summaryDir: "docs/performance/k6",
  reportName: "",
  appImageTag: "",
  appCommit: "",
};

export function envValue(name, fallback) {
  const value = __ENV[name];
  return value === undefined || value === "" ? fallback : value;
}

export function envNumber(name, fallback) {
  const value = envValue(name, fallback);
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : fallback;
}

export const BASE_URL = envValue("BASE_URL", defaults.baseUrl);
export const SMOKE_PATH = envValue("SMOKE_PATH", defaults.smokePath);
export const VUS_COUNT = envNumber("VUS_COUNT", defaults.vusCount);
export const ITERATIONS = envNumber("ITERATIONS", defaults.iterations);
export const TARGET_NAME = envValue("K6_TARGET_NAME", defaults.targetName);
export const SCENARIO_NAME = envValue("K6_SCENARIO_NAME", defaults.scenarioName);
export const REPORT_DIR = envValue("K6_REPORT_DIR", defaults.reportDir);
export const SUMMARY_DIR = envValue("K6_SUMMARY_DIR", defaults.summaryDir);
export const REPORT_NAME = envValue("K6_REPORT_NAME", defaults.reportName);
export const APP_IMAGE_TAG = envValue("APP_IMAGE_TAG", defaults.appImageTag);
export const APP_COMMIT = envValue("APP_COMMIT", defaults.appCommit);
`;
}

function renderK6Report() {
  return `import {
  APP_COMMIT,
  APP_IMAGE_TAG,
  ITERATIONS,
  REPORT_DIR,
  REPORT_NAME,
  SCENARIO_NAME,
  SUMMARY_DIR,
  TARGET_NAME,
  VUS_COUNT,
} from "./env.js";

function pad(value) {
  return String(value).padStart(2, "0");
}

function slug(value) {
  return String(value || "unknown")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "") || "unknown";
}

function metricValues(data, name) {
  return data.metrics?.[name]?.values || {};
}

function formatMs(value) {
  return Number.isFinite(value) ? \`\${value.toFixed(2)} ms\` : "-";
}

function formatRate(value) {
  return Number.isFinite(value) ? \`\${(value * 100).toFixed(2)}%\` : "-";
}

function thresholdStatus(data) {
  const failed = [];
  for (const [metricName, metric] of Object.entries(data.metrics || {})) {
    for (const [threshold, result] of Object.entries(metric.thresholds || {})) {
      if (result?.ok === false) failed.push(\`\${metricName} \${threshold}\`);
    }
  }
  return failed;
}

function reportBaseName(now) {
  if (REPORT_NAME) return slug(REPORT_NAME);
  const date = \`\${now.getFullYear()}-\${pad(now.getMonth() + 1)}-\${pad(now.getDate())}\`;
  const time = \`\${pad(now.getHours())}\${pad(now.getMinutes())}\${pad(now.getSeconds())}\`;
  return \`\${date}-\${time}-\${slug(TARGET_NAME)}-\${slug(SCENARIO_NAME)}\`;
}

function renderMarkdown(data, paths, now) {
  const failed = thresholdStatus(data);
  const checks = metricValues(data, "checks");
  const duration = data.metrics?.http_req_duration?.values || {};
  const reqs = data.metrics?.http_reqs?.values?.count || 0;
  const seconds = (data.state?.testRunDurationMs || 0) / 1000;
  const rps = seconds > 0 ? reqs / seconds : 0;

  return \`# \${TARGET_NAME} \${SCENARIO_NAME} 성능 테스트 리포트

## 요약

| 항목 | 값 |
|---|---|
| 결론 | \${failed.length === 0 ? "pass" : "fail"} |
| 실행 일시 | \\\`\${now.toISOString()}\\\` |
| 테스트 ID | \\\`\${paths.baseName}\\\` |
| 대상 환경 | \\\`\${TARGET_NAME}\\\` |
| 앱 image tag | \\\`\${APP_IMAGE_TAG || "-"}\\\` |
| 앱 commit | \\\`\${APP_COMMIT || "-"}\\\` |

## 부하 조건

| 항목 | 값 |
|---|---:|
| VUs | \${VUS_COUNT} |
| iterations | \${ITERATIONS} |
| duration | \${seconds.toFixed(2)} s |

## k6 결과

| 지표 | 값 |
|---|---:|
| checks | \${checks.passes ?? 0}/\${(checks.passes ?? 0) + (checks.fails ?? 0)} |
| http_reqs | \${reqs} |
| requests/sec | \${rps.toFixed(2)} |
| http_req_failed | \${formatRate(data.metrics?.http_req_failed?.values?.rate ?? 0)} |
| http_req_duration avg | \${formatMs(duration.avg)} |
| http_req_duration med | \${formatMs(duration.med)} |
| http_req_duration p95 | \${formatMs(duration["p(95)"])} |
| http_req_duration p99 | \${formatMs(duration["p(99)"])} |
| http_req_duration max | \${formatMs(duration.max)} |

## Artifact

| 종류 | 경로 |
|---|---|
| k6 summary JSON | \\\`\${paths.jsonPath}\\\` |
| k6 markdown report | \\\`\${paths.mdPath}\\\` |

## 결론

\${failed.length === 0 ? "Threshold 기준으로 통과했다." : \`Threshold 실패: \${failed.join(", ")}\`}
\`;
}

export function createPerformanceSummary(data) {
  const now = new Date();
  const baseName = reportBaseName(now);
  const paths = {
    baseName,
    mdPath: \`\${REPORT_DIR}/\${baseName}.md\`,
    jsonPath: \`\${SUMMARY_DIR}/\${baseName}-summary.json\`,
  };

  return {
    stdout: \`\\n[k6-report] markdown: \${paths.mdPath}\\n[k6-report] summary: \${paths.jsonPath}\\n\\n\`,
    [paths.mdPath]: renderMarkdown(data, paths, now),
    [paths.jsonPath]: JSON.stringify(data, null, 2),
  };
}
`;
}

function replaceReportSection(sectionTitle, body) {
  fs.mkdirSync(path.dirname(reportPath), { recursive: true });
  let report = fs.existsSync(reportPath) ? fs.readFileSync(reportPath, "utf8") : "# Testing Setup Report\n";
  const marker = `\n## ${sectionTitle}\n`;
  const markerIndex = report.indexOf(marker);

  if (markerIndex >= 0) {
    report = `${report.slice(0, markerIndex).trimEnd()}\n`;
  }

  fs.writeFileSync(reportPath, `${report.trimEnd()}\n\n## ${sectionTitle}\n\n${body.trimEnd()}\n`);
}

const policyText = fs.readFileSync(policyPath, "utf8");
const endpoints = parsePolicyEndpoints(policyText);
const generatedEndpoints = endpoints.filter((endpoint) => endpoint.class !== "manual" && endpoint.class !== "forbidden");
const k6SmokeEndpoints = endpoints.filter(isK6SafeSmoke);

ensureBrunoNativeCollection();
resetDir(brunoGeneratedDir);

writeFile(path.join(brunoGeneratedDir, "folder.bru"), renderFolder("generated", 1), brunoGeneratedDir);
writeFile(path.join(brunoGeneratedDir, "smoke/folder.bru"), renderFolder("smoke", 1), brunoGeneratedDir);
writeFile(path.join(brunoGeneratedDir, "draft/folder.bru"), renderFolder("draft", 2), brunoGeneratedDir);
writeFile(path.join(brunoGeneratedDir, "scenario/folder.bru"), renderFolder("scenario", 3), brunoGeneratedDir);

const brunoCounts = { smoke: 0, draft: 0, scenario: 0 };
for (const endpoint of generatedEndpoints) {
  const folder = brunoFolderFor(endpoint);
  brunoCounts[folder] += 1;
  const seq = brunoCounts[folder];
  const filePath = path.join(brunoGeneratedDir, folder, `${String(seq).padStart(3, "0")}-${slugFor(endpoint)}.bru`);
  writeFile(filePath, renderBrunoRequest(endpoint, seq, folder), brunoGeneratedDir);
}

writeFile(path.join(k6GeneratedDir, "smoke.js"), renderK6Smoke(k6SmokeEndpoints), k6GeneratedDir);
writeIfMissing(k6EnvPath, renderK6Env());
writeIfMissing(k6ReportPath, renderK6Report());

fs.mkdirSync(path.dirname(resultPath), { recursive: true });
fs.writeFileSync(resultPath, `${JSON.stringify({
  schemaVersion: 1,
  action: "generate-tests",
  status: "ok",
  timestamp: new Date().toISOString(),
  policy: path.relative(rootDir, policyPath),
  bruno: {
    generatedRoot: path.relative(rootDir, brunoGeneratedDir),
    format: "bru",
    smokeRequests: brunoCounts.smoke,
    draftRequests: brunoCounts.draft,
    scenarioRequests: brunoCounts.scenario,
  },
  k6: {
    generatedRoot: path.relative(rootDir, k6GeneratedDir),
    smokeEndpoints: k6SmokeEndpoints.length,
    smokeScript: "tests/k6/generated/smoke.js",
  },
  skippedManualOrForbidden: endpoints.length - generatedEndpoints.length,
}, null, 2)}\n`);

replaceReportSection("Generated Tests", [
  `- Generated at: \`${new Date().toISOString()}\``,
  `- Bruno smoke requests: \`${brunoCounts.smoke}\``,
  `- Bruno draft requests: \`${brunoCounts.draft}\``,
  `- Bruno scenario requests: \`${brunoCounts.scenario}\``,
  `- k6 smoke endpoints: \`${k6SmokeEndpoints.length}\``,
  `- Skipped manual/forbidden endpoints: \`${endpoints.length - generatedEndpoints.length}\``,
  "",
  "Bruno smoke generation includes only policy smoke endpoints with `reviewRequired: false`. Other smoke candidates are generated as draft requests so the default smoke command stays runnable.",
].join("\n"));

console.log(`[generate] bruno smoke ${brunoCounts.smoke}, draft ${brunoCounts.draft}, scenario ${brunoCounts.scenario}`);
console.log(`[generate] k6 smoke endpoints ${k6SmokeEndpoints.length}`);
console.log(`[state] wrote ${path.relative(rootDir, resultPath)}`);
console.log(`[report] updated ${path.relative(rootDir, reportPath)}`);
