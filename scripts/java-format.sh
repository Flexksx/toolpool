#!/usr/bin/env bash
# Formats one project's src tree in place.
# Usage: java-format.sh [project-dir]
set -euo pipefail

project_dir="${1:-.}"

cd "$project_dir"

if [ ! -d src ]; then
  echo "error: no src directory in ${project_dir}" >&2
  exit 1
fi

find src -name '*.java' -print0 | xargs -0 -r google-java-format --replace
