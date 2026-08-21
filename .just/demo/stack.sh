#!/usr/bin/env bash
# Boots sample-rest-api-client on port 18080, then runs toolpool-demo in the foreground.
# toolpool.base-url in apps/toolpool-demo/src/main/resources/application.yaml points at that port.
set -euo pipefail

api_port=18080
api_task=":libs:sample-rest-api-client:bootRun"
log="$(mktemp)"

./gradlew "$api_task" --args="--server.port=${api_port}" >"$log" 2>&1 &
api_pid=$!

cleanup() {
  kill "$api_pid" >/dev/null 2>&1 || true
  pkill -f "$api_task" >/dev/null 2>&1 || true
}
trap cleanup EXIT

ready=false
for _ in $(seq 1 60); do
  if curl -sf "http://localhost:${api_port}/v3/api-docs" -o /dev/null; then
    ready=true
    break
  fi
  sleep 1
done

if [ "$ready" != true ]; then
  echo "error: sample-rest-api-client did not become ready on port ${api_port}" >&2
  cat "$log" >&2
  exit 1
fi

echo "sample-rest-api-client is ready on http://localhost:${api_port}"
moon run toolpool-demo:run
