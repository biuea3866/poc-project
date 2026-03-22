#!/bin/bash
# Start all Closet services with Pinpoint APM agent attached
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PINPOINT_AGENT_PATH="${PROJECT_ROOT}/docker/pinpoint/pinpoint-agent-2.5.1/pinpoint-bootstrap-2.5.1.jar"
PINPOINT_CONFIG="${PROJECT_ROOT}/docker/pinpoint/pinpoint-agent-2.5.1/pinpoint-root.config"
LOG_DIR="/tmp/closet-logs"

mkdir -p "${LOG_DIR}"

# Check Pinpoint agent exists
if [ ! -f "${PINPOINT_AGENT_PATH}" ]; then
  echo "Pinpoint agent not found. Downloading..."
  bash "${PROJECT_ROOT}/docker/pinpoint/download-agent.sh"
fi

if [ ! -f "${PINPOINT_AGENT_PATH}" ]; then
  echo "ERROR: Pinpoint agent not found at ${PINPOINT_AGENT_PATH}"
  echo "Run: bash docker/pinpoint/download-agent.sh"
  exit 1
fi

declare -A services=(
  ["member"]="8081"
  ["product"]="8082"
  ["order"]="8083"
  ["payment"]="8084"
  ["bff"]="8085"
  ["gateway"]="8080"
)

echo "Starting Closet services with Pinpoint APM..."
echo ""

for svc in "${!services[@]}"; do
  port="${services[$svc]}"
  jar_path="${PROJECT_ROOT}/closet-${svc}/build/libs/closet-${svc}-"*.jar

  # Check jar exists
  if ! ls ${jar_path} 1>/dev/null 2>&1; then
    echo "  [SKIP] closet-${svc} - JAR not found. Run './gradlew :closet-${svc}:bootJar' first."
    continue
  fi

  echo "  Starting closet-${svc} on :${port} with Pinpoint agent..."

  java -javaagent:"${PINPOINT_AGENT_PATH}" \
    -Dpinpoint.agentId="closet-${svc}" \
    -Dpinpoint.agentName="closet-${svc}" \
    -Dpinpoint.applicationName="closet-${svc}" \
    -Dpinpoint.config="${PINPOINT_CONFIG}" \
    -jar ${jar_path} \
    --server.port="${port}" \
    > "${LOG_DIR}/${svc}.log" 2>&1 &

  echo "    PID: $!"
done

echo ""
echo "Pinpoint Web UI: http://localhost:28080"
echo "Log directory: ${LOG_DIR}"
echo ""
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
