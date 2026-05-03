#!/usr/bin/env bash
set -euo pipefail

# Source this file before manual tool runs if user-local tools are not on PATH.
case ":$PATH:" in
  *":${HOME}/.local/bin:"*) ;;
  *) export PATH="${HOME}/.local/bin:${PATH}" ;;
esac
