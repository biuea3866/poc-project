#!/bin/bash
# Start all Closet services (without Pinpoint)
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOG_DIR="/tmp/closet-logs"

mkdir -p "${LOG_DIR}"

declare -A services=(
  ["gateway"]="8080"
  ["member"]="8081"
  ["product"]="8082"
  ["order"]="8083"
  ["payment"]="8084"
  ["bff"]="8085"
)

echo "Starting all Closet services..."
echo ""

for svc in "${!services[@]}"; do
  port="${services[$svc]}"
  jar_path="${PROJECT_ROOT}/closet-${svc}/build/libs/closet-${svc}-"*.jar

  if ! ls ${jar_path} 1>/dev/null 2>&1; then
    echo "  [SKIP] closet-${svc} - JAR not found. Run './gradlew :closet-${svc}:bootJar' first."
    continue
  fi

  echo "  Starting closet-${svc} on :${port}..."
  java -jar ${jar_path} --server.port="${port}" > "${LOG_DIR}/${svc}.log" 2>&1 &
  echo "    PID: $!"
done

echo ""
echo "Log directory: ${LOG_DIR}"
echo "Waiting 30s for services to start..."
sleep 30

echo ""
echo "Health check:"
for port in 8080 8081 8082 8083 8084 8085; do
  if lsof -i :"$port" -sTCP:LISTEN > /dev/null 2>&1; then
    echo "  :${port} UP"
  else
    echo "  :${port} DOWN"
  fi
done
