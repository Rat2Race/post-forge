#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
COLLECTION_DIR="$ROOT_DIR/tests/bruno/api"
USER_NPM_PREFIX="${BRUNO_NPM_PREFIX:-$HOME/.local}"
status=0

source "$ROOT_DIR/setup/scripts/test-env.sh"
load_tests_env "$ROOT_DIR"
source "$ROOT_DIR/setup/scripts/verify-helpers.sh"

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

require_dir "$COLLECTION_DIR" \
  "Bruno collection found: ${COLLECTION_DIR#$ROOT_DIR/}" \
  "Bruno collection missing. Run: ./setup/tools/bruno/install.sh"
require_file "$COLLECTION_DIR/bruno.json" \
  "Bruno native collection config found" \
  "Bruno native collection config missing: bruno.json"
require_file "$COLLECTION_DIR/collection.bru" \
  "Bruno native collection root found" \
  "Bruno native collection root missing: collection.bru"
reject_file "$COLLECTION_DIR/opencollection.yml" \
  "Legacy opencollection.yml is present. Bruno CLI will prefer YAML mode over native .bru requests."
require_file "$COLLECTION_DIR/environments/local.example.bru" \
  "Bruno example environment found" \
  "Bruno example environment missing"
require_dir "$COLLECTION_DIR/generated/smoke" \
  "Bruno generated smoke area found" \
  "Bruno generated smoke area missing. Run: ./setup/tools/bruno/install.sh"
require_file "$COLLECTION_DIR/generated/smoke/folder.bru" \
  "Bruno generated smoke folder metadata found" \
  "Bruno generated smoke folder metadata missing: folder.bru"
require_dir "$COLLECTION_DIR/generated/scenario" \
  "Bruno generated scenario area found" \
  "Bruno generated scenario area missing. Run: ./setup/tools/bruno/install.sh"
require_dir "$COLLECTION_DIR/manual" \
  "Bruno manual collection area found" \
  "Bruno manual collection missing. Run: ./setup/tools/bruno/install.sh"

exit "$status"
