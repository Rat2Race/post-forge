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
const sqlDir = path.join(rootDir, "tests/sql");
const seedScriptPath = path.join(rootDir, "tests/smoke/seed.sh");
const fixtureConfigCandidates = [
  path.join(rootDir, "testing-fixtures.json"),
  path.join(rootDir, "tests/testing-fixtures.json"),
];
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

function readFixtureConfig() {
  const filePath = fixtureConfigCandidates.find((candidate) => fs.existsSync(candidate));
  if (!filePath) return { config: null, path: "" };

  try {
    return {
      config: JSON.parse(fs.readFileSync(filePath, "utf8")),
      path: path.relative(rootDir, filePath),
    };
  } catch (error) {
    throw new Error(`Could not parse ${path.relative(rootDir, filePath)}: ${error.message}`);
  }
}

function safeProjectPath(relativePath, label) {
  if (!relativePath || typeof relativePath !== "string") {
    throw new Error(`${label} must be a non-empty relative path`);
  }

  const resolved = path.resolve(rootDir, relativePath);
  if (!resolved.startsWith(`${path.resolve(rootDir)}${path.sep}`) && resolved !== path.resolve(rootDir)) {
    throw new Error(`${label} escapes project root: ${relativePath}`);
  }

  return resolved;
}

function camelToSnake(value) {
  return String(value)
    .replace(/([a-z0-9])([A-Z])/g, "$1_$2")
    .replace(/([A-Z]+)([A-Z][a-z])/g, "$1_$2")
    .toLowerCase();
}

function singular(value) {
  const text = String(value);
  if (text.endsWith("ies")) return `${text.slice(0, -3)}y`;
  if (text.endsWith("s")) return text.slice(0, -1);
  return text;
}

function escapeSqlString(value) {
  return String(value).replace(/'/g, "''");
}

function sqlLiteral(value) {
  if (value === null || value === undefined) return "NULL";
  if (typeof value === "number") return String(value);
  if (typeof value === "boolean") return value ? "TRUE" : "FALSE";
  if (value === "CURRENT_TIMESTAMP" || value === "CURRENT_DATE") return value;
  return `'${escapeSqlString(value)}'`;
}

function renderInsert(table, columns, rows) {
  if (!table || columns.length === 0 || rows.length === 0) return "";
  const renderedRows = rows.map((row) => {
    return `(${columns.map((column) => sqlLiteral(row[column])).join(", ")})`;
  });

  return `INSERT INTO ${table} (${columns.join(", ")}) VALUES\n${renderedRows.join(",\n")};`;
}

function findJavaFiles(startDir) {
  const ignored = new Set([".git", ".gradle", ".omx", ".codex_tmp", "build", "out", "node_modules", "tests"]);
  const files = [];

  function visit(dir) {
    if (!fs.existsSync(dir)) return;
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
      if (ignored.has(entry.name)) continue;
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        visit(fullPath);
      } else if (entry.isFile() && fullPath.endsWith(".java")) {
        files.push(fullPath);
      }
    }
  }

  visit(startDir);
  return files;
}

function gradleProjectDependencies(moduleName) {
  const gradlePath = path.join(rootDir, moduleName, "build.gradle");
  if (!fs.existsSync(gradlePath)) return [];

  const text = fs.readFileSync(gradlePath, "utf8");
  return [...text.matchAll(/project\s*\(\s*['"]:([^'"]+)['"]\s*\)/g)]
    .map((match) => match[1].replace(/^:/, "").split(":")[0])
    .filter(Boolean);
}

function runtimeJavaRoots() {
  const appGradle = path.join(rootDir, "app", "build.gradle");
  if (!fs.existsSync(appGradle)) {
    return [rootDir];
  }

  const modules = new Set(["app"]);
  const queue = ["app"];
  while (queue.length > 0) {
    const moduleName = queue.shift();
    for (const dependency of gradleProjectDependencies(moduleName)) {
      if (modules.has(dependency)) continue;
      modules.add(dependency);
      queue.push(dependency);
    }
  }

  return [...modules]
    .map((moduleName) => path.join(rootDir, moduleName, "src/main/java"))
    .filter((sourceRoot) => fs.existsSync(sourceRoot));
}

function findRuntimeJavaFiles() {
  const seen = new Set();
  const files = [];
  for (const sourceRoot of runtimeJavaRoots()) {
    for (const filePath of findJavaFiles(sourceRoot)) {
      if (seen.has(filePath)) continue;
      seen.add(filePath);
      files.push(filePath);
    }
  }
  return files;
}

function stripJavaComments(text) {
  return text
    .replace(/\/\*[\s\S]*?\*\//g, "")
    .replace(/\/\/.*$/gm, "");
}

function annotationAttr(annotations, annotationName, attrName = "name") {
  const escaped = annotationName.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const pattern = new RegExp(`@${escaped}\\s*\\(([^)]*)\\)`, "s");
  const match = annotations.match(pattern);
  if (!match) return "";

  const body = match[1];
  const attrPattern = new RegExp(`${attrName}\\s*=\\s*"([^"]+)"`);
  const attrMatch = body.match(attrPattern);
  if (attrMatch) return attrMatch[1];

  const unnamed = body.trim().match(/^"([^"]+)"$/);
  return unnamed ? unnamed[1] : "";
}

function hasAnnotation(annotations, annotationName) {
  return new RegExp(`@${annotationName}\\b`).test(annotations);
}

function parseJavaEnums(javaFiles) {
  const enums = new Map();
  for (const filePath of javaFiles) {
    const text = stripJavaComments(fs.readFileSync(filePath, "utf8"));
    const match = text.match(/\benum\s+([A-Za-z_][A-Za-z0-9_]*)\s*\{([\s\S]*?)\}/);
    if (!match) continue;

    const values = match[2]
      .split(";")[0]
      .split(",")
      .map((value) => value.trim())
      .map((value) => value.match(/^([A-Z][A-Z0-9_]*)\b/)?.[1])
      .filter(Boolean);
    if (values.length > 0) enums.set(match[1], values);
  }
  return enums;
}

function parseEntityFile(filePath) {
  const text = stripJavaComments(fs.readFileSync(filePath, "utf8"));
  if (!/@Entity\b/.test(text)) return null;

  const classMatch = text.match(/\bclass\s+([A-Za-z_][A-Za-z0-9_]*)(?:\s+extends\s+([A-Za-z_][A-Za-z0-9_]*))?/);
  if (!classMatch) return null;

  const [, className, superClass] = classMatch;
  const beforeClass = text.slice(0, classMatch.index);
  const tableName = annotationAttr(beforeClass, "Table", "name") || camelToSnake(className);
  const fields = [];
  const lines = text.split(/\r?\n/);
  let annotations = [];

  for (const rawLine of lines) {
    const line = rawLine.trim();
    if (!line) {
      if (annotations.length > 0) annotations.push("");
      continue;
    }

    if (line.startsWith("@")) {
      annotations.push(line);
      continue;
    }

    const fieldMatch = line.match(/private\s+(?:final\s+)?([A-Za-z0-9_<>, ?]+)\s+([A-Za-z_][A-Za-z0-9_]*)\s*(?:=.*)?;/);
    if (fieldMatch) {
      const [, type, name] = fieldMatch;
      const annotationText = annotations.join("\n");
      const isManyToOne = /@ManyToOne\b/.test(annotationText);
      const isElementCollection = /@ElementCollection\b/.test(annotationText);
      const joinColumn = annotationAttr(annotationText, "JoinColumn", "name");
      const collectionTable = annotationAttr(annotationText, "CollectionTable", "name");
      const collectionJoinColumn = annotationText.match(/joinColumns\s*=\s*@JoinColumn\s*\(\s*name\s*=\s*"([^"]+)"/s)?.[1] || "";
      const columnName = annotationAttr(annotationText, "Column", "name") ||
        (isManyToOne ? (joinColumn || `${camelToSnake(name)}_id`) : camelToSnake(name));

      fields.push({
        name,
        type: type.trim().replace(/\s+/g, " "),
        column: columnName,
        annotations: annotationText,
        id: hasAnnotation(annotationText, "Id"),
        generated: hasAnnotation(annotationText, "GeneratedValue"),
        nullableFalse: /@Column\s*\([^)]*nullable\s*=\s*false/s.test(annotationText) ||
          /@JoinColumn\s*\([^)]*nullable\s*=\s*false/s.test(annotationText) ||
          /@ManyToOne\s*\([^)]*optional\s*=\s*false/s.test(annotationText),
        manyToOne: isManyToOne,
        elementCollection: isElementCollection,
        collectionTable,
        collectionJoinColumn,
        enumerated: hasAnnotation(annotationText, "Enumerated"),
      });
      annotations = [];
      continue;
    }

    annotations = [];
  }

  return {
    file: path.relative(rootDir, filePath),
    className,
    superClass: superClass || "",
    table: tableName,
    fields,
  };
}

function parsePersistentClassFile(filePath) {
  const text = stripJavaComments(fs.readFileSync(filePath, "utf8"));
  const isEntity = /@Entity\b/.test(text);
  const isMappedSuperclass = /@MappedSuperclass\b/.test(text);
  if (!isEntity && !isMappedSuperclass) return null;

  const classMatch = text.match(/\bclass\s+([A-Za-z_][A-Za-z0-9_]*)(?:\s+extends\s+([A-Za-z_][A-Za-z0-9_]*))?/);
  if (!classMatch) return null;

  const [, className, superClass] = classMatch;
  const beforeClass = text.slice(0, classMatch.index);
  const tableName = annotationAttr(beforeClass, "Table", "name") || camelToSnake(className);
  const fields = [];
  const lines = text.split(/\r?\n/);
  let annotations = [];

  for (const rawLine of lines) {
    const line = rawLine.trim();
    if (!line) {
      if (annotations.length > 0) annotations.push("");
      continue;
    }

    if (line.startsWith("@")) {
      annotations.push(line);
      continue;
    }

    const fieldMatch = line.match(/(?:private|protected)\s+(?:final\s+)?([A-Za-z0-9_.$<>, ?\[\]]+)\s+([A-Za-z_][A-Za-z0-9_]*)\s*(?:=.*)?;/);
    if (fieldMatch) {
      const [, type, name] = fieldMatch;
      const annotationText = annotations.join("\n");
      const isManyToOne = /@ManyToOne\b/.test(annotationText);
      const isOneToOne = /@OneToOne\b/.test(annotationText);
      const isOneToMany = /@OneToMany\b/.test(annotationText);
      const isManyToMany = /@ManyToMany\b/.test(annotationText);
      const isElementCollection = /@ElementCollection\b/.test(annotationText);
      const joinColumn = annotationAttr(annotationText, "JoinColumn", "name");
      const collectionTable = annotationAttr(annotationText, "CollectionTable", "name");
      const collectionJoinColumn = annotationText.match(/joinColumns\s*=\s*@JoinColumn\s*\(\s*name\s*=\s*"([^"]+)"/s)?.[1] || "";
      const relation = isManyToOne || isOneToOne;
      const columnName = annotationAttr(annotationText, "Column", "name") ||
        (relation ? (joinColumn || `${camelToSnake(name)}_id`) : camelToSnake(name));

      fields.push({
        name,
        type: type.trim().replace(/\s+/g, " "),
        column: columnName,
        annotations: annotationText,
        id: hasAnnotation(annotationText, "Id"),
        embeddedId: hasAnnotation(annotationText, "EmbeddedId"),
        generated: hasAnnotation(annotationText, "GeneratedValue"),
        nullableFalse: /@Column\s*\([^)]*nullable\s*=\s*false/s.test(annotationText) ||
          /@JoinColumn\s*\([^)]*nullable\s*=\s*false/s.test(annotationText) ||
          /@ManyToOne\s*\([^)]*optional\s*=\s*false/s.test(annotationText) ||
          /@OneToOne\s*\([^)]*optional\s*=\s*false/s.test(annotationText),
        relation,
        manyToOne: isManyToOne,
        oneToOne: isOneToOne,
        oneToMany: isOneToMany,
        manyToMany: isManyToMany,
        elementCollection: isElementCollection,
        collectionTable,
        collectionJoinColumn,
        enumerated: hasAnnotation(annotationText, "Enumerated"),
        transient: hasAnnotation(annotationText, "Transient") || /\btransient\b/.test(line),
        createdDate: hasAnnotation(annotationText, "CreatedDate"),
        lastModifiedDate: hasAnnotation(annotationText, "LastModifiedDate"),
        createdBy: hasAnnotation(annotationText, "CreatedBy"),
        lastModifiedBy: hasAnnotation(annotationText, "LastModifiedBy"),
      });
      annotations = [];
      continue;
    }

    annotations = [];
  }

  return {
    kind: isEntity ? "entity" : "mapped-superclass",
    file: path.relative(rootDir, filePath),
    className,
    superClass: superClass || "",
    table: tableName,
    fields,
  };
}

function discoverJpaModel() {
  const javaFiles = findRuntimeJavaFiles();
  const classes = javaFiles.map(parsePersistentClassFile).filter(Boolean);
  const mappedSuperclasses = new Map(classes
    .filter((item) => item.kind === "mapped-superclass")
    .map((item) => [item.className, item]));

  function inheritedFields(entity) {
    const fields = [];
    const seen = new Set();
    let current = entity.superClass;
    while (current && mappedSuperclasses.has(current) && !seen.has(current)) {
      seen.add(current);
      const mapped = mappedSuperclasses.get(current);
      fields.unshift(...mapped.fields.map((field) => ({ ...field, inheritedFrom: mapped.className })));
      current = mapped.superClass;
    }
    return fields;
  }

  const entities = classes
    .filter((item) => item.kind === "entity")
    .map((entity) => ({
      ...entity,
      fields: [...inheritedFields(entity), ...entity.fields],
    }));

  return {
    entities,
    mappedSuperclasses: [...mappedSuperclasses.values()],
    enums: parseJavaEnums(javaFiles),
  };
}

function findField(entity, namesOrColumns) {
  if (!entity) return null;
  const names = namesOrColumns.map((value) => String(value).toLowerCase());
  return entity.fields.find((field) => {
    return names.includes(field.name.toLowerCase()) || names.includes(field.column.toLowerCase());
  }) || null;
}

function fieldColumn(entity, namesOrColumns) {
  return findField(entity, namesOrColumns)?.column || "";
}

function idField(entity) {
  return entity?.fields.find((field) => field.id || field.embeddedId) || null;
}

function idColumn(entity) {
  return idField(entity)?.column || "id";
}

function simpleTypeName(type) {
  return String(type || "")
    .replace(/\?.*$/, "")
    .replace(/\[\]$/, "")
    .replace(/^.*\./, "")
    .trim();
}

function collectionValueType(type) {
  const match = String(type || "").match(/<\s*([A-Za-z0-9_.$]+)/);
  return match ? simpleTypeName(match[1]) : "String";
}

function lowerFirst(value) {
  const text = String(value || "");
  return text ? `${text[0].toLowerCase()}${text.slice(1)}` : text;
}

function entityVarStem(entity) {
  return lowerFirst(entity.className.replace(/Entity$/, ""));
}

function tableVarStem(entity) {
  return singular(String(entity.table || entity.className).replace(/_/g, ""));
}

function isStringType(type) {
  return ["String", "CharSequence", "Text"].includes(simpleTypeName(type));
}

function isBooleanType(type) {
  return ["Boolean", "boolean"].includes(simpleTypeName(type));
}

function isIntegerType(type) {
  return ["Byte", "byte", "Short", "short", "Integer", "int", "Long", "long"].includes(simpleTypeName(type));
}

function isDecimalType(type) {
  return ["Float", "float", "Double", "double", "BigDecimal", "BigInteger"].includes(simpleTypeName(type));
}

function isTemporalType(type) {
  return ["LocalDateTime", "OffsetDateTime", "ZonedDateTime", "Instant", "Timestamp", "Date"].includes(simpleTypeName(type));
}

function isLocalDateType(type) {
  return ["LocalDate"].includes(simpleTypeName(type));
}

function isUuidType(type) {
  return ["UUID"].includes(simpleTypeName(type));
}

function isBasicType(type, enums) {
  const name = simpleTypeName(type);
  return isStringType(name) || isBooleanType(name) || isIntegerType(name) || isDecimalType(name) ||
    isTemporalType(name) || isLocalDateType(name) || isUuidType(name) || enums.has(name);
}

function isColumnField(field, enums) {
  if (field.transient || field.oneToMany || field.manyToMany || field.elementCollection) return false;
  if (field.id || field.embeddedId || field.relation) return true;
  return isBasicType(field.type, enums) || /@Column\b/.test(field.annotations) ||
    field.createdDate || field.lastModifiedDate || field.createdBy || field.lastModifiedBy;
}

function deterministicIdValue(entity, index = 0) {
  const field = idField(entity);
  const type = simpleTypeName(field?.type || "Long");
  const numberValue = 9001 + index;
  if (isUuidType(type)) return `00000000-0000-0000-0000-${String(numberValue).padStart(12, "0")}`;
  if (isStringType(type)) return `test-${camelToSnake(entity.className).replace(/_/g, "-")}-${numberValue}`;
  return numberValue;
}

function enumValueForField(field, enums) {
  const enumName = field.elementCollection ? collectionValueType(field.type) : simpleTypeName(field.type);
  const values = enums.get(enumName) || [];
  const lowerName = `${field.name} ${field.column}`.toLowerCase();
  const preferred = [
    lowerName.includes("role") ? "USER" : "",
    lowerName.includes("status") ? "ACTIVE" : "",
    lowerName.includes("type") ? "GENERAL" : "",
    "GENERAL",
    "USER",
    "ACTIVE",
  ].filter(Boolean);
  return preferred.find((value) => values.includes(value)) || values[0] || "TEST";
}

function stringValueForField(entity, field, index, context) {
  const lowerName = `${field.name} ${field.column}`.toLowerCase();
  const suffix = index > 0 ? String(index + 1) : "";
  if (/password|passwd|pwd|user_pw/.test(lowerName)) {
    return "{bcrypt}$2a$10$/bF.EhbSpYgFDbAimIdPn.M0CBw.2oASjraWo00sDZWZdYvT6yuda";
  }
  if (/email/.test(lowerName)) return `testuser${index + 1}@example.test`;
  if (/(^|_)user_?id|userid|username|login|account/.test(lowerName)) return `testuser${index + 1}`;
  if (/nick|display/.test(lowerName)) return `테스터${index + 1}`;
  if (/title|subject/.test(lowerName)) return `${entity.className} fixture title${suffix}`;
  if (/content|body|text|message/.test(lowerName)) return `${entity.className} fixture content for generated smoke and scenario tests.`;
  if (/summary|description|desc/.test(lowerName)) return `${entity.className} fixture summary.`;
  if (/name/.test(lowerName)) return `${entity.className} fixture name${suffix}`;
  if (/path|url|uri/.test(lowerName)) return `/tmp/${camelToSnake(entity.className)}-${index + 1}`;
  if (/file/.test(lowerName)) return `${camelToSnake(entity.className)}-${index + 1}.txt`;
  if (/provider/.test(lowerName)) return "local";
  if (/source/.test(lowerName)) return "generated-seed";
  if (/key|token|code|secret/.test(lowerName)) return `test-${camelToSnake(field.name)}-${index + 1}`;
  if (/created_by|modified_by|author|owner/.test(lowerName)) return context.loginIdentifier || "testuser1";
  return `${entity.className}-${field.name}-fixture${suffix}`;
}

function numericValueForField(field, index) {
  const lowerName = `${field.name} ${field.column}`.toLowerCase();
  if (/count|views|size|total|amount|price|score|point/.test(lowerName)) return 0;
  if (/order|sort|seq|rank|level/.test(lowerName)) return index + 1;
  return index + 1;
}

function booleanValueForField(field) {
  const lowerName = `${field.name} ${field.column}`.toLowerCase();
  if (/deleted|removed|expired|locked|blocked|disabled|fail/.test(lowerName)) return false;
  if (/enabled|active|verified|valid|success/.test(lowerName)) return true;
  return true;
}

function valueForField(entity, field, index, context, enums) {
  if (field.id || field.embeddedId) return deterministicIdValue(entity, index);
  if (field.relation) {
    const target = context.entitiesByClass.get(simpleTypeName(field.type));
    if (!target) return null;
    if (target.className === entity.className) {
      return field.nullableFalse ? deterministicIdValue(entity, index) : null;
    }
    return deterministicIdValue(target, 0);
  }

  const type = simpleTypeName(field.type);
  if (enums.has(type) || field.enumerated) return enumValueForField(field, enums);
  if (isStringType(type)) return stringValueForField(entity, field, index, context);
  if (isBooleanType(type)) return booleanValueForField(field);
  if (isIntegerType(type) || isDecimalType(type)) return numericValueForField(field, index);
  if (isTemporalType(type)) return "CURRENT_TIMESTAMP";
  if (isLocalDateType(type)) return "CURRENT_DATE";
  if (isUuidType(type)) return `00000000-0000-0000-0000-${String(9001 + index).padStart(12, "0")}`;
  return null;
}

function dependencyClasses(entity, entitiesByClass) {
  return entity.fields
    .filter((field) => field.relation && !field.transient)
    .map((field) => simpleTypeName(field.type))
    .filter((className) => className !== entity.className && entitiesByClass.has(className));
}

function sortEntitiesByDependencies(entities) {
  const entitiesByClass = new Map(entities.map((entity) => [entity.className, entity]));
  const output = [];
  const state = new Map();

  function visit(entity) {
    const current = state.get(entity.className);
    if (current === "done") return;
    if (current === "visiting") return;
    state.set(entity.className, "visiting");
    for (const depClass of dependencyClasses(entity, entitiesByClass)) {
      visit(entitiesByClass.get(depClass));
    }
    state.set(entity.className, "done");
    output.push(entity);
  }

  entities.forEach(visit);
  return output;
}

function uniquePush(array, value) {
  if (!array.includes(value)) array.push(value);
}

function buildEntityRows(entity, context, enums) {
  const columns = [];
  const row = {};
  const warnings = [];

  for (const field of entity.fields) {
    if (!isColumnField(field, enums)) continue;
    const value = valueForField(entity, field, 0, context, enums);
    if (value === null && field.nullableFalse) {
      warnings.push(`Could not infer non-null value for ${entity.className}.${field.name}; generated seed may need override.`);
    }
    uniquePush(columns, field.column);
    row[field.column] = value;
  }

  if (!columns.includes(idColumn(entity))) {
    columns.unshift(idColumn(entity));
    row[idColumn(entity)] = deterministicIdValue(entity, 0);
  }

  return { columns, rows: [row], warnings };
}

function buildElementCollectionRows(entity, context, enums) {
  const collections = [];
  for (const field of entity.fields.filter((item) => item.elementCollection && !item.transient)) {
    const table = field.collectionTable || `${entity.table}_${camelToSnake(field.name)}`;
    const joinColumn = field.collectionJoinColumn || `${singular(entity.table)}_id`;
    const valueColumn = annotationAttr(field.annotations, "Column", "name") || singular(camelToSnake(field.name));
    const valueType = collectionValueType(field.type);
    const value = enums.has(valueType) ? enumValueForField(field, enums) : stringValueForField(entity, { ...field, type: valueType, name: valueColumn, column: valueColumn }, 0, context);
    collections.push({
      table,
      columns: [joinColumn, valueColumn],
      rows: [{ [joinColumn]: deterministicIdValue(entity, 0), [valueColumn]: value }],
    });
  }
  return collections;
}

function detectAuthContext(entities, context) {
  const authEntity = entities.find((entity) => {
    return entity.fields.some((field) => /password|passwd|pwd|user_pw/i.test(`${field.name} ${field.column}`)) &&
      entity.fields.some((field) => /user_?id|userid|username|login|account|email/i.test(`${field.name} ${field.column}`));
  });
  if (!authEntity) return;

  const loginField = authEntity.fields.find((field) => /user_?id|userid|username|login|account/i.test(`${field.name} ${field.column}`)) ||
    authEntity.fields.find((field) => /email/i.test(`${field.name} ${field.column}`));
  if (!loginField) return;

  context.authSeed = true;
  context.authEntity = authEntity.className;
  context.loginRequestField = /email/i.test(`${loginField.name} ${loginField.column}`) ? "email" : loginField.name;
  context.loginIdentifier = /email/i.test(`${loginField.name} ${loginField.column}`) ? "testuser1@example.test" : "testuser1";
  context.loginUserId = context.loginIdentifier;
  context.loginPassword = "Test1234!";
  context.env.loginIdentifier = context.loginIdentifier;
  context.env.loginUserId = context.loginIdentifier;
  context.env.loginPassword = context.loginPassword;
}

function registerEntityEnvVars(entity, context) {
  const value = deterministicIdValue(entity, 0);
  const classStem = entityVarStem(entity);
  const tableStem = tableVarStem(entity);
  context.env[`${classStem}Id`] = value;
  context.env[`${tableStem}Id`] = value;
  if (/user|member|account/i.test(entity.className) && context.authSeed) {
    context.env.userId = context.loginIdentifier;
    context.env.memberId = value;
    context.env.accountId = value;
  }
}

function buildEntitySeedSql() {
  const model = discoverJpaModel();
  const { entities, enums } = model;
  const seedableEntities = entities.filter((entity) => entity.fields.some((field) => field.id || field.embeddedId));
  const warnings = [];
  const context = {
    authSeed: false,
    loginIdentifier: "testuser1",
    loginUserId: "testuser1",
    loginPassword: "Test1234!",
    loginRequestField: "userId",
    env: {},
    entitiesByClass: new Map(seedableEntities.map((entity) => [entity.className, entity])),
  };

  if (seedableEntities.length === 0) {
    return {
      status: "skipped",
      reason: "no @Entity classes with @Id were found for automatic seed",
      sqlFiles: [],
      seedScript: "",
      model,
      context,
      warnings: ["Automatic SQL seed requires @Entity classes with @Id or explicit testing-fixtures.json sql config."],
    };
  }

  detectAuthContext(seedableEntities, context);
  const orderedEntities = sortEntitiesByDependencies(seedableEntities);
  orderedEntities.forEach((entity) => registerEntityEnvVars(entity, context));

  const entityInserts = [];
  const collectionInserts = [];
  for (const entity of orderedEntities) {
    const built = buildEntityRows(entity, context, enums);
    warnings.push(...built.warnings);
    if (built.columns.length > 0) entityInserts.push({ entity, ...built });
    collectionInserts.push(...buildElementCollectionRows(entity, context, enums));
  }

  const resetLines = [
    "-- Entity-derived setup seed reset. Safe to rerun for generated smoke/scenario data only.",
    "-- Generated from every discovered @Entity with @Id; review before using against shared or production databases.",
    "",
  ];
  for (const collection of [...collectionInserts].reverse()) {
    resetLines.push(`DELETE FROM ${collection.table} WHERE ${collection.columns[0]} = ${sqlLiteral(collection.rows[0][collection.columns[0]])};`);
  }
  for (const item of [...entityInserts].reverse()) {
    const idCol = idColumn(item.entity);
    resetLines.push(`DELETE FROM ${item.entity.table} WHERE ${idCol} = ${sqlLiteral(deterministicIdValue(item.entity, 0))};`);
  }

  const seedLines = [
    "-- Entity-derived setup seed data from all discovered @Entity classes.",
    "-- Safe test credentials are deterministic when a login-capable entity is detected.",
    "-- Raw password for generated login-capable users: Test1234!",
    "-- Password hash uses Spring Security delegating bcrypt format when a password column is detected.",
    "",
  ];
  for (const item of entityInserts) {
    seedLines.push(renderInsert(item.entity.table, item.columns, item.rows), "");
  }
  for (const item of collectionInserts) {
    seedLines.push(renderInsert(item.table, item.columns, item.rows), "");
  }

  return {
    status: "generated",
    reason: "generic @Entity-derived JPA seed",
    sqlFiles: [
      { name: "00_reset.sql", content: `${resetLines.join("\n")}\n` },
      { name: "01_entity_seed.sql", content: `${seedLines.join("\n").replace(/\n{3,}/g, "\n\n").trimEnd()}\n` },
    ],
    seedScript: "",
    model,
    context,
    warnings,
  };
}

function renderGenericSeedScript(sqlFiles) {
  const sqlFileLines = sqlFiles.map((filePath) => `  "$ROOT_DIR/${filePath}"`).join("\n");

  return `#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "\${BASH_SOURCE[0]}")/../.." && pwd)"
SQL_FILES=(
${sqlFileLines}
)

load_dotenv_file() {
  local env_file="$1"
  [[ -f "$env_file" ]] || return 0

  local line key value
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="\${line%$'\\r'}"
    [[ -z "$line" || "$line" == \\#* || "$line" != *=* ]] && continue

    key="\${line%%=*}"
    value="\${line#*=}"
    key="$(printf '%s' "$key" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
    value="$(printf '%s' "$value" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"

    [[ "$key" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || continue
    [[ -z "\${!key+x}" ]] || continue

    if [[ "$value" == \\"*\\" && "$value" == *\\" && \${#value} -ge 2 ]]; then
      value="\${value:1:\${#value}-2}"
    elif [[ "$value" == \\'*\\' && "$value" == *\\' && \${#value} -ge 2 ]]; then
      value="\${value:1:\${#value}-2}"
    fi

    export "$key=$value"
  done < "$env_file"
}

load_default_env() {
  [[ "\${SEED_LOAD_ENV:-true}" == "false" ]] && return 0
  load_dotenv_file "$ROOT_DIR/.env"
  load_dotenv_file "$ROOT_DIR/.env.local"
}

require_file() {
  local file="$1"
  if [[ ! -f "$file" ]]; then
    echo "[seed] missing SQL file: $file" >&2
    exit 1
  fi
}

run_docker_compose_psql_file() {
  local file="$1"
  local user="$2"
  local password="$3"
  local db="\${DB_NAME:-\${POSTGRES_DB:-}}"
  local compose_file="\${DB_DOCKER_COMPOSE_FILE:-\${POSTGRES_COMPOSE_FILE:-$ROOT_DIR/docker-compose.local.yml}}"
  local service="\${DB_DOCKER_COMPOSE_SERVICE:-\${POSTGRES_SERVICE:-postgres}}"

  command -v docker >/dev/null 2>&1 || return 1
  docker compose version >/dev/null 2>&1 || return 1
  [[ -f "$compose_file" ]] || return 1

  local container_id
  container_id="$(docker compose -f "$compose_file" ps -q "$service" 2>/dev/null || true)"
  [[ -n "$container_id" ]] || return 1

  local args=(-v ON_ERROR_STOP=1)
  if [[ -n "$user" ]]; then
    args+=(-U "$user")
  fi
  if [[ -n "$db" ]]; then
    args+=(-d "$db")
  fi
  args+=(-f -)

  echo "[seed] docker compose exec $service psql < $file"
  if [[ -n "$password" ]]; then
    PGPASSWORD="$password" docker compose -f "$compose_file" exec -T -e PGPASSWORD "$service" psql "\${args[@]}" < "$file"
  else
    docker compose -f "$compose_file" exec -T "$service" psql "\${args[@]}" < "$file"
  fi
}

run_psql_file() {
  local file="$1"
  local url="\${PSQL_URL:-\${DATABASE_URL:-}}"
  local user="\${DB_USER:-\${SPRING_DATASOURCE_USERNAME:-\${POSTGRES_USER:-}}}"
  local password="\${DB_PASSWORD:-\${SPRING_DATASOURCE_PASSWORD:-\${POSTGRES_PASSWORD:-}}}"

  if [[ -z "$url" ]]; then
    local jdbc_url="\${SPRING_DATASOURCE_URL:-\${DB_URL:-}}"
    if [[ "$jdbc_url" == jdbc:postgresql://* ]]; then
      url="\${jdbc_url#jdbc:}"
    elif [[ -n "\${POSTGRES_DB:-}" ]]; then
      local host="\${POSTGRES_HOST:-localhost}"
      local port="\${POSTGRES_PORT:-5432}"
      url="postgresql://$host:$port/\${POSTGRES_DB}"
    fi
  fi

  if [[ -z "$url" ]]; then
    echo "[seed] no PostgreSQL URL found. Set PSQL_URL, DATABASE_URL, SPRING_DATASOURCE_URL, DB_URL, or POSTGRES_DB." >&2
    exit 1
  fi

  if ! command -v psql >/dev/null 2>&1; then
    if run_docker_compose_psql_file "$file" "$user" "$password"; then
      return 0
    fi
    echo "[seed] psql is required for PostgreSQL seed execution, or set DB_DOCKER_COMPOSE_FILE/DB_DOCKER_COMPOSE_SERVICE for docker compose fallback." >&2
    exit 1
  fi

  local args=("$url" -v ON_ERROR_STOP=1 -f "$file")
  if [[ -n "$user" ]]; then
    args=(-U "$user" "\${args[@]}")
  fi

  echo "[seed] psql < $file"
  if [[ -n "$password" ]]; then
    PGPASSWORD="$password" psql "\${args[@]}"
  else
    psql "\${args[@]}"
  fi
}

run_h2_file() {
  local file="$1"
  local url="\${SPRING_DATASOURCE_URL:-\${DB_URL:-}}"
  local user="\${DB_USER:-\${SPRING_DATASOURCE_USERNAME:-sa}}"
  local password="\${DB_PASSWORD:-\${SPRING_DATASOURCE_PASSWORD:-}}"

  if [[ -z "$url" || "$url" != jdbc:h2:* ]]; then
    echo "[seed] H2 seed requires SPRING_DATASOURCE_URL or DB_URL starting with jdbc:h2:." >&2
    exit 1
  fi
  if [[ -z "\${H2_JAR:-}" || ! -f "$H2_JAR" ]]; then
    echo "[seed] H2 seed requires H2_JAR=/path/to/h2.jar." >&2
    exit 1
  fi

  echo "[seed] h2 RunScript < $file"
  java -cp "$H2_JAR" org.h2.tools.RunScript \
    -url "$url" \
    -user "$user" \
    -password "$password" \
    -script "$file"
}

main() {
  load_default_env

  for file in "\${SQL_FILES[@]}"; do
    require_file "$file"
  done

  local client="\${DB_CLIENT:-auto}"
  local jdbc_url="\${SPRING_DATASOURCE_URL:-\${DB_URL:-}}"
  if [[ "$client" == "auto" ]]; then
    if [[ "$jdbc_url" == jdbc:h2:* ]]; then
      client="h2"
    else
      client="postgres"
    fi
  fi

  for file in "\${SQL_FILES[@]}"; do
    case "$client" in
      postgres|postgresql) run_psql_file "$file" ;;
      h2) run_h2_file "$file" ;;
      *) echo "[seed] unsupported DB_CLIENT=$client (expected auto, postgres, h2)." >&2; exit 1 ;;
    esac
  done

  echo "[seed] complete"
}

main "$@"
`;
}

function writeSqlFixtures(fixtureConfig) {
  const sqlConfig = fixtureConfig?.sql;
  if (sqlConfig?.enabled === false) {
    return {
      status: "skipped",
      reason: "fixture config disabled sql section",
      sqlFiles: [],
      seedScript: "",
      context: null,
      warnings: [],
    };
  }

  const sourceDir = sqlConfig?.sourceDir || "testing-fixtures/sql";
  const outputDir = sqlConfig?.outputDir || "tests/sql";
  const files = Array.isArray(sqlConfig?.files) ? sqlConfig.files : [];

  const outputRoot = safeProjectPath(outputDir, "sql.outputDir");
  const writtenFiles = [];
  fs.rmSync(outputRoot, { recursive: true, force: true });
  fs.mkdirSync(outputRoot, { recursive: true });

  if (sqlConfig && files.length > 0) {
    const sourceRoot = safeProjectPath(sourceDir, "sql.sourceDir");
    for (const fileName of files) {
      const sourcePath = path.resolve(sourceRoot, fileName);
      if (!sourcePath.startsWith(`${sourceRoot}${path.sep}`) && sourcePath !== sourceRoot) {
        throw new Error(`sql file escapes sourceDir: ${fileName}`);
      }
      if (!fs.existsSync(sourcePath)) {
        throw new Error(`SQL fixture source file not found: ${path.relative(rootDir, sourcePath)}`);
      }

      const relativeOutput = path.join(outputDir, fileName);
      const outputPath = path.resolve(outputRoot, fileName);
      writeFile(outputPath, fs.readFileSync(sourcePath, "utf8"), outputRoot);
      writtenFiles.push(relativeOutput);
    }
  } else {
    const autoSeed = buildEntitySeedSql();
    if (autoSeed.status !== "generated") {
      return {
        status: autoSeed.status,
        reason: autoSeed.reason,
        sqlFiles: [],
        seedScript: "",
        context: autoSeed.context,
        warnings: autoSeed.warnings,
      };
    }

    for (const file of autoSeed.sqlFiles) {
      const relativeOutput = path.join(outputDir, file.name);
      writeFile(path.resolve(outputRoot, file.name), file.content, outputRoot);
      writtenFiles.push(relativeOutput);
    }

    const seedConfig = fixtureConfig?.seed || {};
    const seedPath = seedConfig.output || "tests/smoke/seed.sh";
    writeFile(safeProjectPath(seedPath, "seed.output"), renderGenericSeedScript(writtenFiles), path.dirname(safeProjectPath(seedPath, "seed.output")));
    fs.chmodSync(safeProjectPath(seedPath, "seed.output"), 0o755);

    return {
      status: "generated",
      reason: autoSeed.reason,
      sqlFiles: writtenFiles,
      seedScript: seedPath,
      context: autoSeed.context,
      warnings: autoSeed.warnings,
    };
  }

  const seedConfig = fixtureConfig.seed || {};
  const seedPath = seedConfig.output || "tests/smoke/seed.sh";
  writeFile(safeProjectPath(seedPath, "seed.output"), renderGenericSeedScript(writtenFiles), path.dirname(safeProjectPath(seedPath, "seed.output")));
  fs.chmodSync(safeProjectPath(seedPath, "seed.output"), 0o755);

  return {
    status: "generated",
    reason: "fixture config sql section",
    sqlFiles: writtenFiles,
    seedScript: seedPath,
    context: null,
    warnings: [],
  };
}

function fixtureValue(value) {
  if (value === undefined || value === null) return "";
  if (typeof value === "string") return value;
  return JSON.stringify(value);
}

function renderJsonBody(body) {
  return JSON.stringify(body || {}, null, 2)
    .split("\n")
    .map((line) => `  ${line}`)
    .join("\n");
}

function renderBrunoAuthRequest(request, seq, tokenVar) {
  const method = String(request.method || "GET").toLowerCase();
  const requestPath = request.path || "/";
  const body = request.body || null;
  const hasBody = body !== null && body !== undefined;
  const docs = [
    "Managed by setup-agent from fixture config or entity-derived defaults.",
    "This generated request is intended for seeded/authenticated smoke coverage.",
  ].join("\n");

  return `meta {
  name: ${request.name || `${method.toUpperCase()} ${requestPath}`}
  type: http
  seq: ${seq}
  tags: [
    generated
    auth
  ]
}

${method} {
  url: {{baseUrl}}${brunoPath(requestPath)}
  body: ${hasBody ? "json" : "none"}
  auth: bearer
}

auth:bearer {
  token: {{${tokenVar}}}
}
${hasBody ? `
headers {
  content-type: application/json
}

body:json {
${renderJsonBody(body)}
}
` : ""}
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

function renderBrunoLoginRequest(loginConfig, tokenVar) {
  const method = String(loginConfig.method || "POST").toLowerCase();
  const body = loginConfig.body || {};
  const tokenPaths = Array.isArray(loginConfig.tokenJsonPaths) && loginConfig.tokenJsonPaths.length > 0
    ? loginConfig.tokenJsonPaths
    : ["accessToken", "data.accessToken", "result.accessToken"];

  return `meta {
  name: ${loginConfig.name || "Login and store access token"}
  type: http
  seq: 1
  tags: [
    generated
    auth
    login
  ]
}

${method} {
  url: {{baseUrl}}${brunoPath(loginConfig.path || "/auth/login")}
  body: json
  auth: none
}

headers {
  content-type: application/json
}

body:json {
${renderJsonBody(body)}
}

script:post-response {
  const body = res.getBody();
  const readPath = (source, path) => String(path).split(".").reduce((value, key) => value && value[key], source);
  const token = ${JSON.stringify(tokenPaths)}.map((path) => readPath(body, path)).find((value) => typeof value === "string" && value.length > 0);

  if (token) {
    bru.setEnvVar(${JSON.stringify(tokenVar)}, token, { persist: true });
    bru.setVar(${JSON.stringify(tokenVar)}, token);
  }
}

tests {
  test("login returns access token", function() {
    expect(res.getStatus()).to.be.within(200, 399);
    expect(bru.getVar(${JSON.stringify(tokenVar)}) || bru.getEnvVar(${JSON.stringify(tokenVar)})).to.be.a("string").and.not.empty;
  });
}

settings {
  encodeUrl: true
  timeout: 3000
  followRedirects: true
}

docs {
  Managed by setup-agent from fixture config or entity-derived defaults.
  Run this request before generated/auth requests that use {{${tokenVar}}}.
}
`;
}

function renderBrunoEnv(vars) {
  const lines = Object.entries(vars).map(([key, value]) => `  ${key}: ${fixtureValue(value)}`);
  return `vars {\n${lines.join("\n")}\n}\n`;
}

function upsertBrunoEnvFile(filePath, vars) {
  const existing = fs.existsSync(filePath) ? fs.readFileSync(filePath, "utf8") : "";
  if (!existing.trim()) {
    writeIfMissing(filePath, renderBrunoEnv({ baseUrl: "http://localhost:8080", accessToken: "", refreshToken: "", ...vars }));
    return;
  }

  const lines = existing.split(/\r?\n/);
  const present = new Set();
  const output = [];
  let inVars = false;
  let inserted = false;

  for (const line of lines) {
    if (/^\s*vars\s*\{\s*$/.test(line)) {
      inVars = true;
      output.push(line);
      continue;
    }

    if (inVars && /^\s*}\s*$/.test(line)) {
      for (const [key, value] of Object.entries(vars)) {
        if (!present.has(key)) output.push(`  ${key}: ${fixtureValue(value)}`);
      }
      inserted = true;
      inVars = false;
      output.push(line);
      continue;
    }

    if (inVars) {
      const match = line.match(/^\s*([A-Za-z_][A-Za-z0-9_]*):/);
      if (match) present.add(match[1]);
    }
    output.push(line);
  }

  if (!inserted) {
    output.push("");
    output.push(renderBrunoEnv(vars).trimEnd());
  }

  fs.writeFileSync(filePath, `${output.join("\n").replace(/\n+$/, "")}\n`);
}

function defaultBrunoFixtureConfig(fixtureConfig, sqlFixtures, endpoints) {
  if (fixtureConfig?.bruno) return fixtureConfig;
  const context = sqlFixtures?.context;
  if (!context?.authSeed) return fixtureConfig;

  const loginEndpoint = endpoints.find((endpoint) =>
    String(endpoint.method).toUpperCase() === "POST" &&
    /\/(auth\/)?login$/i.test(String(endpoint.path)),
  );
  if (!loginEndpoint) return fixtureConfig;

  const envVars = {
    ...(context.env || {}),
    loginIdentifier: context.loginIdentifier,
    loginUserId: context.loginIdentifier,
    loginPassword: context.loginPassword,
  };
  const placeholders = (endpointPath) => [...String(endpointPath).matchAll(/\{([^}]+)\}/g)].map((match) => match[1]);
  const endpointVarsAreResolvable = (endpoint) => placeholders(endpoint.path).every((name) => Object.hasOwn(envVars, name));

  const authorizedRequests = endpoints
    .filter((endpoint) => String(endpoint.method).toUpperCase() === "GET")
    .filter((endpoint) => !/(auth\/email|files?\/|presigned|download|oauth|token)/i.test(String(endpoint.path)))
    .filter((endpoint) => {
      const endpointPath = String(endpoint.path);
      if (/\/(me|profile|account|user)(\/|$)/i.test(endpointPath)) return true;
      return placeholders(endpointPath).length > 0 && endpointVarsAreResolvable(endpoint);
    })
    .slice(0, 6)
    .map((endpoint) => ({
      name: titleFor(endpoint),
      method: endpoint.method,
      path: endpoint.path,
    }));

  if (authorizedRequests.length === 0) return fixtureConfig;

  return {
    ...(fixtureConfig || {}),
    bruno: {
      enabled: true,
      tokenVar: "accessToken",
      env: envVars,
      login: {
        name: "Login generated seed user",
        method: loginEndpoint.method,
        path: loginEndpoint.path,
        body: {
          [context.loginRequestField || "userId"]: "{{loginIdentifier}}",
          password: "{{loginPassword}}",
        },
        tokenJsonPaths: ["accessToken", "data.accessToken", "result.accessToken"],
      },
      authorizedRequests,
    },
  };
}

function writeBrunoFixtureTests(fixtureConfig) {
  const brunoConfig = fixtureConfig?.bruno;
  if (!brunoConfig || brunoConfig.enabled === false) {
    return { status: "skipped", reason: fixtureConfig ? "fixture config has no enabled bruno section" : "fixture config not found", authRequests: 0 };
  }

  const loginConfig = brunoConfig.login;
  const requests = Array.isArray(brunoConfig.authorizedRequests) ? brunoConfig.authorizedRequests : [];
  if (!loginConfig || requests.length === 0) {
    return { status: "skipped", reason: "bruno.login or bruno.authorizedRequests missing", authRequests: 0 };
  }

  const authDir = path.join(brunoGeneratedDir, "auth");
  fs.rmSync(authDir, { recursive: true, force: true });
  fs.mkdirSync(authDir, { recursive: true });
  writeFile(path.join(authDir, "folder.bru"), renderFolder("auth", 4), authDir);

  const tokenVar = brunoConfig.tokenVar || "accessToken";
  const envVars = brunoConfig.env || {};
  if (Object.keys(envVars).length > 0) {
    upsertBrunoEnvFile(path.join(brunoCollectionDir, "environments/local.example.bru"), envVars);
    upsertBrunoEnvFile(path.join(brunoCollectionDir, "environments/local.bru"), envVars);
  }

  writeFile(path.join(authDir, "001-login.bru"), renderBrunoLoginRequest(loginConfig, tokenVar), authDir);
  requests.forEach((request, index) => {
    const fileName = `${String(index + 2).padStart(3, "0")}-${slugFor(request)}.bru`;
    writeFile(path.join(authDir, fileName), renderBrunoAuthRequest(request, index + 2, tokenVar), authDir);
  });

  return { status: "generated", reason: "fixture config bruno section", authRequests: requests.length + 1 };
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
const fixtureConfigResult = readFixtureConfig();
const fixtureConfig = fixtureConfigResult.config;

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
const sqlFixtures = writeSqlFixtures(fixtureConfig);
const brunoFixtureConfig = defaultBrunoFixtureConfig(fixtureConfig, sqlFixtures, endpoints);
const brunoFixtures = writeBrunoFixtureTests(brunoFixtureConfig);

fs.mkdirSync(path.dirname(resultPath), { recursive: true });
fs.writeFileSync(resultPath, `${JSON.stringify({
  schemaVersion: 1,
  action: "generate-tests",
  status: "ok",
  timestamp: new Date().toISOString(),
  policy: path.relative(rootDir, policyPath),
  fixtureConfig: fixtureConfigResult.path || null,
  bruno: {
    generatedRoot: path.relative(rootDir, brunoGeneratedDir),
    format: "bru",
    smokeRequests: brunoCounts.smoke,
    draftRequests: brunoCounts.draft,
    scenarioRequests: brunoCounts.scenario,
    authFixtureStatus: brunoFixtures.status,
    authFixtureRequests: brunoFixtures.authRequests,
  },
  k6: {
    generatedRoot: path.relative(rootDir, k6GeneratedDir),
    smokeEndpoints: k6SmokeEndpoints.length,
    smokeScript: "tests/k6/generated/smoke.js",
  },
  sqlFixtures: {
    status: sqlFixtures.status,
    reason: sqlFixtures.reason,
    generatedRoot: "tests/sql",
    files: sqlFixtures.sqlFiles,
    seedScript: sqlFixtures.seedScript,
    warnings: sqlFixtures.warnings || [],
  },
  skippedManualOrForbidden: endpoints.length - generatedEndpoints.length,
}, null, 2)}\n`);

replaceReportSection("Generated Tests", [
  `- Generated at: \`${new Date().toISOString()}\``,
  `- Bruno smoke requests: \`${brunoCounts.smoke}\``,
  `- Bruno draft requests: \`${brunoCounts.draft}\``,
  `- Bruno scenario requests: \`${brunoCounts.scenario}\``,
  `- Bruno auth fixture status: \`${brunoFixtures.status}\``,
  `- Bruno auth fixture requests: \`${brunoFixtures.authRequests}\``,
  `- k6 smoke endpoints: \`${k6SmokeEndpoints.length}\``,
  `- SQL fixture status: \`${sqlFixtures.status}\``,
  `- SQL fixture reason: \`${sqlFixtures.reason}\``,
  `- SQL fixture files: \`${sqlFixtures.sqlFiles.length}\``,
  `- SQL seed script: \`${sqlFixtures.seedScript || "-"}\``,
  `- SQL fixture warnings: \`${(sqlFixtures.warnings || []).length}\``,
  `- Skipped manual/forbidden endpoints: \`${endpoints.length - generatedEndpoints.length}\``,
  "",
  "Bruno smoke generation includes only policy smoke endpoints with `reviewRequired: false`. Other smoke candidates are generated as draft requests so the default smoke command stays runnable.",
  "SQL fixtures are generated from explicit root `testing-fixtures.json` sql config when present; otherwise setup derives a conservative seed from JPA entities.",
  "Authenticated Bruno requests are generated from root `testing-fixtures.json` bruno config when present; otherwise setup derives a limited auth scenario when entity seed and a login endpoint are recognizable.",
  ...(sqlFixtures.warnings || []).map((warning) => `- SQL warning: ${warning}`),
].join("\n"));

console.log(`[generate] bruno smoke ${brunoCounts.smoke}, draft ${brunoCounts.draft}, scenario ${brunoCounts.scenario}`);
console.log(`[generate] bruno auth fixtures ${brunoFixtures.status} (${brunoFixtures.authRequests})`);
console.log(`[generate] k6 smoke endpoints ${k6SmokeEndpoints.length}`);
if (sqlFixtures.status === "generated") {
  console.log(`[generate] sql fixtures ${sqlFixtures.sqlFiles.concat(sqlFixtures.seedScript).join(", ")}`);
} else {
  console.log(`[generate] sql fixtures skipped: ${sqlFixtures.reason}`);
}
console.log(`[state] wrote ${path.relative(rootDir, resultPath)}`);
console.log(`[report] updated ${path.relative(rootDir, reportPath)}`);
