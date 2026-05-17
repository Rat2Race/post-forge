import {
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
  return Number.isFinite(value) ? `${value.toFixed(2)} ms` : "-";
}

function formatRate(value) {
  return Number.isFinite(value) ? `${(value * 100).toFixed(2)}%` : "-";
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

function reportBaseName(now) {
  if (REPORT_NAME) return slug(REPORT_NAME);
  const date = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
  const time = `${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`;
  return `${date}-${time}-${slug(TARGET_NAME)}-${slug(SCENARIO_NAME)}`;
}

function renderMarkdown(data, paths, now, metadata) {
  const failed = thresholdStatus(data);
  const checks = metricValues(data, "checks");
  const duration = data.metrics?.http_req_duration?.values || {};
  const reqs = data.metrics?.http_reqs?.values?.count || 0;
  const seconds = (data.state?.testRunDurationMs || 0) / 1000;
  const rps = seconds > 0 ? reqs / seconds : 0;

  return `# ${TARGET_NAME} ${SCENARIO_NAME} k6 report

## Summary

| Item | Value |
|---|---|
| Result | ${failed.length === 0 ? "pass" : "fail"} |
| Run time | \`${now.toISOString()}\` |
| Test ID | \`${paths.baseName}\` |
| Purpose | ${metadata.purpose || "-"} |
| Script | \`${metadata.script || "-"}\` |
| Target | \`${TARGET_NAME}\` |
| App image tag | \`${APP_IMAGE_TAG || "-"}\` |
| App commit | \`${APP_COMMIT || "-"}\` |

## Load

| Item | Value |
|---|---:|
| VUs | ${VUS_COUNT} |
| Iterations | ${ITERATIONS} |
| Duration | ${seconds.toFixed(2)} s |

## k6 Result

| Metric | Value |
|---|---:|
| checks | ${checks.passes ?? 0}/${(checks.passes ?? 0) + (checks.fails ?? 0)} |
| http_reqs | ${reqs} |
| requests/sec | ${rps.toFixed(2)} |
| http_req_failed | ${formatRate(data.metrics?.http_req_failed?.values?.rate ?? 0)} |
| http_req_duration avg | ${formatMs(duration.avg)} |
| http_req_duration med | ${formatMs(duration.med)} |
| http_req_duration p95 | ${formatMs(duration["p(95)"])} |
| http_req_duration p99 | ${formatMs(duration["p(99)"])} |
| http_req_duration max | ${formatMs(duration.max)} |

## Artifact

| Type | Path |
|---|---|
| k6 summary JSON | \`${paths.jsonPath}\` |
| k6 markdown report | \`${paths.mdPath}\` |

## Conclusion

${failed.length === 0 ? "Threshold checks passed." : `Threshold failures: ${failed.join(", ")}`}
`;
}

export function createPerformanceSummary(data, metadata = {}) {
  const now = new Date();
  const baseName = reportBaseName(now);
  const paths = {
    baseName,
    mdPath: `${REPORT_DIR}/${baseName}.md`,
    jsonPath: `${SUMMARY_DIR}/${baseName}-summary.json`,
  };

  return {
    stdout: `\n[k6-report] markdown: ${paths.mdPath}\n[k6-report] summary: ${paths.jsonPath}\n\n`,
    [paths.mdPath]: renderMarkdown(data, paths, now, metadata),
    [paths.jsonPath]: JSON.stringify(data, null, 2),
  };
}
