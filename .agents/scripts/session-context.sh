#!/usr/bin/env bash
# Prints the just commands so a coding agent sees them at session start.
set -euo pipefail

echo '## Available just commands'
just --list --list-submodules 2>&1
