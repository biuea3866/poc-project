#!/bin/bash
# Start all Closet services with OpenTelemetry Java Agent for Jaeger tracing
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OTEL_AGENT="${PROJECT_ROOT}/docker/otel/opentelemetry-javaagent.jar"
LOG_DIR="/tmp/closet-logs"

mkdir -p "${LOG_DIR}"

# Download OpenTelemetry Java Agent if not present
if [ ! -f "${OTEL_AGENT}" ]; then
  echo "OpenTelemetry Java Agent not found. Downloading..."
  mkdir -p "${PROJECT_ROOT}/docker/otel"
  curl -L -o "${OTEL_AGENT}" \
    "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.1.0/opentelemetry-javaagent.jar"
  echo "Downloaded: ${OTEL_AGENT}"
fi

if [ ! -f "${OTEL_AGENT}" ]; then
  echo "ERROR: Failed to download OpenTelemetry Java Agent"
  exit 1
fi

declare -A services=(
  ["gateway"]="8080"
  ["member"]="8081"
  ["product"]="8082"
  ["order"]="8083"
  ["payment"]="8084"
  ["bff"]="8085"
)

echo "Starting Closet services with OpenTelemetry Agent (Jaeger tracing)..."
echo ""

for svc in "${!services[@]}"; do
  port="${services[$svc]}"
  jar_path="${PROJECT_ROOT}/closet-${svc}/build/libs/closet-${svc}-"*.jar

  # Check jar exists
  if ! ls ${jar_path} 1>/dev/null 2>&1; then
    echo "  [SKIP] closet-${svc} - JAR not found. Run './gradlew :closet-${svc}:bootJar' first."
    continue
  fi

  echo "  Starting closet-${svc} on :${port} with OpenTelemetry agent..."

  java -javaagent:"${OTEL_AGENT}" \
    -Dotel.service.name="closet-${svc}" \
    -Dotel.exporter.otlp.endpoint="http://localhost:4317" \
    -Dotel.traces.exporter=otlp \
    -Dotel.metrics.exporter=none \
    -Dotel.logs.exporter=none \
    -jar ${jar_path} \
    --server.port="${port}" \
    > "${LOG_DIR}/${svc}.log" 2>&1 &

  echo "    PID: $!"
done

echo ""
echo "Jaeger UI:     http://localhost:16686"
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

echo ""
echo "Verify tracing: curl -s http://localhost:16686/api/services | python3 -m json.tool"
