#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
USER_BIN_DIR="${K6_BIN_DIR:-$HOME/.local/bin}"
status=0

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

if [[ -f "$ROOT_DIR/tests/k6/generated/smoke.js" ]]; then
  printf '[ok] k6 generated smoke script found\n'
else
  printf '[warn] k6 generated smoke script missing. Run: ./setup/tools/k6/install.sh\n'
  status=1
fi

if [[ -d "$ROOT_DIR/tests/k6/manual" ]]; then
  printf '[ok] k6 manual script area found\n'
else
  printf '[warn] k6 manual script area missing. Run: ./setup/tools/k6/install.sh\n'
  status=1
fi

exit "$status"
