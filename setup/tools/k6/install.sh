#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
USER_BIN_DIR="${K6_BIN_DIR:-$HOME/.local/bin}"

source "$ROOT_DIR/setup/scripts/test-env.sh"
load_tests_env "$ROOT_DIR"

prepend_path_once() {
  local bin_dir="$1"

  [[ -n "$bin_dir" ]] || return 0
  case ":$PATH:" in
    *":$bin_dir:"*) ;;
    *) export PATH="${bin_dir}:${PATH}" ;;
  esac
}

write_if_missing() {
  local path="$1"
  local content="$2"

  if [[ -f "$path" ]]; then
    printf '[k6] skip %s\n' "${path#$ROOT_DIR/}"
    return
  fi

  mkdir -p "$(dirname "$path")"
  printf '%s\n' "$content" > "$path"
  printf '[k6] create %s\n' "${path#$ROOT_DIR/}"
}

write_if_missing "$ROOT_DIR/tests/k6/AGENTS.md" '# AGENTS.md - k6 Performance Tests

Rules for files under tests/k6/**.

- Keep smoke performance scripts light.
- Do not run destructive or high-load tests against production without explicit approval.
- Use tests/k6/env.js for default target settings, and BASE_URL for one-off overrides.
- Separate smoke, baseline, and stress scripts.
- Generated scripts belong under generated.
- Manual scripts belong under manual and must not be overwritten automatically.
'

write_if_missing "$ROOT_DIR/tests/k6/manual/.gitkeep" ''

write_if_missing "$ROOT_DIR/tests/k6/env.js" 'const defaults = {
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
export const APP_COMMIT = envValue("APP_COMMIT", defaults.appCommit);'

write_if_missing "$ROOT_DIR/tests/k6/report.js" 'import {
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
      if (result?.ok === false) failed.push(`${metricName} ${threshold}`);
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

export function createPerformanceSummary(data) {
  const now = new Date();
  const baseName = reportBaseName(now);
  const mdPath = `${REPORT_DIR}/${baseName}.md`;
  const jsonPath = `${SUMMARY_DIR}/${baseName}-summary.json`;
  const failed = thresholdStatus(data);
  const checks = metricValues(data, "checks");
  const duration = metricValues(data, "http_req_duration");
  const reqs = metricValues(data, "http_reqs").count || 0;
  const seconds = (data.state?.testRunDurationMs || 0) / 1000;
  const rps = seconds > 0 ? reqs / seconds : 0;

  const markdown = `# ${TARGET_NAME} ${SCENARIO_NAME} 성능 테스트 리포트

## 요약

| 항목 | 값 |
|---|---|
| 결론 | ${failed.length === 0 ? "pass" : "fail"} |
| 실행 일시 | \`${now.toISOString()}\` |
| 테스트 ID | \`${baseName}\` |
| 대상 환경 | \`${TARGET_NAME}\` |
| 앱 image tag | \`${APP_IMAGE_TAG || "-"}\` |
| 앱 commit | \`${APP_COMMIT || "-"}\` |

## 부하 조건

| 항목 | 값 |
|---|---:|
| VUs | ${VUS_COUNT} |
| iterations | ${ITERATIONS} |
| duration | ${seconds.toFixed(2)} s |

## k6 결과

| 지표 | 값 |
|---|---:|
| checks | ${checks.passes ?? 0}/${(checks.passes ?? 0) + (checks.fails ?? 0)} |
| http_reqs | ${reqs} |
| requests/sec | ${rps.toFixed(2)} |
| http_req_failed | ${formatRate(metricValues(data, "http_req_failed").rate ?? 0)} |
| http_req_duration avg | ${formatMs(duration.avg)} |
| http_req_duration med | ${formatMs(duration.med)} |
| http_req_duration p95 | ${formatMs(duration["p(95)"])} |
| http_req_duration p99 | ${formatMs(duration["p(99)"])} |
| http_req_duration max | ${formatMs(duration.max)} |

## Artifact

| 종류 | 경로 |
|---|---|
| k6 summary JSON | \`${jsonPath}\` |
| k6 markdown report | \`${mdPath}\` |

## 결론

${failed.length === 0 ? "Threshold 기준으로 통과했다." : `Threshold 실패: ${failed.join(", ")}`}
`;

  return {
    stdout: `\n[k6-report] markdown: ${mdPath}\n[k6-report] summary: ${jsonPath}\n\n`,
    [mdPath]: markdown,
    [jsonPath]: JSON.stringify(data, null, 2),
  };
}'

write_if_missing "$ROOT_DIR/tests/k6/generated/smoke.js" 'import { BASE_URL, SMOKE_PATH, VUS_COUNT, ITERATIONS } from "../env.js";
import { createPerformanceSummary } from "../report.js";
import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: VUS_COUNT,
  iterations: ITERATIONS,
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<1000"],
    "http_req_duration{name:GET smoke path}": ["p(95)<1000"],
  },
};

if (!BASE_URL) {
  throw new Error("BASE_URL is empty. Set tests/k6/env.js or run with BASE_URL=http://localhost:8080 k6 run tests/k6/generated/smoke.js");
}

export default function () {
  const res = http.get(`${BASE_URL}${SMOKE_PATH}`, {
    tags: { name: "GET smoke path" },
  });

  check(res, {
    "status is 2xx or 3xx": (r) => r.status >= 200 && r.status < 400,
  });

  sleep(1);
}

export function handleSummary(data) {
  return createPerformanceSummary(data, {
    purpose: "generated smoke",
    script: "tests/k6/generated/smoke.js",
  });
}'

k6_bin() {
  if command -v k6 >/dev/null 2>&1; then
    command -v k6
  elif [[ -x "$USER_BIN_DIR/k6" ]]; then
    printf '%s\n' "$USER_BIN_DIR/k6"
  fi
}

install_user_local_k6() {
  command -v curl >/dev/null 2>&1 || {
    printf '[k6] curl not found. Install curl or run with sudo-enabled apt setup.\n' >&2
    exit 1
  }
  command -v tar >/dev/null 2>&1 || {
    printf '[k6] tar not found. Install tar or run with sudo-enabled apt setup.\n' >&2
    exit 1
  }

  local machine
  local arch
  local version
  local tag
  local archive
  local url
  local tmp_dir

  machine="$(uname -m)"
  case "$machine" in
    x86_64|amd64) arch="amd64" ;;
    aarch64|arm64) arch="arm64" ;;
    *)
      printf '[k6] unsupported Linux architecture for user-local install: %s\n' "$machine" >&2
      exit 1
      ;;
  esac

  version="${K6_VERSION:-}"
  if [[ -z "$version" ]]; then
    version="$(curl -fsSL https://api.github.com/repos/grafana/k6/releases/latest | sed -nE 's/.*"tag_name"[[:space:]]*:[[:space:]]*"v?([^"]+)".*/\1/p' | head -1)"
  fi

  [[ -n "$version" ]] || {
    printf '[k6] could not resolve latest k6 version. Set K6_VERSION and rerun.\n' >&2
    exit 1
  }

  tag="v${version#v}"
  archive="k6-${tag}-linux-${arch}.tar.gz"
  url="https://github.com/grafana/k6/releases/download/${tag}/${archive}"
  tmp_dir="$(mktemp -d)"

  printf '[k6] installing user-local binary: %s\n' "$tag"
  curl -fsSL "$url" -o "$tmp_dir/$archive"
  tar -xzf "$tmp_dir/$archive" -C "$tmp_dir"
  mkdir -p "$USER_BIN_DIR"
  cp "$tmp_dir/k6-${tag}-linux-${arch}/k6" "$USER_BIN_DIR/k6"
  chmod +x "$USER_BIN_DIR/k6"
  rm -rf "$tmp_dir"

  prepend_path_once "$USER_BIN_DIR"
  if ! command -v k6 >/dev/null 2>&1; then
    printf '[k6] installed CLI at %s/k6\n' "$USER_BIN_DIR"
    printf '[k6] run through ./setup/run-k6 or source ./setup/env.sh before direct k6 commands.\n'
  fi
}

install_system_k6() {
  local os="$1"

  if [[ "$os" == "Darwin" ]]; then
    if ! command -v brew >/dev/null 2>&1; then
      printf '[k6] Homebrew not found. Install Homebrew or set K6_BIN to an existing k6 binary.\n' >&2
      exit 1
    fi
    printf '[k6] installing with Homebrew because K6_INSTALL_MODE=system\n'
    brew install k6
  elif [[ "$os" == "Linux" ]]; then
    if ! command -v apt-get >/dev/null 2>&1; then
      printf '[k6] apt-get not found. Install k6 manually or keep SETUP_INSTALL_MODE=user on supported Linux architectures.\n' >&2
      exit 1
    fi
    if ! command -v sudo >/dev/null 2>&1; then
      printf '[k6] sudo not found. System install requires sudo; use SETUP_INSTALL_MODE=user instead.\n' >&2
      exit 1
    fi
    if ! sudo -n true 2>/dev/null; then
      printf '[k6] sudo requires an interactive password. System install was requested but cannot run non-interactively.\n' >&2
      exit 1
    fi
    if ! command -v gpg >/dev/null 2>&1 || ! command -v curl >/dev/null 2>&1; then
      printf '[k6] installing prerequisites with apt because K6_INSTALL_MODE=system\n'
      sudo apt-get update
      sudo apt-get install -y ca-certificates curl gpg
    fi
    printf '[k6] installing with apt repository because K6_INSTALL_MODE=system\n'
    curl -fsSL https://dl.k6.io/key.gpg | sudo gpg --dearmor --yes -o /usr/share/keyrings/k6-archive-keyring.gpg
    echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list >/dev/null
    sudo apt-get update
    sudo apt-get install -y k6
  else
    printf '[k6] unsupported OS: %s\n' "$os" >&2
    exit 1
  fi
}

if [[ -n "$(k6_bin)" ]]; then
  cli="$(k6_bin)"
  if [[ "$cli" == "$USER_BIN_DIR/k6" ]]; then
    prepend_path_once "$USER_BIN_DIR"
  fi
  printf '[k6] found: %s\n' "$("$cli" version | head -1)"
else
  os="$(uname -s)"
  install_mode="${K6_INSTALL_MODE:-${SETUP_INSTALL_MODE:-user}}"

  case "$install_mode" in
    user)
      if [[ "$os" != "Linux" ]]; then
        printf '[k6] user-local auto-install currently supports Linux only. Set K6_BIN to an existing binary or run K6_INSTALL_MODE=system for Homebrew on macOS.\n' >&2
        exit 1
      fi
      install_user_local_k6
      ;;
    system)
      install_system_k6 "$os"
      ;;
    *)
      printf '[k6] invalid K6_INSTALL_MODE/SETUP_INSTALL_MODE: %s (expected user or system)\n' "$install_mode" >&2
      exit 1
      ;;
  esac
fi

printf '[k6] setup complete\n'
