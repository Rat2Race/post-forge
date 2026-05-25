#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
USER_BIN_DIR="${K6_BIN_DIR:-$HOME/.local/bin}"
status=0

source "$ROOT_DIR/setup/scripts/test-env.sh"
load_tests_env "$ROOT_DIR"
source "$ROOT_DIR/setup/scripts/verify-helpers.sh"

if command -v k6 >/dev/null 2>&1; then
  printf '[ok] k6 found: %s\n' "$(k6 version | head -1)"
elif [[ -x "$USER_BIN_DIR/k6" ]]; then
  printf '[ok] k6 found: %s\n' "$("$USER_BIN_DIR/k6" version | head -1)"
  printf '[warn] k6 is installed outside PATH. Run: source ./setup/env.sh, then rerun verify.\n'
  status=1
else
  printf '[warn] k6 not found. Run: ./setup/tools/k6/install.sh\n'
  status=1
fi

require_file "$ROOT_DIR/tests/k6/generated/smoke.js" \
  "k6 generated smoke script found" \
  "k6 generated smoke script missing. Run: ./setup/tools/k6/install.sh"
require_dir "$ROOT_DIR/tests/k6/manual" \
  "k6 manual script area found" \
  "k6 manual script area missing. Run: ./setup/tools/k6/install.sh"
require_file "$ROOT_DIR/tests/.env" \
  "shared tests env found: tests/.env" \
  "shared tests env missing: tests/.env"
require_executable "$ROOT_DIR/setup/tools/k6/run.sh" \
  "k6 run wrapper found" \
  "k6 run wrapper missing or not executable: setup/tools/k6/run.sh"
require_executable "$ROOT_DIR/setup/run-k6" \
  "setup k6 shortcut found" \
  "setup k6 shortcut missing or not executable: setup/run-k6"

exit "$status"
