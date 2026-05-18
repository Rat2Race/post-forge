import http from "k6/http";
import { check, fail, group, sleep } from "k6";
import { Counter } from "k6/metrics";

const BASE_URL = envValue("BASE_URL", "");
const COLLECTOR_SOURCE = envValue("COLLECTOR_SOURCE", "naver-news");
const INTERNAL_API_KEY = envValue(
  "COLLECTOR_INTERNAL_API_KEY",
  envValue("INTERNAL_API_KEY", ""),
);
const COLLECTOR_VUS = envNumber("COLLECTOR_VUS", 1);
const COLLECTOR_ITERATIONS = envNumber("COLLECTOR_ITERATIONS", 1);
const COLLECTOR_MAX_DURATION = envValue("COLLECTOR_MAX_DURATION", "2m");
const COLLECTOR_SLEEP_SECONDS = envNumber("COLLECTOR_SLEEP_SECONDS", 0.5);
const COLLECTOR_P95_MS = envNumber("COLLECTOR_P95_MS", 15000);
const REPORT_DIR = envValue("K6_REPORT_DIR", "docs/performance");
const SUMMARY_DIR = envValue("K6_SUMMARY_DIR", "docs/performance/k6");
const REPORT_NAME = envValue("K6_REPORT_NAME", "");
const TARGET_NAME = envValue("K6_TARGET_NAME", "local");
const SCENARIO_NAME = envValue("K6_SCENARIO_NAME", "collector-trigger");

const targetTransactions = new Counter("collector_target_transactions");
const successfulTransactions = new Counter("collector_successful_transactions");

export const options = {
  scenarios: {
    trigger_collector: {
      executor: "shared-iterations",
      vus: COLLECTOR_VUS,
      iterations: COLLECTOR_ITERATIONS,
      maxDuration: COLLECTOR_MAX_DURATION,
      gracefulStop: "10s",
    },
  },
  thresholds: {
    checks: ["rate>0.99"],
    http_req_failed: ["rate<0.01"],
    [`http_req_duration{name:POST /collector/{source}}`]: [`p(95)<${COLLECTOR_P95_MS}`],
    "collector_target_transactions": ["count>=1"],
    "collector_successful_transactions": ["count>=1"],
  },
  summaryTrendStats: ["avg", "med", "p(90)", "p(95)", "p(99)", "max"],
};

export default function () {
  if (!BASE_URL) {
    fail("BASE_URL is empty. Set BASE_URL=http://localhost:8080.");
  }

  if (!INTERNAL_API_KEY) {
    fail("Set COLLECTOR_INTERNAL_API_KEY or INTERNAL_API_KEY for X-Internal-Api-Key.");
  }

  group("collector trigger", () => {
    const res = http.post(
      `${BASE_URL}/collector/${encodeURIComponent(COLLECTOR_SOURCE)}`,
      null,
      {
        headers: {
          Accept: "application/json",
          "X-Internal-Api-Key": INTERNAL_API_KEY,
        },
        tags: {
          name: "POST /collector/{source}",
          source: COLLECTOR_SOURCE,
        },
      },
    );
    const body = parseJson(res);
    const ok = res.status === 200 && typeof body?.message === "string";

    targetTransactions.add(1);
    if (ok) {
      successfulTransactions.add(1);
    }

    check(res, {
      "collector trigger returns 200": () => res.status === 200,
      "collector trigger returns message": () => typeof body?.message === "string",
    });
  });

  sleep(COLLECTOR_SLEEP_SECONDS);
}

export function handleSummary(data) {
  const redacted = redactSecrets(data);
  const baseName = reportBaseName();
  const paths = {
    mdPath: `${REPORT_DIR}/${baseName}.md`,
    jsonPath: `${SUMMARY_DIR}/${baseName}-summary.json`,
  };

  return {
    stdout: `\n[collector-k6] markdown: ${paths.mdPath}\n[collector-k6] summary: ${paths.jsonPath}\n\n`,
    [paths.mdPath]: renderMarkdown(redacted, paths),
    [paths.jsonPath]: JSON.stringify(redacted, null, 2),
  };
}

function envValue(name, fallback) {
  const value = __ENV[name];
  return value === undefined || value === "" ? fallback : value;
}

function envNumber(name, fallback) {
  const value = Number(envValue(name, fallback));
  return Number.isFinite(value) ? value : fallback;
}

function parseJson(res) {
  try {
    return res.json();
  } catch {
    return null;
  }
}

function redactSecrets(value) {
  if (Array.isArray(value)) {
    return value.map(redactSecrets);
  }

  if (value && typeof value === "object") {
    for (const [key, nestedValue] of Object.entries(value)) {
      if (/(authorization|api[_-]?key|token|cookie|password|secret)/i.test(key)) {
        value[key] = "[redacted]";
      } else {
        value[key] = redactSecrets(nestedValue);
      }
    }
  }

  return value;
}

function renderMarkdown(data, paths) {
  const duration = data.metrics?.["http_req_duration{name:POST /collector/{source}}"]?.values || {};
  const targetCount = data.metrics?.collector_target_transactions?.values?.count || 0;
  const successCount = data.metrics?.collector_successful_transactions?.values?.count || 0;
  const checks = data.metrics?.checks?.values || {};
  const failed = thresholdStatus(data);
  const seconds = (data.state?.testRunDurationMs || 0) / 1000;
  const tps = seconds > 0 ? successCount / seconds : 0;

  return `# ${TARGET_NAME} ${SCENARIO_NAME} collector k6 report

## Run

| Item | Value |
|---|---:|
| Result | ${failed.length === 0 ? "pass" : "fail"} |
| Target | \`${BASE_URL}\` |
| Source | \`${COLLECTOR_SOURCE}\` |
| VUs | ${COLLECTOR_VUS} |
| Iterations | ${COLLECTOR_ITERATIONS} |
| Duration | ${seconds.toFixed(2)} s |

## Result

| Requests | Success Tx | TPS | avg | med | p95 | p99 | max | Checks |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| ${targetCount} | ${successCount} | ${formatNumber(tps)} | ${formatMs(duration.avg)} | ${formatMs(duration.med)} | ${formatMs(duration["p(95)"])} | ${formatMs(duration["p(99)"])} | ${formatMs(duration.max)} | ${checks.passes ?? 0}/${(checks.passes ?? 0) + (checks.fails ?? 0)} |

## Artifact

| Type | Path |
|---|---|
| k6 summary JSON | \`${paths.jsonPath}\` |
| k6 markdown report | \`${paths.mdPath}\` |

## Notes

- This script triggers \`POST /collector/{source}\` and may persist newly collected data.
- Threshold failures: ${failed.length === 0 ? "none" : failed.join(", ")}
`;
}

function thresholdStatus(data) {
  const failed = [];
  for (const [metricName, metric] of Object.entries(data.metrics || {})) {
    for (const [threshold, result] of Object.entries(metric.thresholds || {})) {
      if (result?.ok === false) {
        failed.push(`${metricName} ${threshold}`);
      }
    }
  }
  return failed;
}

function reportBaseName() {
  if (REPORT_NAME) return slug(REPORT_NAME);
  const now = new Date();
  return `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}-${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}-${slug(SCENARIO_NAME)}`;
}

function pad(value) {
  return String(value).padStart(2, "0");
}

function slug(value) {
  return String(value || "unknown")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "") || "unknown";
}

function formatNumber(value) {
  return Number.isFinite(value) ? value.toFixed(2) : "-";
}

function formatMs(value) {
  return Number.isFinite(value) ? `${value.toFixed(2)} ms` : "-";
}
