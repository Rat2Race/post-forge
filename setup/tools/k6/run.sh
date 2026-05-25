#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"

export PATH="${HOME}/.local/bin:${PATH}"

source "$ROOT_DIR/setup/scripts/test-env.sh"
load_tests_env "$ROOT_DIR"

usage() {
  cat <<'EOF'
Usage:
  ./setup/run-k6 [smoke|manual|<script>] [options] [-- <k6 run options>]
  ./setup/tools/k6/run.sh [smoke|manual|<script>] [options] [-- <k6 run options>]
  ./setup/run.sh run-k6 [smoke|manual|<script>] [options] [-- <k6 run options>]

Examples:
  ./setup/run-k6
  BASE_URL=http://127.0.0.1:8080 ./setup/run-k6 manual
  ./setup/run-k6 manual/example.js
  ./setup/run-k6 manual/performance.js -- --quiet

Script shortcuts:
  smoke                 tests/k6/generated/smoke.js
  manual, performance   tests/k6/manual/performance.js
  manual/<name>         tests/k6/manual/<name>.js
  <path>                Relative path under tests/k6 or repo root

Options:
  --base-url <url>      Set BASE_URL. Default: http://localhost:8080
  --target <name>       Set K6_TARGET_NAME. Default: local
  --scenario <name>     Set K6_SCENARIO_NAME. Default: inferred from script
  --name <name>         Set K6_REPORT_NAME. Default: <run-id>-<scenario>
  --run-id <id>         Set run id. Default: current local timestamp
  --script-root <path>  Set k6 script root. Default: tests/k6
  --report-dir <path>   Set K6_REPORT_DIR. Default: <K6_REPORT_ROOT>/<run-id>
  --summary-dir <path>  Set K6_SUMMARY_DIR. Default: report dir
  --no-log              Do not tee k6 output to <report-dir>/<name>.log
  --print-command       Print the resolved command without running k6
  -h, --help            Show this help

Existing k6 script environment variables still pass through unchanged. Put native
k6 options after --.
EOF
}

die() {
  printf '[fail] %s\n' "$1" >&2
  exit 1
}

warn() {
  printf '[warn] %s\n' "$1" >&2
}

option_value() {
  local option="$1"
  local value="${2-}"

  [[ "$#" -ge 2 ]] || die "$option requires a value"
  printf '%s' "$value"
}

slug() {
  local value="$1"
  value="$(printf '%s' "$value" | tr '[:upper:]' '[:lower:]')"
  value="$(printf '%s' "$value" | sed -E 's/[^a-z0-9]+/-/g; s/^-+//; s/-+$//')"
  printf '%s' "${value:-unknown}"
}

abs_path() {
  local path="$1"
  if [[ "$path" == /* ]]; then
    printf '%s' "$path"
  else
    printf '%s/%s' "$ROOT_DIR" "$path"
  fi
}

resolve_script() {
  local ref="$1"
  local no_ext="${ref%.js}"
  local candidates=()

  case "$ref" in
    smoke|generated|generated/smoke)
      candidates+=("$SCRIPT_ROOT_ABS/generated/smoke.js")
      ;;
    manual|performance)
      candidates+=("$SCRIPT_ROOT_ABS/manual/performance.js")
      ;;
    /*)
      candidates+=("$ref")
      ;;
    *)
      candidates+=(
        "$ROOT_DIR/$ref"
        "$ROOT_DIR/$no_ext.js"
        "$SCRIPT_ROOT_ABS/$ref"
        "$SCRIPT_ROOT_ABS/$no_ext.js"
        "$SCRIPT_ROOT_ABS/manual/$ref"
        "$SCRIPT_ROOT_ABS/manual/$no_ext.js"
        "$SCRIPT_ROOT_ABS/generated/$ref"
        "$SCRIPT_ROOT_ABS/generated/$no_ext.js"
      )
      ;;
  esac

  local candidate
  for candidate in "${candidates[@]}"; do
    if [[ -f "$candidate" ]]; then
      printf '%s' "$candidate"
      return
    fi
  done

  die "k6 script not found for '${ref}'. Use smoke, manual, manual/<name>, or a tests/k6 path."
}

infer_scenario() {
  local ref="$1"
  local script_rel="$2"

  case "$ref" in
    smoke|generated|generated/smoke)
      printf 'smoke'
      return
      ;;
    manual|performance)
      printf 'manual-public'
      return
      ;;
  esac

  local without_ext="${script_rel%.js}"
  without_ext="${without_ext#$SCRIPT_ROOT_REL/}"
  without_ext="${without_ext#manual/}"
  without_ext="${without_ext#generated/}"
  slug "$without_ext"
}

k6_bin() {
  if [[ -n "${K6_BIN:-}" ]]; then
    if command -v "$K6_BIN" >/dev/null 2>&1; then
      command -v "$K6_BIN"
    else
      printf '%s' "$K6_BIN"
    fi
  elif command -v k6 >/dev/null 2>&1; then
    command -v k6
  elif [[ -x "$HOME/.local/bin/k6" ]]; then
    printf '%s' "$HOME/.local/bin/k6"
  fi
}

print_run_config() {
  printf '[k6] script: %s\n' "$script_rel"
  printf '[k6] script root: %s\n' "$SCRIPT_ROOT_REL"
  printf '[k6] base url: %s\n' "$base_url"
  printf '[k6] target: %s\n' "$target_name"
  printf '[k6] scenario: %s\n' "$scenario_name"
  printf '[k6] report dir: %s\n' "$report_dir"
  printf '[k6] summary dir: %s\n' "$summary_dir"
  printf '[k6] report name: %s\n' "$report_name"
  if [[ "$tee_log" == "true" ]]; then
    printf '[k6] log: %s\n' "${log_path_abs#$ROOT_DIR/}"
  fi
}

export_k6_env() {
  export BASE_URL="$base_url"
  export K6_TARGET_NAME="$target_name"
  export K6_SCENARIO_NAME="$scenario_name"
  export K6_REPORT_NAME="$report_name"
  export K6_REPORT_DIR="$report_dir"
  export K6_SUMMARY_DIR="$summary_dir"
}

print_resolved_command() {
  printf '[k6] command:'
  printf ' %q' \
    "BASE_URL=$BASE_URL" \
    "K6_TARGET_NAME=$K6_TARGET_NAME" \
    "K6_SCENARIO_NAME=$K6_SCENARIO_NAME" \
    "K6_REPORT_NAME=$K6_REPORT_NAME" \
    "K6_REPORT_DIR=$K6_REPORT_DIR" \
    "K6_SUMMARY_DIR=$K6_SUMMARY_DIR" \
    "${cmd[@]}"
  printf '\n'
}

run_k6() {
  cd "$ROOT_DIR"

  set +e
  if [[ "$tee_log" == "true" ]]; then
    "${cmd[@]}" 2>&1 | tee "$log_path_abs"
    status="${PIPESTATUS[0]}"
  else
    "${cmd[@]}"
    status="$?"
  fi
  set -e
}

script_ref="${K6_SCRIPT:-smoke}"
script_root="${K6_SCRIPT_ROOT:-tests/k6}"
base_url="${BASE_URL:-http://localhost:8080}"
target_name="${K6_TARGET_NAME:-local}"
scenario_name="${K6_SCENARIO_NAME:-}"
run_id="${K6_RUN_ID:-$(date +%Y%m%d-%H%M%S)}"
report_dir="${K6_REPORT_DIR:-}"
summary_dir="${K6_SUMMARY_DIR:-}"
report_root="${K6_REPORT_ROOT:-setup/reports/k6}"
report_name="${K6_REPORT_NAME:-}"
tee_log="${K6_TEE_LOG:-true}"
print_command=false
k6_args=()
script_ref_seen=false

while [[ "$#" -gt 0 ]]; do
  case "$1" in
    -h|--help)
      usage
      exit 0
      ;;
    --base-url)
      base_url="$(option_value "$@")"
      shift 2
      ;;
    --target)
      target_name="$(option_value "$@")"
      shift 2
      ;;
    --scenario)
      scenario_name="$(option_value "$@")"
      shift 2
      ;;
    --name|--report-name)
      report_name="$(option_value "$@")"
      shift 2
      ;;
    --run-id)
      run_id="$(option_value "$@")"
      shift 2
      ;;
    --script-root)
      script_root="$(option_value "$@")"
      shift 2
      ;;
    --report-dir)
      report_dir="$(option_value "$@")"
      shift 2
      ;;
    --summary-dir)
      summary_dir="$(option_value "$@")"
      shift 2
      ;;
    --no-log)
      tee_log=false
      shift
      ;;
    --print-command|--dry-run)
      print_command=true
      shift
      ;;
    --)
      shift
      k6_args+=("$@")
      break
      ;;
    -*)
      die "unknown wrapper option: $1. Put native k6 options after --."
      ;;
    *)
      if [[ "$script_ref_seen" == "true" ]]; then
        die "unexpected argument: $1. Put native k6 options after --."
      fi
      script_ref="$1"
      script_ref_seen=true
      shift
      ;;
  esac
done

SCRIPT_ROOT_ABS="$(abs_path "$script_root")"
SCRIPT_ROOT_REL="${SCRIPT_ROOT_ABS#$ROOT_DIR/}"

script_path="$(resolve_script "$script_ref")"
script_rel="${script_path#$ROOT_DIR/}"

if [[ -z "$scenario_name" ]]; then
  scenario_name="$(infer_scenario "$script_ref" "$script_rel")"
fi

scenario_slug="$(slug "$scenario_name")"
if [[ -z "$report_name" ]]; then
  report_name="${run_id}-${scenario_slug}"
fi
report_name="$(slug "$report_name")"

if [[ -z "$report_dir" ]]; then
  report_dir="$report_root/$run_id"
fi
if [[ -z "$summary_dir" ]]; then
  summary_dir="$report_dir"
fi

report_dir_abs="$(abs_path "$report_dir")"
summary_dir_abs="$(abs_path "$summary_dir")"
mkdir -p "$report_dir_abs" "$summary_dir_abs"

log_path_abs="$report_dir_abs/$report_name.log"
summary_export_abs="$summary_dir_abs/$report_name-summary-export.json"

bin="$(k6_bin)"
[[ -n "$bin" ]] || die "k6 CLI not found. Run ./setup/tools/k6/install.sh or install k6 first."
if [[ "$bin" == */* ]]; then
  [[ -x "$bin" ]] || die "k6 is not executable: $bin"
elif ! command -v "$bin" >/dev/null 2>&1; then
  die "k6 command not found: $bin"
fi

if ! grep -q 'handleSummary' "$script_path"; then
  warn "$script_rel has no handleSummary(); markdown report generation depends on the script. A raw summary export and log will still be written."
fi

cmd=("$bin" run --summary-export "$summary_export_abs" "${k6_args[@]}" "$script_rel")

print_run_config
export_k6_env

if [[ "$print_command" == "true" ]]; then
  print_resolved_command
  exit 0
fi

run_k6

if [[ "$status" -eq 0 ]]; then
  printf '[k6] complete. Artifacts are under %s\n' "$report_dir"
else
  printf '[k6] failed with exit code %s. Artifacts are under %s\n' "$status" "$report_dir" >&2
fi

exit "$status"
