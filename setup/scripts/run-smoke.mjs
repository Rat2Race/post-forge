#!/usr/bin/env node
import fs from "node:fs";
import http from "node:http";
import https from "node:https";
import path from "node:path";
import { spawnSync } from "node:child_process";

const [rootDirArg, resultPathArg, reportPathArg] = process.argv.slice(2);

if (!rootDirArg || !resultPathArg || !reportPathArg) {
  console.error("Usage: run-smoke.mjs <rootDir> <resultPath> <reportPath>");
  process.exit(1);
}

const rootDir = path.resolve(rootDirArg);
const resultPath = path.resolve(rootDir, resultPathArg);
const reportPath = path.resolve(rootDir, reportPathArg);
const stateDir = path.dirname(resultPath);
const baseUrl = process.env.BASE_URL || "http://localhost:8080";
const failOnError = process.env.SMOKE_FAIL_ON_ERROR === "true";
const brunoVerbose = process.env.BRUNO_VERBOSE === "true";
const brunoCollectionDir = path.join(rootDir, "tests/bruno/api");
const k6ScriptPath = path.join(rootDir, "tests/k6/generated/smoke.js");
const brunoEnvPath = path.join(stateDir, "bruno-smoke-env.bru");
const brunoReportJsonPath = path.join(stateDir, "bruno-smoke-report.json");
const k6SummaryPath = path.join(stateDir, "k6-smoke-summary.json");
const k6ReportDir = path.join(path.dirname(reportPath), "k6");
const k6GeneratedSummaryDir = path.join(stateDir, "k6");

function now() {
  return new Date().toISOString();
}

function commandExists(command) {
  const result = spawnSync("bash", ["-lc", `command -v ${command}`], { encoding: "utf8" });
  return result.status === 0 ? result.stdout.trim() : "";
}

function probeBaseUrl(url) {
  return new Promise((resolve) => {
    let parsed;
    try {
      parsed = new URL(url);
    } catch (error) {
      resolve({ ok: false, error: `Invalid BASE_URL: ${error.message}` });
      return;
    }

    const client = parsed.protocol === "https:" ? https : http;
    const request = client.request(
      {
        method: "GET",
        hostname: parsed.hostname,
        port: parsed.port || (parsed.protocol === "https:" ? 443 : 80),
        path: parsed.pathname === "/" ? "/" : parsed.pathname,
        timeout: 2500,
      },
      (response) => {
        response.resume();
        resolve({
          ok: response.statusCode >= 200 && response.statusCode < 500,
          statusCode: response.statusCode,
        });
      },
    );

    request.on("timeout", () => {
      request.destroy(new Error("timeout"));
    });
    request.on("error", (error) => {
      resolve({ ok: false, error: error.message });
    });
    request.end();
  });
}

function runCommand(name, command, args, options = {}) {
  const startedAt = Date.now();
  const result = spawnSync(command, args, {
    cwd: options.cwd || rootDir,
    env: { ...process.env, ...(options.env || {}) },
    encoding: "utf8",
    maxBuffer: 1024 * 1024 * 20,
  });

  return {
    name,
    command: [command, ...args].join(" "),
    cwd: path.relative(rootDir, options.cwd || rootDir) || ".",
    exitCode: result.status,
    durationMs: Date.now() - startedAt,
    stdout: result.stdout || "",
    stderr: result.stderr || "",
    error: result.error ? result.error.message : null,
  };
}

function writeBrunoEnv() {
  fs.mkdirSync(stateDir, { recursive: true });
  fs.writeFileSync(brunoEnvPath, `vars {
  baseUrl: ${baseUrl}
  accessToken:
  refreshToken:
}
`);
}

function readJsonIfExists(filePath) {
  if (!fs.existsSync(filePath)) return null;
  try {
    return JSON.parse(fs.readFileSync(filePath, "utf8"));
  } catch {
    return null;
  }
}

function metricValue(summary, metricName, key) {
  const metric = summary?.metrics?.[metricName];
  if (!metric) return null;

  if (metric.values && Object.hasOwn(metric.values, key)) {
    return metric.values[key];
  }

  if (Object.hasOwn(metric, key)) {
    return metric[key];
  }

  if (key === "rate" && Object.hasOwn(metric, "value")) {
    return metric.value;
  }

  return null;
}

function analyzeK6(summary) {
  if (!summary) return null;

  const failedRate = metricValue(summary, "http_req_failed", "rate");
  const checksRate = metricValue(summary, "checks", "rate");
  const p95 = metricValue(summary, "http_req_duration", "p(95)");
  const avg = metricValue(summary, "http_req_duration", "avg");
  const max = metricValue(summary, "http_req_duration", "max");
  const httpReqs = metricValue(summary, "http_reqs", "count");

  return {
    httpReqs,
    failedRate,
    checksRate,
    durationMs: { avg, p95, max },
  };
}

function classifyFindings({ probe, bruno, k6, k6Analysis }) {
  const findings = [];
  const improvements = [];
  const smokePassed = bruno?.status === "passed" || k6?.status === "passed";

  if (!probe.ok && !smokePassed) {
    findings.push({
      severity: "error",
      area: "target",
      message: `BASE_URL is not reachable: ${probe.error || `HTTP ${probe.statusCode}`}`,
    });
    improvements.push("Spring Boot 앱이 `BASE_URL`에서 실행 중인지 확인한다. DB/Redis만 떠 있어도 API smoke는 실패한다.");
  } else if (!probe.ok && smokePassed) {
    findings.push({
      severity: "info",
      area: "target",
      message: `BASE_URL root probe returned ${probe.error || `HTTP ${probe.statusCode}`}, but generated smoke endpoints passed.`,
    });
    improvements.push("루트 경로(`/`)가 500을 반환한다면 health endpoint를 별도로 두거나 smoke probe 대상에서 제외한다.");
  }

  if (bruno && bruno.exitCode !== 0) {
    findings.push({
      severity: "error",
      area: "bruno",
      message: "Bruno smoke failed.",
    });
    if (/ECONNREFUSED|connect|timeout|ENOTFOUND/i.test(`${bruno.stdout}\n${bruno.stderr}`)) {
      improvements.push("Bruno 연결 실패는 앱 서버 미실행, 잘못된 baseUrl, 포트 충돌 가능성이 높다.");
    } else if (/401|403|Unauthorized|Forbidden/i.test(`${bruno.stdout}\n${bruno.stderr}`)) {
      improvements.push("Bruno 인증 실패는 smoke endpoint 분류를 재검토하거나 accessToken이 필요한 요청을 draft로 내린다.");
    } else {
      improvements.push("Bruno 실패 로그에서 첫 번째 실패 request를 확인하고 `tests/testing-policy.yml`의 class/reviewRequired를 조정한다.");
    }
  }

  if (k6 && k6.exitCode !== 0) {
    findings.push({
      severity: "error",
      area: "k6",
      message: "k6 smoke failed.",
    });
  }

  if (k6Analysis) {
    const failedRate = Number(k6Analysis.failedRate ?? 0);
    const checksRate = Number(k6Analysis.checksRate ?? 1);
    const p95 = Number(k6Analysis.durationMs.p95 ?? 0);
    const avg = Number(k6Analysis.durationMs.avg ?? 0);

    if (failedRate > 0) {
      findings.push({
        severity: "error",
        area: "k6",
        message: `k6 request failure rate is ${(failedRate * 100).toFixed(2)}%.`,
      });
      improvements.push("실패율이 있으면 HTTP status, 애플리케이션 로그, DB/Redis 연결 상태를 먼저 확인한다.");
    }

    if (checksRate < 1) {
      findings.push({
        severity: "error",
        area: "k6",
        message: `k6 check pass rate is ${(checksRate * 100).toFixed(2)}%.`,
      });
    }

    if (p95 > 1000) {
      findings.push({
        severity: "warn",
        area: "performance",
        message: `p95 latency is ${p95.toFixed(2)}ms, above the smoke threshold target of 1000ms.`,
      });
      improvements.push("p95가 높으면 slow query, N+1 조회, Redis miss, 불필요한 외부 호출 여부를 확인한다.");
    } else if (avg > 500) {
      findings.push({
        severity: "info",
        area: "performance",
        message: `average latency is ${avg.toFixed(2)}ms; watch this before raising load.`,
      });
      improvements.push("평균 응답시간이 높으면 먼저 단일 endpoint의 DB index와 response size를 점검한다.");
    }
  }

  if (findings.length === 0) {
    findings.push({
      severity: "ok",
      area: "smoke",
      message: "Bruno and k6 smoke checks completed without detected failures.",
    });
    improvements.push("다음 단계는 draft 요청의 전제값을 채우고 scenario harness 후보를 좁히는 것이다.");
  }

  return { findings, improvements: [...new Set(improvements)] };
}

function tail(text, maxLines = 80) {
  return String(text || "").split(/\r?\n/).slice(-maxLines).join("\n");
}

function renderReport(result) {
  const k6 = result.k6.analysis;
  const lines = [
    "# Testing Run Report",
    "",
    `- Generated at: \`${result.timestamp}\``,
    `- BASE_URL: \`${result.baseUrl}\``,
    "- Scope: generated smoke only; draft/scenario/manual flows are excluded.",
    `- Target probe: \`${result.targetProbe.ok ? "ok" : "failed"}\``,
    `- Bruno: \`${result.bruno.status}\``,
    `- k6: \`${result.k6.status}\``,
    "",
    "## Summary",
    "",
    `- Bruno exit code: \`${result.bruno.exitCode ?? "skipped"}\``,
    `- Bruno duration: \`${result.bruno.durationMs ?? 0}ms\``,
    `- k6 exit code: \`${result.k6.exitCode ?? "skipped"}\``,
    `- k6 duration: \`${result.k6.durationMs ?? 0}ms\``,
  ];

  if (k6) {
    lines.push(`- k6 http requests: \`${k6.httpReqs ?? "n/a"}\``);
    lines.push(`- k6 failed rate: \`${k6.failedRate == null ? "n/a" : `${(k6.failedRate * 100).toFixed(2)}%`}\``);
    lines.push(`- k6 check pass rate: \`${k6.checksRate == null ? "n/a" : `${(k6.checksRate * 100).toFixed(2)}%`}\``);
    lines.push(`- k6 avg latency: \`${k6.durationMs.avg == null ? "n/a" : `${k6.durationMs.avg.toFixed(2)}ms`}\``);
    lines.push(`- k6 p95 latency: \`${k6.durationMs.p95 == null ? "n/a" : `${k6.durationMs.p95.toFixed(2)}ms`}\``);
    lines.push(`- k6 max latency: \`${k6.durationMs.max == null ? "n/a" : `${k6.durationMs.max.toFixed(2)}ms`}\``);
  }

  lines.push("", "## Findings", "");
  for (const finding of result.findings) {
    lines.push(`- ${finding.severity.toUpperCase()} [${finding.area}] ${finding.message}`);
  }

  lines.push("", "## Improvement Directions", "");
  for (const improvement of result.improvements) {
    lines.push(`- ${improvement}`);
  }

  lines.push("", "## Command Output Tails", "");
  lines.push("### Bruno", "", "```text", tail(`${result.bruno.stdout}\n${result.bruno.stderr}`), "```", "");
  lines.push("### k6", "", "```text", tail(`${result.k6.stdout}\n${result.k6.stderr}`), "```", "");

  return `${lines.join("\n")}\n`;
}

fs.mkdirSync(stateDir, { recursive: true });

const probe = await probeBaseUrl(baseUrl);
const bruPath = commandExists("bru");
const k6Path = commandExists("k6");

let bruno = {
  status: "skipped",
  exitCode: null,
  durationMs: 0,
  stdout: "",
  stderr: "",
  command: null,
};

if (bruPath && fs.existsSync(brunoCollectionDir)) {
  writeBrunoEnv();
  fs.rmSync(brunoReportJsonPath, { force: true });
  const brunoArgs = [
    "run",
    "--tags=smoke",
    "--exclude-tags=draft",
    "--exclude-tags=scenario",
    "--exclude-tags=requires-review",
    "--env-file",
    brunoEnvPath,
    "--reporter-json",
    brunoReportJsonPath,
  ];
  if (brunoVerbose) {
    brunoArgs.push("--verbose");
  }
  bruno = runCommand(
    "bruno",
    bruPath,
    brunoArgs,
    { cwd: brunoCollectionDir },
  );
  bruno.status = bruno.exitCode === 0 ? "passed" : "failed";
} else {
  bruno.stderr = "Bruno CLI or collection directory not found.";
}

let k6 = {
  status: "skipped",
  exitCode: null,
  durationMs: 0,
  stdout: "",
  stderr: "",
  command: null,
  analysis: null,
};

if (k6Path && fs.existsSync(k6ScriptPath)) {
  fs.rmSync(k6SummaryPath, { force: true });
  fs.mkdirSync(k6ReportDir, { recursive: true });
  fs.mkdirSync(k6GeneratedSummaryDir, { recursive: true });
  k6 = runCommand(
    "k6",
    k6Path,
    ["run", "--summary-export", k6SummaryPath, k6ScriptPath],
    {
      env: {
        BASE_URL: baseUrl,
        K6_REPORT_DIR: path.relative(rootDir, k6ReportDir),
        K6_SUMMARY_DIR: path.relative(rootDir, k6GeneratedSummaryDir),
        K6_REPORT_NAME: "setup-smoke",
      },
    },
  );
  k6.status = k6.exitCode === 0 ? "passed" : "failed";
  k6.analysis = analyzeK6(readJsonIfExists(k6SummaryPath));
} else {
  k6.stderr = "k6 CLI or generated smoke script not found.";
}

const { findings, improvements } = classifyFindings({ probe, bruno, k6, k6Analysis: k6.analysis });
const status = findings.some((finding) => finding.severity === "error") ? "failed" : "ok";

const result = {
  schemaVersion: 1,
  action: "run-smoke",
  status,
  timestamp: now(),
  baseUrl,
  targetProbe: probe,
  bruno: {
    status: bruno.status,
    exitCode: bruno.exitCode,
    durationMs: bruno.durationMs,
    command: bruno.command,
    cwd: bruno.cwd,
    reporterJson: fs.existsSync(brunoReportJsonPath) ? path.relative(rootDir, brunoReportJsonPath) : null,
    stdout: bruno.stdout,
    stderr: bruno.stderr,
  },
  k6: {
    status: k6.status,
    exitCode: k6.exitCode,
    durationMs: k6.durationMs,
    command: k6.command,
    cwd: k6.cwd,
    stdout: k6.stdout,
    stderr: k6.stderr,
    summary: path.relative(rootDir, k6SummaryPath),
    analysis: k6.analysis,
  },
  findings,
  improvements,
};

fs.writeFileSync(resultPath, `${JSON.stringify(result, null, 2)}\n`);
fs.mkdirSync(path.dirname(reportPath), { recursive: true });
fs.writeFileSync(reportPath, renderReport(result));

console.log(`[smoke] status: ${status}`);
console.log(`[smoke] bruno: ${bruno.status}`);
console.log(`[smoke] k6: ${k6.status}`);
console.log(`[state] wrote ${path.relative(rootDir, resultPath)}`);
console.log(`[report] wrote ${path.relative(rootDir, reportPath)}`);

if (status !== "ok" && failOnError) {
  process.exit(1);
}
