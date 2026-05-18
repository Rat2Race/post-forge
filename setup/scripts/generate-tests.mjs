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
const sqlGeneratedDir = path.join(rootDir, "tests/sql/generated");
const sqlManualDir = path.join(rootDir, "tests/sql/manual");
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

  writeIfMissing(path.join(brunoCollectionDir, "manual/folder.bru"), renderFolder("manual", 4));
  writeIfMissing(path.join(brunoCollectionDir, "manual/exploratory/.gitkeep"), "");
}

function ensureK6ManualArea() {
  writeIfMissing(path.join(rootDir, "tests/k6/manual/.gitkeep"), "\n");
}

function ensureSqlManualArea() {
  writeIfMissing(path.join(sqlManualDir, ".gitkeep"), "\n");
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

function isRunnableSmoke(endpoint) {
  return endpoint.class === "smoke" && endpoint.reviewRequired === false;
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
  return isRunnableSmoke(endpoint) &&
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
  const renderedRows = rows.map((row) => `(${columns.map((column) => sqlLiteral(row[column])).join(", ")})`);
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
  if (!fs.existsSync(appGradle)) return [rootDir];

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
  const match = annotations.match(new RegExp(`@${escaped}\\s*\\(([^)]*)\\)`, "s"));
  if (!match) return "";

  const attrMatch = match[1].match(new RegExp(`${attrName}\\s*=\\s*"([^"]+)"`));
  if (attrMatch) return attrMatch[1];

  const unnamed = match[1].trim().match(/^"([^"]+)"$/);
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
      .map((value) => value.trim().match(/^([A-Z][A-Z0-9_]*)\b/)?.[1])
      .filter(Boolean);
    if (values.length > 0) enums.set(match[1], values);
  }
  return enums;
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
    if (!fieldMatch) {
      annotations = [];
      continue;
    }

    const [, type, name] = fieldMatch;
    const annotationText = annotations.join("\n");
    const isManyToOne = /@ManyToOne\b/.test(annotationText);
    const isOneToOne = /@OneToOne\b/.test(annotationText);
    const isOneToMany = /@OneToMany\b/.test(annotationText);
    const isManyToMany = /@ManyToMany\b/.test(annotationText);
    const isElementCollection = /@ElementCollection\b/.test(annotationText);
    const relation = isManyToOne || isOneToOne;
    const joinColumn = annotationAttr(annotationText, "JoinColumn", "name");
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
      oneToMany: isOneToMany,
      manyToMany: isManyToMany,
      elementCollection: isElementCollection,
      collectionTable: annotationAttr(annotationText, "CollectionTable", "name"),
      collectionJoinColumn: annotationText.match(/joinColumns\s*=\s*@JoinColumn\s*\(\s*name\s*=\s*"([^"]+)"/s)?.[1] || "",
      enumerated: hasAnnotation(annotationText, "Enumerated"),
      transient: hasAnnotation(annotationText, "Transient") || /\btransient\b/.test(line),
      createdDate: hasAnnotation(annotationText, "CreatedDate"),
      lastModifiedDate: hasAnnotation(annotationText, "LastModifiedDate"),
      createdBy: hasAnnotation(annotationText, "CreatedBy"),
      lastModifiedBy: hasAnnotation(annotationText, "LastModifiedBy"),
    });
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

  return {
    entities: classes
      .filter((item) => item.kind === "entity")
      .map((entity) => ({ ...entity, fields: [...inheritedFields(entity), ...entity.fields] })),
    enums: parseJavaEnums(javaFiles),
  };
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
  return simpleTypeName(type) === "LocalDate";
}

function isUuidType(type) {
  return simpleTypeName(type) === "UUID";
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

function stringValueForField(entity, field, index) {
  const lowerName = `${field.name} ${field.column}`.toLowerCase();
  const suffix = index > 0 ? String(index + 1) : "";
  if (/password|passwd|pwd|user_pw/.test(lowerName)) {
    return "{bcrypt}$2a$10$/bF.EhbSpYgFDbAimIdPn.M0CBw.2oASjraWo00sDZWZdYvT6yuda";
  }
  if (/email/.test(lowerName)) return `testuser${index + 1}@example.test`;
  if (/(^|_)user_?id|userid|username|login|account/.test(lowerName)) return `testuser${index + 1}`;
  if (/nick|display/.test(lowerName)) return `tester${index + 1}`;
  if (/title|subject/.test(lowerName)) return `${entity.className} draft title${suffix}`;
  if (/content|body|text|message/.test(lowerName)) return `${entity.className} draft content for generated API tests.`;
  if (/summary|description|desc/.test(lowerName)) return `${entity.className} draft summary.`;
  if (/name/.test(lowerName)) return `${entity.className} draft name${suffix}`;
  if (/path|url|uri/.test(lowerName)) return `/tmp/${camelToSnake(entity.className)}-${index + 1}`;
  if (/file/.test(lowerName)) return `${camelToSnake(entity.className)}-${index + 1}.txt`;
  if (/provider/.test(lowerName)) return "local";
  if (/source/.test(lowerName)) return "generated-sql-draft";
  if (/key|token|code|secret/.test(lowerName)) return `test-${camelToSnake(field.name)}-${index + 1}`;
  if (/created_by|modified_by|author|owner/.test(lowerName)) return "setup-generated";
  return `${entity.className}-${field.name}-draft${suffix}`;
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
    if (target.className === entity.className) return field.nullableFalse ? deterministicIdValue(entity, index) : null;
    return deterministicIdValue(target, 0);
  }

  const type = simpleTypeName(field.type);
  if (enums.has(type) || field.enumerated) return enumValueForField(field, enums);
  if (isStringType(type)) return stringValueForField(entity, field, index);
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
      warnings.push(`Could not infer non-null value for ${entity.className}.${field.name}; review generated SQL before use.`);
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
    const value = enums.has(valueType)
      ? enumValueForField(field, enums)
      : stringValueForField(entity, { ...field, type: valueType, name: valueColumn, column: valueColumn }, 0);
    collections.push({
      table,
      columns: [joinColumn, valueColumn],
      rows: [{ [joinColumn]: deterministicIdValue(entity, 0), [valueColumn]: value }],
    });
  }
  return collections;
}

function buildJpaSqlDrafts() {
  const model = discoverJpaModel();
  const { entities, enums } = model;
  const draftableEntities = entities.filter((entity) => entity.fields.some((field) => field.id || field.embeddedId));
  const warnings = [];
  const context = {
    entitiesByClass: new Map(draftableEntities.map((entity) => [entity.className, entity])),
  };

  if (draftableEntities.length === 0) {
    return {
      status: "skipped",
      reason: "no @Entity classes with @Id were found for SQL draft generation",
      files: [],
      warnings: ["SQL draft generation requires JPA @Entity classes with @Id."],
    };
  }

  const orderedEntities = sortEntitiesByDependencies(draftableEntities);
  const entityInserts = [];
  const collectionInserts = [];
  for (const entity of orderedEntities) {
    const built = buildEntityRows(entity, context, enums);
    warnings.push(...built.warnings);
    if (built.columns.length > 0) entityInserts.push({ entity, ...built });
    collectionInserts.push(...buildElementCollectionRows(entity, context, enums));
  }

  const resetLines = [
    "-- Generated SQL draft reset statements.",
    "-- Review before running against any database.",
    "",
  ];
  for (const collection of [...collectionInserts].reverse()) {
    resetLines.push(`DELETE FROM ${collection.table} WHERE ${collection.columns[0]} = ${sqlLiteral(collection.rows[0][collection.columns[0]])};`);
  }
  for (const item of [...entityInserts].reverse()) {
    const idCol = idColumn(item.entity);
    resetLines.push(`DELETE FROM ${item.entity.table} WHERE ${idCol} = ${sqlLiteral(deterministicIdValue(item.entity, 0))};`);
  }

  const insertLines = [
    "-- Generated SQL draft insert statements from discovered JPA entities.",
    "-- This file is not executed by setup; review and move/adapt SQL under tests/sql/manual when needed.",
    "-- Password-like columns use a deterministic bcrypt sample for local-only review.",
    "",
  ];
  for (const item of entityInserts) {
    insertLines.push(renderInsert(item.entity.table, item.columns, item.rows), "");
  }
  for (const item of collectionInserts) {
    insertLines.push(renderInsert(item.table, item.columns, item.rows), "");
  }

  return {
    status: "generated",
    reason: "JPA entity SQL draft",
    files: [
      { name: "001-reset-draft.sql", content: `${resetLines.join("\n").trimEnd()}\n` },
      { name: "002-jpa-insert-draft.sql", content: `${insertLines.join("\n").replace(/\n{3,}/g, "\n\n").trimEnd()}\n` },
    ],
    warnings,
  };
}

function writeSqlDrafts() {
  resetDir(sqlGeneratedDir);
  ensureSqlManualArea();

  const drafts = buildJpaSqlDrafts();
  const files = [];
  for (const file of drafts.files) {
    const filePath = path.join(sqlGeneratedDir, file.name);
    writeFile(filePath, file.content, sqlGeneratedDir);
    files.push(path.relative(rootDir, filePath));
  }

  return {
    ...drafts,
    generatedRoot: path.relative(rootDir, sqlGeneratedDir),
    manualRoot: path.relative(rootDir, sqlManualDir),
    files,
  };
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
const runnableSmokeEndpoints = endpoints.filter(isRunnableSmoke);
const smokeReviewCandidates = endpoints.filter((endpoint) => endpoint.class === "smoke" && endpoint.reviewRequired);
const scenarioEndpoints = endpoints.filter((endpoint) => endpoint.class === "scenario");
const manualOrForbiddenEndpoints = endpoints.filter((endpoint) => endpoint.class === "manual" || endpoint.class === "forbidden");
const k6SmokeEndpoints = endpoints.filter(isK6SafeSmoke);

ensureBrunoNativeCollection();
ensureK6ManualArea();
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
const sqlDrafts = writeSqlDrafts();

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
    runnableSmokeEndpoints: runnableSmokeEndpoints.length,
    smokeReviewCandidates: smokeReviewCandidates.length,
    scenarioEndpoints: scenarioEndpoints.length,
  },
  k6: {
    generatedRoot: path.relative(rootDir, k6GeneratedDir),
    smokeEndpoints: k6SmokeEndpoints.length,
    smokeScript: "tests/k6/generated/smoke.js",
    policy: "GET-only, reviewRequired=false, no path variables",
  },
  sqlDrafts: {
    status: sqlDrafts.status,
    reason: sqlDrafts.reason,
    generatedRoot: sqlDrafts.generatedRoot,
    manualRoot: sqlDrafts.manualRoot,
    files: sqlDrafts.files,
    warnings: sqlDrafts.warnings || [],
  },
  skippedManualOrForbidden: manualOrForbiddenEndpoints.length,
}, null, 2)}\n`);

replaceReportSection("Generated Tests", [
  `- Generated at: \`${new Date().toISOString()}\``,
  `- Bruno smoke requests: \`${brunoCounts.smoke}\``,
  `- Bruno draft requests: \`${brunoCounts.draft}\``,
  `- Bruno scenario requests: \`${brunoCounts.scenario}\``,
  `- Runnable smoke endpoints: \`${runnableSmokeEndpoints.length}\``,
  `- Smoke candidates requiring review: \`${smokeReviewCandidates.length}\``,
  `- Scenario endpoints kept out of smoke: \`${scenarioEndpoints.length}\``,
  `- k6 smoke endpoints: \`${k6SmokeEndpoints.length}\``,
  `- SQL draft status: \`${sqlDrafts.status}\``,
  `- SQL draft reason: \`${sqlDrafts.reason}\``,
  `- SQL draft files: \`${sqlDrafts.files.length}\``,
  `- SQL draft warnings: \`${(sqlDrafts.warnings || []).length}\``,
  `- Skipped manual/forbidden endpoints: \`${manualOrForbiddenEndpoints.length}\``,
  "",
  "Bruno smoke generation includes only policy smoke endpoints with `reviewRequired: false`. Other smoke candidates are generated as draft requests so the default smoke command stays runnable.",
  "k6 generated smoke is stricter: it includes only GET smoke endpoints without path variables. Scenario/manual flows stay outside automatic smoke and should be promoted under `tests/k6/manual` or Bruno `manual/` after fixtures and cleanup are defined.",
  "SQL draft generation reads discovered JPA entities and writes reviewable SQL under `tests/sql/generated`; setup does not execute those SQL files.",
  ...(sqlDrafts.warnings || []).map((warning) => `- SQL draft warning: ${warning}`),
].join("\n"));

console.log(`[generate] bruno smoke ${brunoCounts.smoke}, draft ${brunoCounts.draft}, scenario ${brunoCounts.scenario}`);
console.log(`[generate] k6 smoke endpoints ${k6SmokeEndpoints.length}`);
console.log(`[generate] sql drafts ${sqlDrafts.status} (${sqlDrafts.files.length})`);
console.log(`[state] wrote ${path.relative(rootDir, resultPath)}`);
console.log(`[report] updated ${path.relative(rootDir, reportPath)}`);
