#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
COLLECTION_DIR="$ROOT_DIR/tests/bruno/api"
USER_NPM_PREFIX="${BRUNO_NPM_PREFIX:-$HOME/.local}"
status=0

if command -v bru >/dev/null 2>&1; then
  printf '[ok] bruno CLI found: %s\n' "$(bru --version 2>/dev/null || true)"
elif [[ -x "$USER_NPM_PREFIX/bin/bru" ]]; then
  printf '[ok] bruno CLI found: %s\n' "$("$USER_NPM_PREFIX/bin/bru" --version 2>/dev/null || true)"
  printf '[warn] bruno CLI is installed outside PATH. Run: source ./setup/env.sh, then rerun verify.\n'
  status=1
else
  printf '[warn] bruno CLI not found. Run: ./setup/tools/bruno/install.sh\n'
  status=1
fi

if [[ -d "$COLLECTION_DIR" ]]; then
  printf '[ok] Bruno collection found: %s\n' "${COLLECTION_DIR#$ROOT_DIR/}"
else
  printf '[warn] Bruno collection missing. Run: ./setup/tools/bruno/install.sh\n'
  status=1
fi

if [[ -f "$COLLECTION_DIR/environments/local.example.bru" ]]; then
  printf '[ok] Bruno example environment found\n'
else
  printf '[warn] Bruno example environment missing\n'
  status=1
fi

if [[ -d "$COLLECTION_DIR/generated/smoke" ]]; then
  printf '[ok] Bruno generated smoke area found\n'
else
  printf '[warn] Bruno generated smoke area missing. Run: ./setup/tools/bruno/install.sh\n'
  status=1
fi

if [[ -d "$COLLECTION_DIR/generated/scenario" ]]; then
  printf '[ok] Bruno generated scenario area found\n'
else
  printf '[warn] Bruno generated scenario area missing. Run: ./setup/tools/bruno/install.sh\n'
  status=1
fi

if [[ -d "$COLLECTION_DIR/manual" ]]; then
  printf '[ok] Bruno manual collection area found\n'
else
  printf '[warn] Bruno manual collection area missing. Run: ./setup/tools/bruno/install.sh\n'
  status=1
fi

exit "$status"
