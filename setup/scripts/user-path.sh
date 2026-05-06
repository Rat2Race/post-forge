#!/usr/bin/env bash

ensure_user_path() {
  local bin_dir="$1"
  local label="${2:-testing-tools}"
  local marker_begin="# >>> ${label} PATH >>>"
  local marker_end="# <<< ${label} PATH <<<"
  local block
  local lock_dir
  local lock_parent
  local target_files=()
  local file
  local tmp_file
  local attempts=0

  [[ -n "$bin_dir" ]] || return 0
  mkdir -p "$bin_dir"

  case ":$PATH:" in
    *":$bin_dir:"*) ;;
    *) export PATH="${bin_dir}:${PATH}" ;;
  esac

  block="$(cat <<EOF
${marker_begin}
if [ -d "${bin_dir}" ]; then
  case ":\$PATH:" in
    *":${bin_dir}:"*) ;;
    *) export PATH="${bin_dir}:\$PATH" ;;
  esac
fi
${marker_end}
EOF
)"

  lock_parent="$HOME/.cache/testing-tools-setup"
  lock_dir="$lock_parent/path.lock"
  mkdir -p "$lock_parent"

  until mkdir "$lock_dir" 2>/dev/null; do
    attempts=$((attempts + 1))
    if [[ "$attempts" -gt 100 ]]; then
      printf '[path] could not acquire lock: %s\n' "$lock_dir" >&2
      return 1
    fi
    sleep 0.1
  done

  target_files+=("$HOME/.profile")

  case "${SHELL:-}" in
    */bash) target_files+=("$HOME/.bashrc") ;;
    */zsh) target_files+=("$HOME/.zshenv" "$HOME/.zprofile" "$HOME/.zshrc") ;;
  esac

  [[ -f "$HOME/.bashrc" ]] && target_files+=("$HOME/.bashrc")
  [[ -f "$HOME/.zshenv" ]] && target_files+=("$HOME/.zshenv")
  [[ -f "$HOME/.zprofile" ]] && target_files+=("$HOME/.zprofile")
  [[ -f "$HOME/.zshrc" ]] && target_files+=("$HOME/.zshrc")

  for file in "${target_files[@]}"; do
    [[ -n "$file" ]] || continue

    mkdir -p "$(dirname "$file")"
    touch "$file"
    tmp_file="$(mktemp)"

    awk -v begin="$marker_begin" -v end="$marker_end" '
      index($0, begin) { depth++; next }
      index($0, end) { if (depth > 0) depth--; next }
      depth == 0 { print }
    ' "$file" > "$tmp_file"

    {
      printf '%s\n\n' "$block"
      cat "$tmp_file"
    } > "$file"
    rm -f "$tmp_file"
    printf '[path] ensured %s in %s\n' "$bin_dir" "$file"
  done

  rmdir "$lock_dir" 2>/dev/null || true
}
