#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
COLLECTION_DIR="$ROOT_DIR/tests/bruno/api"
USER_NPM_PREFIX="${BRUNO_NPM_PREFIX:-$HOME/.local}"

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
    printf '[bruno] skip %s\n' "${path#$ROOT_DIR/}"
    return
  fi

  mkdir -p "$(dirname "$path")"
  printf '%s\n' "$content" > "$path"
  printf '[bruno] create %s\n' "${path#$ROOT_DIR/}"
}

remove_if_default() {
  local path="$1"
  local expected="$2"

  if [[ ! -f "$path" ]]; then
    return
  fi

  if [[ "$(tr -d '\r' < "$path" | sed -e '${/^$/d;}')" == "$(printf '%s\n' "$expected" | tr -d '\r' | sed -e '${/^$/d;}')" ]]; then
    rm -f "$path"
    printf '[bruno] remove legacy %s\n' "${path#$ROOT_DIR/}"
  fi
}

bruno_bin() {
  local cli_path

  if command -v bru >/dev/null 2>&1; then
    cli_path="$(command -v bru)"
    printf '%s\n' "$cli_path"
  elif [[ -x "$USER_NPM_PREFIX/bin/bru" ]]; then
    prepend_path_once "$USER_NPM_PREFIX/bin"
    printf '%s\n' "$USER_NPM_PREFIX/bin/bru"
  fi
}

write_if_missing "$ROOT_DIR/tests/bruno/AGENTS.md" '# AGENTS.md - Bruno API Collection

Rules for files under tests/bruno/**.

- Do not commit secrets, JWTs, OAuth secrets, API keys, or local passwords.
- Treat environments/local.bru as local-only.
- Keep environments/local.example.bru on the safe default http://localhost:8080.
- Use smoke only for fast, stable requests.
- Use draft for unstable requests or data-heavy prerequisites.
- Generated requests belong under api/generated.
- Manual requests belong under api/manual and must not be overwritten automatically.
'

write_if_missing "$COLLECTION_DIR/.gitignore" 'environments/local.bru
.env
node_modules/'

remove_if_default "$COLLECTION_DIR/opencollection.yml" 'info:
  name: API Test Collection
  type: collection
  version: 1'

write_if_missing "$COLLECTION_DIR/bruno.json" '{
  "version": "1",
  "name": "API Test Collection",
  "type": "collection",
  "ignore": [
    "node_modules",
    ".git"
  ]
}'

write_if_missing "$COLLECTION_DIR/collection.bru" 'meta {
  type: collection
}

auth {
  mode: none
}'

write_if_missing "$COLLECTION_DIR/environments/local.example.bru" 'vars {
  baseUrl: http://localhost:8080
  accessToken:
  refreshToken:
}'

write_if_missing "$COLLECTION_DIR/environments/local.bru" 'vars {
  baseUrl: http://localhost:8080
  accessToken:
  refreshToken:
}'

remove_if_default "$COLLECTION_DIR/generated/folder.yml" 'info:
  name: generated
  type: folder
  seq: 1'

write_if_missing "$COLLECTION_DIR/generated/folder.bru" 'meta {
  name: generated
  type: folder
  seq: 1
}'

remove_if_default "$COLLECTION_DIR/generated/smoke/folder.yml" 'info:
  name: smoke
  type: folder
  seq: 1'

write_if_missing "$COLLECTION_DIR/generated/smoke/folder.bru" 'meta {
  name: smoke
  type: folder
  seq: 1
}'

remove_if_default "$COLLECTION_DIR/generated/draft/folder.yml" 'info:
  name: draft
  type: folder
  seq: 2'

write_if_missing "$COLLECTION_DIR/generated/draft/folder.bru" 'meta {
  name: draft
  type: folder
  seq: 2
}'

remove_if_default "$COLLECTION_DIR/generated/scenario/folder.yml" 'info:
  name: scenario
  type: folder
  seq: 3'

write_if_missing "$COLLECTION_DIR/generated/scenario/folder.bru" 'meta {
  name: scenario
  type: folder
  seq: 3
}'

remove_if_default "$COLLECTION_DIR/manual/folder.yml" 'info:
  name: manual
  type: folder
  seq: 2'

write_if_missing "$COLLECTION_DIR/manual/folder.bru" 'meta {
  name: manual
  type: folder
  seq: 2
}'

write_if_missing "$COLLECTION_DIR/manual/exploratory/.gitkeep" ''

if [[ -n "$(bruno_bin)" ]]; then
  cli="$(bruno_bin)"
  printf '[bruno] CLI found: %s\n' "$("$cli" --version 2>/dev/null || true)"
else
  if ! command -v npm >/dev/null 2>&1; then
    printf '[bruno] npm not found. Install Node.js LTS first, then run: npm install -g @usebruno/cli\n' >&2
    exit 1
  fi

  version="${BRUNO_CLI_VERSION:-latest}"
  package="@usebruno/cli@$version"
  install_mode="${BRUNO_INSTALL_MODE:-${SETUP_INSTALL_MODE:-user}}"

  case "$install_mode" in
    user)
      mkdir -p "$USER_NPM_PREFIX"
      printf '[bruno] installing CLI with user npm prefix: %s\n' "$USER_NPM_PREFIX"
      npm install -g --prefix "$USER_NPM_PREFIX" "$package"
      prepend_path_once "$USER_NPM_PREFIX/bin"
      if ! command -v bru >/dev/null 2>&1; then
        printf '[bruno] installed CLI at %s/bin/bru\n' "$USER_NPM_PREFIX"
        printf '[bruno] run through ./setup/run.sh or source ./setup/env.sh before direct bru commands.\n'
      fi
      ;;
    system)
      printf '[bruno] installing CLI with npm global prefix because BRUNO_INSTALL_MODE=system: %s\n' "$package"
      npm install -g "$package"
      ;;
    *)
      printf '[bruno] invalid BRUNO_INSTALL_MODE/SETUP_INSTALL_MODE: %s (expected user or system)\n' "$install_mode" >&2
      exit 1
      ;;
  esac
fi

printf '[bruno] setup complete\n'
