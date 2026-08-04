#!/usr/bin/env bash
# Boots a webapp, pulls its generated OpenAPI spec, writes it to ./openapi, shuts the app down.
# Usage: generate.sh <webapp-name> <port>
set -euo pipefail

name="$1"
port="$2"
task=":libs:${name}:bootRun"
out_dir="openapi"
log="$(mktemp)"

mkdir -p "$out_dir"

./gradlew "$task" --args="--server.port=${port}" >"$log" 2>&1 &
pid=$!

cleanup() {
  kill "$pid" >/dev/null 2>&1 || true
  pkill -f "$task" >/dev/null 2>&1 || true
}
trap cleanup EXIT

ready=false
for _ in $(seq 1 60); do
  if curl -sf "http://localhost:${port}/v3/api-docs" -o /dev/null; then
    ready=true
    break
  fi
  sleep 1
done

if [ "$ready" != true ]; then
  echo "error: ${name} did not become ready on port ${port} within 60s" >&2
  cat "$log" >&2
  exit 1
fi

curl -sf "http://localhost:${port}/v3/api-docs" -o "${out_dir}/${name}.openapi.json"
curl -sf "http://localhost:${port}/v3/api-docs.yaml" -o "${out_dir}/${name}.openapi.yaml"

echo "wrote ${out_dir}/${name}.openapi.json"
echo "wrote ${out_dir}/${name}.openapi.yaml"
