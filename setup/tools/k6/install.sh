#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
USER_BIN_DIR="${K6_BIN_DIR:-$HOME/.local/bin}"

source "$ROOT_DIR/setup/scripts/user-path.sh"

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
- Use BASE_URL for target selection.
- Separate smoke, baseline, and stress scripts.
- Generated scripts belong under generated.
- Manual scripts belong under manual and must not be overwritten automatically.
'

write_if_missing "$ROOT_DIR/tests/k6/manual/.gitkeep" ''

write_if_missing "$ROOT_DIR/tests/k6/generated/smoke.js" 'import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<1000"],
  },
};

const BASE_URL = __ENV.BASE_URL;
const SMOKE_PATH = __ENV.SMOKE_PATH || "/";

if (!BASE_URL) {
  throw new Error("BASE_URL is required. Example: BASE_URL=http://localhost:8080 k6 run tests/k6/generated/smoke.js");
}

export default function () {
  const res = http.get(`${BASE_URL}${SMOKE_PATH}`);

  check(res, {
    "status is 2xx or 3xx": (r) => r.status >= 200 && r.status < 400,
  });

  sleep(1);
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

  ensure_user_path "$USER_BIN_DIR" "testing-tools-setup"
  if ! command -v k6 >/dev/null 2>&1; then
    printf '[k6] installed CLI at %s/k6\n' "$USER_BIN_DIR"
    printf '[k6] open a new shell or run: source ./setup/env.sh\n'
  fi
}

if [[ -n "$(k6_bin)" ]]; then
  cli="$(k6_bin)"
  if [[ "$cli" == "$USER_BIN_DIR/k6" ]]; then
    ensure_user_path "$USER_BIN_DIR" "testing-tools-setup"
  fi
  printf '[k6] found: %s\n' "$("$cli" version | head -1)"
else
  os="$(uname -s)"
  if [[ "$os" == "Darwin" ]]; then
    if ! command -v brew >/dev/null 2>&1; then
      printf '[k6] Homebrew not found. Install Homebrew or install k6 manually: https://grafana.com/docs/k6/latest/set-up/install-k6/\n' >&2
      exit 1
    fi
    printf '[k6] installing with Homebrew\n'
    brew install k6
  elif [[ "$os" == "Linux" ]]; then
    if ! command -v apt-get >/dev/null 2>&1; then
      printf '[k6] apt-get not found. Install k6 manually: https://grafana.com/docs/k6/latest/set-up/install-k6/\n' >&2
      exit 1
    fi
    if ! command -v sudo >/dev/null 2>&1; then
      printf '[k6] sudo not found. Falling back to user-local binary install.\n'
      install_user_local_k6
      printf '[k6] setup complete\n'
      exit 0
    fi
    if ! sudo -n true 2>/dev/null; then
      printf '[k6] sudo requires an interactive password. Falling back to user-local binary install.\n'
      install_user_local_k6
      printf '[k6] setup complete\n'
      exit 0
    fi
    if ! command -v gpg >/dev/null 2>&1 || ! command -v curl >/dev/null 2>&1; then
      printf '[k6] installing prerequisites with apt\n'
      sudo apt-get update
      sudo apt-get install -y ca-certificates curl gpg
    fi
    printf '[k6] installing with apt repository\n'
    curl -fsSL https://dl.k6.io/key.gpg | sudo gpg --dearmor --yes -o /usr/share/keyrings/k6-archive-keyring.gpg
    echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list >/dev/null
    sudo apt-get update
    sudo apt-get install -y k6
  else
    printf '[k6] unsupported OS: %s\n' "$os" >&2
    exit 1
  fi
fi

printf '[k6] setup complete\n'
