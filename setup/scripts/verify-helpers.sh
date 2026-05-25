#!/usr/bin/env bash

# Small helpers for setup tool verify scripts. Callers own the global `status`
# variable so every failed check can be reported before exiting once.

verify_warn() {
  printf '[warn] %s\n' "$1"
  status=1
}

require_file() {
  local path="$1"
  local ok_message="$2"
  local warn_message="$3"

  if [[ -f "$path" ]]; then
    printf '[ok] %s\n' "$ok_message"
  else
    verify_warn "$warn_message"
  fi
}

require_dir() {
  local path="$1"
  local ok_message="$2"
  local warn_message="$3"

  if [[ -d "$path" ]]; then
    printf '[ok] %s\n' "$ok_message"
  else
    verify_warn "$warn_message"
  fi
}

require_executable() {
  local path="$1"
  local ok_message="$2"
  local warn_message="$3"

  if [[ -x "$path" ]]; then
    printf '[ok] %s\n' "$ok_message"
  else
    verify_warn "$warn_message"
  fi
}

reject_file() {
  local path="$1"
  local warn_message="$2"

  if [[ -f "$path" ]]; then
    verify_warn "$warn_message"
  fi
}
