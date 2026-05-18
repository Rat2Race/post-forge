const defaults = {
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
