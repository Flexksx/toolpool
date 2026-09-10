#!/usr/bin/env bash
# Checks Java formatting and style for one project's src tree.
# Usage: java-lint.sh <checkstyle-config> [project-dir]
set -euo pipefail

if [ "$#" -lt 1 ]; then
  echo "usage: java-lint.sh <checkstyle-config> [project-dir]" >&2
  exit 2
fi

checkstyle_config="$1"
project_dir="${2:-.}"

cd "$project_dir"

if [ ! -d src ]; then
  echo "error: no src directory in ${project_dir}" >&2
  exit 1
fi

find src -name '*.java' -print0 | xargs -0 -r google-java-format --dry-run --set-exit-if-changed
find src -name '*.java' -print0 | xargs -0 -r checkstyle -c "$checkstyle_config"
