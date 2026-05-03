#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";

const [rootDir, policyPath, resultPath, reportPath] = process.argv.slice(2);

if (!rootDir || !policyPath || !resultPath || !reportPath) {
  console.error("Usage: generate-tests.mjs <rootDir> <policyPath> <resultPath> <reportPath>");
  process.exit(1);
}

const brunoGeneratedDir = path.join(rootDir, "tests/bruno/api/generated");
const k6GeneratedDir = path.join(rootDir, "tests/k6/generated");

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

function renderBrunoRequest(endpoint, seq, folder) {
  const tags = tagsFor(endpoint, folder).map((tag) => `    - ${tag}`).join("\n");
  const docs = [
    "Managed by setup-agent from tests/testing-policy.yml.",
    `Policy class: ${endpoint.class}`,
    `Confidence: ${endpoint.confidence}`,
    `Review required: ${endpoint.reviewRequired ? "true" : "false"}`,
    `Reason: ${endpoint.reason || ""}`,
    "Generated requests use auth: none to keep smoke execution stable. Move protected requests to manual/ and configure auth there.",
  ].join("\n  ");

  return `info:
  name: ${JSON.stringify(titleFor(endpoint))}
  type: http
  seq: ${seq}
  tags:
${tags}

http:
  method: ${String(endpoint.method).toUpperCase()}
  url: "{{baseUrl}}${brunoPath(endpoint.path)}"

runtime:
  scripts:
    - type: tests
      code: |-
        test("returns a 2xx or 3xx response", function () {
          expect(res.getStatus()).to.be.within(200, 399);
        });

settings:
  encodeUrl: true
  timeout: 3000
  followRedirects: true

docs: |-
  ${docs}
`;
}

function renderFolder(name, seq) {
  return `info:
  name: ${name}
  type: folder
  seq: ${seq}
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

  return `import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: 1,
  iterations: Math.max(1, Number(__ENV.K6_ITERATIONS || ${Math.max(1, endpoints.length)})),
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<1000"],
  },
};

const BASE_URL = __ENV.BASE_URL;
const SMOKE_ENDPOINTS = [
${endpointArray}
];

if (!BASE_URL) {
  throw new Error("BASE_URL is required. Example: BASE_URL=http://localhost:8080 k6 run tests/k6/generated/smoke.js");
}

if (SMOKE_ENDPOINTS.length === 0) {
  throw new Error("No k6-safe smoke endpoints were generated from tests/testing-policy.yml");
}

export default function () {
  const endpoint = SMOKE_ENDPOINTS[__ITER % SMOKE_ENDPOINTS.length];
  const res = http.request(endpoint.method, \`\${BASE_URL}\${endpoint.path}\`);

  check(res, {
    [\`\${endpoint.name} returns 2xx or 3xx\`]: (r) => r.status >= 200 && r.status < 400,
  });

  sleep(1);
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

for (const folder of ["smoke", "draft", "scenario"]) {
  resetDir(path.join(brunoGeneratedDir, folder));
}

writeFile(path.join(brunoGeneratedDir, "folder.yml"), renderFolder("generated", 1), brunoGeneratedDir);
writeFile(path.join(brunoGeneratedDir, "smoke/folder.yml"), renderFolder("smoke", 1), brunoGeneratedDir);
writeFile(path.join(brunoGeneratedDir, "draft/folder.yml"), renderFolder("draft", 2), brunoGeneratedDir);
writeFile(path.join(brunoGeneratedDir, "scenario/folder.yml"), renderFolder("scenario", 3), brunoGeneratedDir);

const brunoCounts = { smoke: 0, draft: 0, scenario: 0 };
for (const endpoint of generatedEndpoints) {
  const folder = brunoFolderFor(endpoint);
  brunoCounts[folder] += 1;
  const seq = brunoCounts[folder];
  const filePath = path.join(brunoGeneratedDir, folder, `${String(seq).padStart(3, "0")}-${slugFor(endpoint)}.yml`);
  writeFile(filePath, renderBrunoRequest(endpoint, seq, folder), brunoGeneratedDir);
}

writeFile(path.join(k6GeneratedDir, "smoke.js"), renderK6Smoke(k6SmokeEndpoints), k6GeneratedDir);

fs.mkdirSync(path.dirname(resultPath), { recursive: true });
fs.writeFileSync(resultPath, `${JSON.stringify({
  schemaVersion: 1,
  action: "generate-tests",
  status: "ok",
  timestamp: new Date().toISOString(),
  policy: path.relative(rootDir, policyPath),
  bruno: {
    generatedRoot: path.relative(rootDir, brunoGeneratedDir),
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
