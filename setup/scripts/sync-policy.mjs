#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";

const [rootDir, inventoryPath, policyPath, resultPath, reportPath] = process.argv.slice(2);

if (!rootDir || !inventoryPath || !policyPath || !resultPath || !reportPath) {
  console.error("Usage: sync-policy.mjs <rootDir> <inventoryPath> <policyPath> <resultPath> <reportPath>");
  process.exit(1);
}

const METHOD_ORDER = ["GET", "HEAD", "OPTIONS", "POST", "PUT", "PATCH", "DELETE", "TRACE", "ANY"];
const OPENAPI_METHODS = new Set(["GET", "HEAD", "OPTIONS", "POST", "PUT", "PATCH", "DELETE", "TRACE"]);

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, "utf8"));
}

function quoteYaml(value) {
  return JSON.stringify(String(value));
}

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

function parseExistingEndpoints(policyText) {
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
  return endpoints.filter((endpoint) => endpoint.method && endpoint.path);
}

function headerWithoutEndpoints(policyText) {
  const endpointsMatch = policyText.match(/^endpoints:\s*(?:\[\])?\s*$/m);
  if (!endpointsMatch) {
    return `${policyText.trimEnd()}\n\n`;
  }

  return `${policyText.slice(0, endpointsMatch.index).trimEnd()}\n\n`;
}

function normalizeSpringPath(input) {
  let value = String(input || "").trim();

  if (!value) return "";
  value = value.replace(/\{([^}:]+):[^}]+\}/g, "{$1}");
  value = value.replace(/\\/g, "");
  return value;
}

function joinPaths(basePath, childPath) {
  const base = normalizeSpringPath(basePath);
  const child = normalizeSpringPath(childPath);

  if (!base && !child) return "/";
  if (!base) return child.startsWith("/") ? child : `/${child}`;
  if (!child) return base.startsWith("/") ? base : `/${base}`;

  const joined = `${base.replace(/\/+$/, "")}/${child.replace(/^\/+/, "")}`;
  return joined.replace(/\/+/g, "/");
}

function normalizeOpenApiPath(input) {
  let value = String(input || "").trim();

  if (!value) return "/";
  if (!value.startsWith("/")) value = `/${value}`;
  return value.replace(/\/+/g, "/");
}

function lineForJsonKey(text, key, startLine = 0) {
  const lines = text.split(/\r?\n/);
  const escaped = JSON.stringify(String(key));

  for (let index = startLine; index < lines.length; index += 1) {
    if (lines[index].includes(escaped)) return index + 1;
  }

  return undefined;
}

function discoverOpenApiJsonEndpoints(text, sourceFile) {
  const document = JSON.parse(text);
  const paths = document?.paths;
  if (!paths || typeof paths !== "object") return [];

  const endpoints = [];
  for (const [endpointPath, pathItem] of Object.entries(paths)) {
    if (!pathItem || typeof pathItem !== "object") continue;

    const pathLine = lineForJsonKey(text, endpointPath);
    for (const methodName of Object.keys(pathItem)) {
      const method = methodName.toUpperCase();
      if (!OPENAPI_METHODS.has(method)) continue;

      const line = lineForJsonKey(text, methodName, Math.max(0, Number(pathLine || 1) - 1));
      const normalizedPath = normalizeOpenApiPath(endpointPath);
      endpoints.push({
        method,
        path: normalizedPath,
        ...classifyEndpoint(method, normalizedPath),
        source: {
          type: "openapi",
          file: sourceFile,
          ...(line ? { line } : {}),
        },
      });
    }
  }

  return endpoints;
}

function yamlIndent(line) {
  return line.match(/^\s*/)?.[0].length || 0;
}

function yamlKey(line) {
  const trimmed = line.trim();
  if (!trimmed || trimmed.startsWith("#") || trimmed.startsWith("- ")) return null;

  const match = trimmed.match(/^("([^"\\]|\\.)*"|'[^']*'|[^:]+)\s*:/);
  if (!match) return null;

  const rawKey = match[1].trim();
  if (rawKey.startsWith('"') && rawKey.endsWith('"')) {
    try {
      return JSON.parse(rawKey);
    } catch {
      return rawKey.slice(1, -1);
    }
  }
  if (rawKey.startsWith("'") && rawKey.endsWith("'")) return rawKey.slice(1, -1);
  return rawKey;
}

function discoverOpenApiYamlEndpoints(text, sourceFile) {
  const lines = text.split(/\r?\n/);
  const endpoints = [];
  let inPaths = false;
  let pathsIndent = -1;
  let currentPath = "";
  let currentPathIndent = -1;

  for (let index = 0; index < lines.length; index += 1) {
    const line = lines[index];
    if (!line.trim() || line.trim().startsWith("#")) continue;

    const indent = yamlIndent(line);
    const key = yamlKey(line);
    if (!key) continue;

    if (!inPaths) {
      if (key === "paths") {
        inPaths = true;
        pathsIndent = indent;
      }
      continue;
    }

    if (indent <= pathsIndent) break;

    if (String(key).startsWith("/") && indent > pathsIndent) {
      currentPath = normalizeOpenApiPath(key);
      currentPathIndent = indent;
      continue;
    }

    const method = String(key).toUpperCase();
    if (currentPath && indent > currentPathIndent && OPENAPI_METHODS.has(method)) {
      endpoints.push({
        method,
        path: currentPath,
        ...classifyEndpoint(method, currentPath),
        source: {
          type: "openapi",
          file: sourceFile,
          line: index + 1,
        },
      });
    }
  }

  return endpoints;
}

function discoverOpenApiEndpoints(inventory) {
  const discovered = new Map();

  for (const entry of inventory.openapiFiles || []) {
    const sourceFile = String(entry.path || entry.file || "");
    if (!sourceFile) continue;

    const absolutePath = path.resolve(rootDir, sourceFile);
    if (!fs.existsSync(absolutePath)) continue;

    const text = fs.readFileSync(absolutePath, "utf8");
    const trimmed = text.trimStart();
    let endpoints = [];

    try {
      if (sourceFile.toLowerCase().endsWith(".json") || trimmed.startsWith("{")) {
        endpoints = discoverOpenApiJsonEndpoints(text, sourceFile);
      } else {
        endpoints = discoverOpenApiYamlEndpoints(text, sourceFile);
      }
    } catch (error) {
      console.warn(`[policy] could not parse OpenAPI file ${sourceFile}: ${error.message}`);
      continue;
    }

    for (const endpoint of endpoints) {
      discovered.set(endpointKey(endpoint), endpoint);
    }
  }

  return [...discovered.values()];
}

function endpointKey(endpoint) {
  return `${String(endpoint.method).toUpperCase()} ${endpoint.path}`;
}

function isAuthFlow(method, endpointPath) {
  if (method !== "POST") return false;
  return /\/(auth|oauth|token|session)(\/|$)/i.test(endpointPath) &&
    /(login|token|reissue|refresh|exchange|session)/i.test(endpointPath);
}

function classifyEndpoint(method, endpointPath) {
  const hasPathVariable = endpointPath.includes("{") && endpointPath.includes("}");
  const lowerPath = endpointPath.toLowerCase();

  if (lowerPath.includes("/internal/") || lowerPath.startsWith("/internal/")) {
    return {
      class: "manual",
      confidence: "medium",
      reviewRequired: true,
      reason: "Internal route candidate; keep out of automatic smoke/scenario until reviewed.",
    };
  }

  if (["GET", "HEAD", "OPTIONS"].includes(method)) {
    if (hasPathVariable) {
      return {
        class: "draft",
        confidence: "medium",
        reviewRequired: true,
        reason: "Read-only route has path variables; requires known test data before smoke use.",
      };
    }

    if (/(verify|presigned|download|callback)/i.test(endpointPath)) {
      return {
        class: "draft",
        confidence: "medium",
        reviewRequired: true,
        reason: "Read-like route appears action-oriented or parameter-dependent; review before smoke use.",
      };
    }

    const reviewRequired = /\/(user|me|profile|admin|file|files)\b/i.test(endpointPath);
    return {
      class: "smoke",
      confidence: reviewRequired ? "medium" : "high",
      reviewRequired,
      reason: reviewRequired
        ? "Read-only route candidate; auth or parameter prerequisites should be reviewed."
        : "Read-only route candidate without path variables.",
    };
  }

  if (isAuthFlow(method, endpointPath)) {
    return {
      class: "smoke",
      confidence: "medium",
      reviewRequired: true,
      reason: "Authentication flow candidate; local credentials/secrets must be supplied by the user.",
    };
  }

  return {
    class: "scenario",
    confidence: "medium",
    reviewRequired: true,
    reason: "Write route candidate; requires isolated test data and cleanup strategy.",
  };
}

function discoverEndpoints(inventory) {
  const openApiEndpoints = discoverOpenApiEndpoints(inventory);
  if (openApiEndpoints.length > 0) {
    return {
      endpoints: openApiEndpoints,
      source: "openapi",
    };
  }

  const candidates = [...(inventory.routeCandidates || [])].sort((a, b) => {
    if (a.file !== b.file) return a.file.localeCompare(b.file);
    return Number(a.line) - Number(b.line);
  });

  const baseByFile = new Map();
  const discovered = new Map();

  for (const candidate of candidates) {
    const file = candidate.file;
    const method = String(candidate.method || "ANY").toUpperCase();
    const raw = String(candidate.raw || "");
    const pathHint = normalizeSpringPath(candidate.pathHint || "");

    if (method === "ANY" && raw.includes("@RequestMapping") && pathHint) {
      baseByFile.set(file, pathHint);
      continue;
    }

    const isRoute = /@(GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping|RequestMapping)/.test(raw) ||
      /\.(get|post|put|patch|delete|route)\s*\(/.test(raw);

    if (!isRoute || method === "ANY") continue;

    const endpointPath = joinPaths(baseByFile.get(file) || "", pathHint);
    const classified = classifyEndpoint(method, endpointPath);
    const endpoint = {
      method,
      path: endpointPath,
      ...classified,
      source: {
        type: "controllers",
        file,
        line: Number(candidate.line),
      },
    };

    discovered.set(endpointKey(endpoint), endpoint);
  }

  return {
    endpoints: [...discovered.values()],
    source: "controllers",
  };
}

function mergePolicy(existingEndpoints, discoveredEndpoints) {
  const existingByKey = new Map(existingEndpoints.map((endpoint) => [endpointKey(endpoint), endpoint]));
  const discoveredByKey = new Map(discoveredEndpoints.map((endpoint) => [endpointKey(endpoint), endpoint]));
  const merged = [];
  const added = [];
  const stale = [];
  const conflicts = [];
  const preserved = [];

  for (const existing of existingEndpoints) {
    const key = endpointKey(existing);
    const discovered = discoveredByKey.get(key);

    if (!discovered) {
      const next = { ...existing, stale: true };
      merged.push(next);
      stale.push(key);
      continue;
    }

    const next = {
      ...discovered,
      ...existing,
      source: existing.source || discovered.source,
      stale: false,
    };

    if (existing.class && existing.class !== discovered.class) {
      next.suggestedClass = discovered.class;
      next.reviewRequired = true;
      conflicts.push(key);
    } else {
      preserved.push(key);
    }

    merged.push(next);
  }

  for (const discovered of discoveredEndpoints) {
    const key = endpointKey(discovered);
    if (existingByKey.has(key)) continue;

    merged.push(discovered);
    added.push(key);
  }

  merged.sort((a, b) => {
    const pathCompare = String(a.path).localeCompare(String(b.path));
    if (pathCompare !== 0) return pathCompare;
    return METHOD_ORDER.indexOf(a.method) - METHOD_ORDER.indexOf(b.method);
  });

  return { merged, added, stale, conflicts, preserved };
}

function renderEndpoint(endpoint) {
  const lines = [];
  lines.push(`  - method: ${String(endpoint.method).toUpperCase()}`);
  lines.push(`    path: ${quoteYaml(endpoint.path)}`);
  lines.push(`    class: ${endpoint.class}`);
  lines.push(`    confidence: ${endpoint.confidence}`);
  lines.push(`    reason: ${quoteYaml(endpoint.reason || "")}`);
  lines.push(`    reviewRequired: ${endpoint.reviewRequired ? "true" : "false"}`);

  if (endpoint.stale) {
    lines.push("    stale: true");
  }

  if (endpoint.suggestedClass) {
    lines.push(`    suggestedClass: ${endpoint.suggestedClass}`);
  }

  if (endpoint.source) {
    lines.push("    source:");
    if (endpoint.source.type) lines.push(`      type: ${endpoint.source.type}`);
    if (endpoint.source.file) lines.push(`      file: ${quoteYaml(endpoint.source.file)}`);
    if (endpoint.source.line) lines.push(`      line: ${endpoint.source.line}`);
  }

  return lines.join("\n");
}

function writePolicy(policyText, endpoints) {
  const header = headerWithoutEndpoints(policyText);
  const body = endpoints.length === 0
    ? "endpoints: []\n"
    : `endpoints:\n${endpoints.map(renderEndpoint).join("\n")}\n`;

  fs.mkdirSync(path.dirname(policyPath), { recursive: true });
  fs.writeFileSync(policyPath, `${header}${body}`);
}

function writeResult(result, discoveredCount, existingCount, discoverySource) {
  fs.mkdirSync(path.dirname(resultPath), { recursive: true });
  fs.writeFileSync(resultPath, `${JSON.stringify({
    schemaVersion: 1,
    action: "sync-policy",
    status: "ok",
    timestamp: new Date().toISOString(),
    inventory: path.relative(rootDir, inventoryPath),
    policy: path.relative(rootDir, policyPath),
    discoverySource,
    discoveredEndpoints: discoveredCount,
    existingEndpoints: existingCount,
    addedEndpoints: result.added.length,
    staleEndpoints: result.stale.length,
    conflicts: result.conflicts.length,
    preservedEndpoints: result.preserved.length,
    added: result.added,
    stale: result.stale,
    conflictKeys: result.conflicts,
  }, null, 2)}\n`);
}

function appendReport(result, discoveredCount, existingCount, discoverySource) {
  fs.mkdirSync(path.dirname(reportPath), { recursive: true });
  let existingReport = fs.existsSync(reportPath) ? fs.readFileSync(reportPath, "utf8") : "";
  const marker = "\n## Policy Sync\n";
  const markerIndex = existingReport.indexOf(marker);

  if (markerIndex >= 0) {
    existingReport = `${existingReport.slice(0, markerIndex).trimEnd()}\n`;
  }

  const report = [
    "## Policy Sync",
    "",
    `- Synced at: \`${new Date().toISOString()}\``,
    `- Discovery source: \`${discoverySource}\``,
    `- Discovered endpoints: \`${discoveredCount}\``,
    `- Existing endpoints before sync: \`${existingCount}\``,
    `- Added endpoints: \`${result.added.length}\``,
    `- Stale endpoints: \`${result.stale.length}\``,
    `- Conflicts requiring review: \`${result.conflicts.length}\``,
    "",
    "Policy sync prefers OpenAPI paths when an OpenAPI/Swagger file exists. If no OpenAPI endpoints are available, it falls back to controller route candidates. Existing endpoint classes are preserved.",
    "",
  ].join("\n");

  fs.writeFileSync(reportPath, `${existingReport.trimEnd()}\n\n${report}`);
}

const inventory = readJson(inventoryPath);
const policyText = fs.existsSync(policyPath) ? fs.readFileSync(policyPath, "utf8") : "endpoints: []\n";
const existingEndpoints = parseExistingEndpoints(policyText);
const discovery = discoverEndpoints(inventory);
const discoveredEndpoints = discovery.endpoints;
const result = mergePolicy(existingEndpoints, discoveredEndpoints);

writePolicy(policyText, result.merged);
writeResult(result, discoveredEndpoints.length, existingEndpoints.length, discovery.source);
appendReport(result, discoveredEndpoints.length, existingEndpoints.length, discovery.source);

console.log(`[policy] discovery source ${discovery.source}`);
console.log(`[policy] discovered ${discoveredEndpoints.length} endpoints`);
console.log(`[policy] added ${result.added.length}, stale ${result.stale.length}, conflicts ${result.conflicts.length}`);
console.log(`[state] wrote ${path.relative(rootDir, resultPath)}`);
console.log(`[report] updated ${path.relative(rootDir, reportPath)}`);
