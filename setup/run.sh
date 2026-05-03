#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MANIFEST="$ROOT_DIR/setup/manifest.yml"
STATE_DIR="$ROOT_DIR/setup/state"
REPORT_DIR="$ROOT_DIR/setup/reports"
POLICY_FILE="$ROOT_DIR/tests/testing-policy.yml"

# Tool installers prefer user-local binaries when system paths are not writable.
export PATH="${HOME}/.local/bin:${PATH}"

ACTION="${1:-plan}"

usage() {
  cat <<'EOF'
Usage:
  ./setup/run.sh assess
  ./setup/run.sh plan
  ./setup/run.sh install
  ./setup/run.sh verify
  ./setup/run.sh analyze-project
  ./setup/run.sh sync-policy
  ./setup/run.sh generate-tests
  ./setup/run.sh run-smoke

Wrappers:
  ./setup/install.sh
  ./setup/verify.sh
EOF
}

timestamp() {
  date -u +"%Y-%m-%dT%H:%M:%SZ"
}

die() {
  printf '[fail] %s\n' "$1" >&2
  exit 1
}

pass() {
  printf '[ok] %s\n' "$1"
}

warn() {
  printf '[warn] %s\n' "$1"
}

manifest_tools() {
  awk '
    /^tools:/ { in_tools = 1; next }
    /^[^[:space:]][^:]*:/ && in_tools { exit }
    in_tools && /^  [A-Za-z0-9_-]+:/ {
      tool = $1
      sub(":", "", tool)
      current = tool
      order[++count] = tool
      enabled[tool] = "true"
      next
    }
    in_tools && current != "" && /^    enabled:/ {
      enabled[current] = $2
      next
    }
    END {
      for (i = 1; i <= count; i++) {
        tool = order[i]
        if (enabled[tool] != "false") {
          print tool
        }
      }
    }
  ' "$MANIFEST"
}

detect_stage() {
  local os_name="$1"
  case "$os_name" in
    Linux) printf 'linux' ;;
    Darwin) printf 'macos' ;;
    *) printf 'unknown' ;;
  esac
}

json_array() {
  local first=1
  printf '['
  for item in "$@"; do
    if [[ "$first" -eq 0 ]]; then
      printf ','
    fi
    first=0
    printf '"%s"' "$item"
  done
  printf ']'
}

write_state() {
  local action="$1"
  local status="$2"
  shift 2

  mkdir -p "$STATE_DIR"
  local path="$STATE_DIR/${action}-result.json"

  {
    printf '{\n'
    printf '  "schemaVersion": 1,\n'
    printf '  "action": "%s",\n' "$action"
    printf '  "status": "%s",\n' "$status"
    printf '  "timestamp": "%s",\n' "$(timestamp)"
    printf '  "manifest": "setup/manifest.yml",\n'
    printf '  "items": '
    json_array "$@"
    printf '\n}\n'
  } > "$path"

  printf '[state] wrote %s\n' "${path#$ROOT_DIR/}"
}

json_string() {
  local value="$1"
  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  value="${value//$'\t'/\\t}"
  value="${value//$'\r'/\\r}"
  value="${value//$'\n'/\\n}"
  printf '%s' "$value"
}

command_state() {
  local cmd="$1"
  if command -v "$cmd" >/dev/null 2>&1; then
    printf 'present'
  else
    printf 'missing'
  fi
}

command_version() {
  local cmd="$1"
  shift
  if command -v "$cmd" >/dev/null 2>&1; then
    "$cmd" "$@" 2>/dev/null | head -1
  fi
}

relative_path() {
  local path="$1"
  printf '%s' "${path#$ROOT_DIR/}"
}

find_openapi_files() {
  find "$ROOT_DIR" \
    \( -type d \( -name .git -o -name .gradle -o -name .omx -o -name build -o -name out -o -name node_modules \) -prune \) -o \
    -type f \( \
      -iname 'openapi.yml' -o \
      -iname 'openapi.yaml' -o \
      -iname 'openapi.json' -o \
      -iname 'swagger.yml' -o \
      -iname 'swagger.yaml' -o \
      -iname 'swagger.json' \
    \) -print | sort
}

route_pattern_for_file() {
  local file="$1"

  case "$file" in
    *.java|*.kt|*.groovy)
      printf '%s' '@(RestController|Controller)([[:space:]]|$)|@(RequestMapping|GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping)'
      ;;
    *.js|*.jsx|*.ts|*.tsx)
      printf '%s' '\.(get|post|put|patch|delete|route)[[:space:]]*\('
      ;;
    *)
      printf ''
      ;;
  esac
}

find_route_files() {
  local pattern

  while IFS= read -r file; do
    pattern="$(route_pattern_for_file "$file")"
    [[ -z "$pattern" ]] && continue

    if grep -Eq "$pattern" "$file" 2>/dev/null; then
      printf '%s\n' "$file"
    fi
  done < <(
    find "$ROOT_DIR" \
      \( -type d \( -name .git -o -name .gradle -o -name .omx -o -name build -o -name out -o -name node_modules \) -prune \) -o \
      -type f \( \
        -name '*.java' -o \
        -name '*.kt' -o \
        -name '*.groovy' -o \
        -name '*.js' -o \
        -name '*.jsx' -o \
        -name '*.ts' -o \
        -name '*.tsx' \
      \) \
      ! -path "$ROOT_DIR/setup/*" \
      ! -path "$ROOT_DIR/tests/*" \
      ! -path "$ROOT_DIR/*/src/test/*" \
      -print
  ) | sort
}

detect_method_from_line() {
  local line="$1"

  if [[ "$line" == *"@GetMapping"* ]] || [[ "$line" == *".get("* ]]; then
    printf 'GET'
  elif [[ "$line" == *"@PostMapping"* ]] || [[ "$line" == *".post("* ]]; then
    printf 'POST'
  elif [[ "$line" == *"@PutMapping"* ]] || [[ "$line" == *".put("* ]]; then
    printf 'PUT'
  elif [[ "$line" == *"@PatchMapping"* ]] || [[ "$line" == *".patch("* ]]; then
    printf 'PATCH'
  elif [[ "$line" == *"@DeleteMapping"* ]] || [[ "$line" == *".delete("* ]]; then
    printf 'DELETE'
  elif [[ "$line" =~ RequestMethod\.([A-Z]+) ]]; then
    printf '%s' "${BASH_REMATCH[1]}"
  else
    printf 'ANY'
  fi
}

extract_path_from_line() {
  local line="$1"
  local path

  path="$(printf '%s\n' "$line" | sed -nE "s/.*\([^)]*['\"]([^'\"]+)['\"].*/\1/p" | head -1)"
  if [[ -z "$path" ]]; then
    path="$(printf '%s\n' "$line" | sed -nE "s/.*['\"](\/[^'\"]*)['\"].*/\1/p" | head -1)"
  fi

  printf '%s' "$path"
}

write_project_inventory() {
  mkdir -p "$STATE_DIR"
  local path="$STATE_DIR/project-inventory.json"

  {
    printf '{\n'
    printf '  "schemaVersion": 1,\n'
    printf '  "action": "analyze-project",\n'
    printf '  "status": "ok",\n'
    printf '  "timestamp": "%s",\n' "$(timestamp)"
    printf '  "policy": "%s",\n' "$(relative_path "$POLICY_FILE")"
    printf '  "sourcePriority": ["openapi","controllers","dto-schema","existing-tests","live-server"],\n'
    printf '  "openapiFiles": [\n'

    local first=1
    local file
    while IFS= read -r file; do
      if [[ "$first" -eq 0 ]]; then
        printf ',\n'
      fi
      first=0
      printf '    { "type": "openapi", "path": "%s" }' "$(json_string "$(relative_path "$file")")"
    done < <(find_openapi_files)

    printf '\n'
    printf '  ],\n'
    printf '  "routeCandidates": [\n'

    first=1
    while IFS= read -r file; do
      local pattern
      pattern="$(route_pattern_for_file "$file")"
      [[ -z "$pattern" ]] && continue

      local line
      while IFS=: read -r line_number line; do
        [[ -z "$line_number" || -z "$line" ]] && continue

        local method
        local route_path
        method="$(detect_method_from_line "$line")"
        route_path="$(extract_path_from_line "$line")"

        if [[ "$first" -eq 0 ]]; then
          printf ',\n'
        fi
        first=0
        printf '    { "type": "route-candidate", "file": "%s", "line": %s, "method": "%s", "pathHint": "%s", "raw": "%s" }' \
          "$(json_string "$(relative_path "$file")")" \
          "$line_number" \
          "$(json_string "$method")" \
          "$(json_string "$route_path")" \
          "$(json_string "$line")"
      done < <(grep -nE "$pattern" "$file" 2>/dev/null || true)
    done < <(find_route_files)

    printf '\n'
    printf '  ]\n'
    printf '}\n'
  } > "$path"

  printf '[state] wrote %s\n' "$(relative_path "$path")"
}

write_project_report() {
  mkdir -p "$REPORT_DIR"
  local inventory="$STATE_DIR/project-inventory.json"
  local report="$REPORT_DIR/testing-setup-report.md"
  local openapi_count
  local route_count

  openapi_count="$(grep -c '"type": "openapi"' "$inventory" 2>/dev/null || true)"
  route_count="$(grep -c '"type": "route-candidate"' "$inventory" 2>/dev/null || true)"
  openapi_count="${openapi_count:-0}"
  route_count="${route_count:-0}"

  {
    printf '# Testing Setup Report\n\n'
    printf '%s\n' "- Generated at: \`$(timestamp)\`"
    printf '%s\n' "- Inventory: \`$(relative_path "$inventory")\`"
    printf '%s\n' "- Policy: \`$(relative_path "$POLICY_FILE")\`"
    printf '%s\n' "- OpenAPI files found: \`$openapi_count\`"
    printf '%s\n\n' "- Route candidates found: \`$route_count\`"
    printf '## Next Agent Step\n\n'
    printf 'Use the inventory and existing policy to update `tests/testing-policy.yml`. Preserve existing endpoint classes, add new endpoints, mark missing endpoints as `stale: true`, and require review on conflicts.\n'
  } > "$report"

  printf '[report] wrote %s\n' "$(relative_path "$report")"
}

write_assess_state() {
  local os_name="$1"
  local uname_value="$2"
  local stage="$3"
  local node_state="$4"
  local npm_state="$5"
  local bru_state="$6"
  local k6_state="$7"
  local apt_state="$8"
  local brew_state="$9"

  mkdir -p "$STATE_DIR"
  local path="$STATE_DIR/assess-result.json"

  {
    printf '{\n'
    printf '  "schemaVersion": 1,\n'
    printf '  "action": "assess",\n'
    printf '  "status": "ok",\n'
    printf '  "timestamp": "%s",\n' "$(timestamp)"
    printf '  "manifest": "setup/manifest.yml",\n'
    printf '  "os": {\n'
    printf '    "name": "%s",\n' "$(json_string "$os_name")"
    printf '    "uname": "%s"\n' "$(json_string "$uname_value")"
    printf '  },\n'
    printf '  "stage": "%s",\n' "$(json_string "$stage")"
    printf '  "commands": {\n'
    printf '    "node": "%s",\n' "$node_state"
    printf '    "npm": "%s",\n' "$npm_state"
    printf '    "bru": "%s",\n' "$bru_state"
    printf '    "k6": "%s",\n' "$k6_state"
    printf '    "aptGet": "%s",\n' "$apt_state"
    printf '    "brew": "%s"\n' "$brew_state"
    printf '  }\n'
    printf '}\n'
  } > "$path"

  printf '[state] wrote %s\n' "${path#$ROOT_DIR/}"
}

ensure_manifest() {
  [[ -f "$MANIFEST" ]] || die "setup/manifest.yml not found"
}

assess() {
  ensure_manifest

  local os_name
  local uname_value
  local stage
  os_name="$(uname -s 2>/dev/null || printf unknown)"
  uname_value="$(uname -a 2>/dev/null || printf unknown)"
  stage="$(detect_stage "$os_name")"

  local node_state
  local npm_state
  local bru_state
  local k6_state
  local apt_state
  local brew_state
  node_state="$(command_state node)"
  npm_state="$(command_state npm)"
  bru_state="$(command_state bru)"
  k6_state="$(command_state k6)"
  apt_state="$(command_state apt-get)"
  brew_state="$(command_state brew)"

  printf '[info] environment assessment\n'
  printf '  os: %s\n' "$os_name"
  printf '  stage: %s\n' "$stage"
  printf '  node: %s\n' "$node_state"
  printf '  npm: %s\n' "$npm_state"
  printf '  bruno CLI: %s\n' "$bru_state"
  printf '  k6: %s\n' "$k6_state"
  printf '  apt-get: %s\n' "$apt_state"
  printf '  brew: %s\n' "$brew_state"

  write_assess_state \
    "$os_name" \
    "$uname_value" \
    "$stage" \
    "$node_state" \
    "$npm_state" \
    "$bru_state" \
    "$k6_state" \
    "$apt_state" \
    "$brew_state"
}

plan() {
  ensure_manifest
  local os_name
  local stage
  os_name="$(uname -s 2>/dev/null || printf unknown)"
  stage="$(detect_stage "$os_name")"

  printf '[info] manifest: %s\n' "${MANIFEST#$ROOT_DIR/}"
  printf '[info] stage: %s\n' "$stage"
  printf '[info] tools:\n'
  local tools=()
  while IFS= read -r tool; do
    [[ -z "$tool" ]] && continue
    tools+=("$tool")
    printf '  - %s\n' "$tool"
  done < <(manifest_tools)

  write_state "plan" "ok" "$stage" "${tools[@]}"
}

install() {
  ensure_manifest
  local tools=()
  while IFS= read -r tool; do
    [[ -z "$tool" ]] && continue
    tools+=("$tool")
  done < <(manifest_tools)

  [[ "${#tools[@]}" -gt 0 ]] || die "no enabled tools declared in setup/manifest.yml"

  printf '[info] installing enabled tools in parallel: %s\n' "${tools[*]}"

  local pids=()
  local names=()

  for tool in "${tools[@]}"; do
    local script="$ROOT_DIR/setup/tools/$tool/install.sh"
    [[ -x "$script" ]] || die "missing executable install script: ${script#$ROOT_DIR/}"

    (
      printf '[%s] install start\n' "$tool"
      "$script"
      printf '[%s] install complete\n' "$tool"
    ) &
    pids+=("$!")
    names+=("$tool")
  done

  local failed=0
  for i in "${!pids[@]}"; do
    if ! wait "${pids[$i]}"; then
      printf '[fail] %s install failed\n' "${names[$i]}" >&2
      failed=1
    fi
  done

  if [[ "$failed" -ne 0 ]]; then
    write_state "install" "failed" "${tools[@]}"
    exit 1
  fi

  write_state "install" "ok" "${tools[@]}"
  pass "setup install complete"
}

verify() {
  ensure_manifest

  command -v bash >/dev/null 2>&1 && pass "bash found" || die "bash not found"

  local tools=()
  local status=0
  while IFS= read -r tool; do
    [[ -z "$tool" ]] && continue
    tools+=("$tool")
  done < <(manifest_tools)

  for tool in "${tools[@]}"; do
    local script="$ROOT_DIR/setup/tools/$tool/verify.sh"
    [[ -x "$script" ]] || die "missing executable verify script: ${script#$ROOT_DIR/}"
    if ! "$script"; then
      status=1
    fi
  done

  if [[ "$status" -ne 0 ]]; then
    write_state "verify" "failed" "${tools[@]}"
    die "setup verification failed"
  fi

  write_state "verify" "ok" "${tools[@]}"
  pass "setup verification complete"
}

analyze_project() {
  ensure_manifest

  if [[ -f "$POLICY_FILE" ]]; then
    pass "testing policy found: $(relative_path "$POLICY_FILE")"
  else
    warn "testing policy missing: $(relative_path "$POLICY_FILE")"
  fi

  write_project_inventory
  write_project_report
  pass "project inventory complete"
}

sync_policy() {
  ensure_manifest

  local inventory="$STATE_DIR/project-inventory.json"
  local result="$STATE_DIR/policy-sync-result.json"
  local report="$REPORT_DIR/testing-setup-report.md"

  if [[ ! -f "$inventory" ]]; then
    warn "project inventory missing. Running analyze-project first."
    analyze_project
  fi

  command -v node >/dev/null 2>&1 || die "node is required for sync-policy"

  node "$ROOT_DIR/setup/scripts/sync-policy.mjs" \
    "$ROOT_DIR" \
    "$inventory" \
    "$POLICY_FILE" \
    "$result" \
    "$report"

  pass "testing policy sync complete"
}

generate_tests() {
  ensure_manifest

  local result="$STATE_DIR/generated-tests-result.json"
  local report="$REPORT_DIR/testing-setup-report.md"

  if [[ ! -f "$POLICY_FILE" ]] || ! grep -q '^  - method:' "$POLICY_FILE" 2>/dev/null; then
    warn "testing policy has no endpoints. Running analyze-project and sync-policy first."
    analyze_project
    sync_policy
  fi

  command -v node >/dev/null 2>&1 || die "node is required for generate-tests"

  node "$ROOT_DIR/setup/scripts/generate-tests.mjs" \
    "$ROOT_DIR" \
    "$POLICY_FILE" \
    "$result" \
    "$report"

  pass "generated tests complete"
}

run_smoke() {
  ensure_manifest

  local result="$STATE_DIR/smoke-run-result.json"
  local report="$REPORT_DIR/testing-run-report.md"

  command -v node >/dev/null 2>&1 || die "node is required for run-smoke"

  node "$ROOT_DIR/setup/scripts/run-smoke.mjs" \
    "$ROOT_DIR" \
    "$result" \
    "$report"

  pass "smoke run complete"
}

case "$ACTION" in
  assess) assess ;;
  plan) plan ;;
  install) install ;;
  verify) verify ;;
  analyze-project) analyze_project ;;
  sync-policy) sync_policy ;;
  generate-tests) generate_tests ;;
  run-smoke) run_smoke ;;
  -h|--help|help) usage ;;
  *) usage; die "unknown action: $ACTION" ;;
esac
