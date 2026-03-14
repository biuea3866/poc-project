#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${ROOT_DIR}/pinpoint-agent.env"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "missing env file: ${ENV_FILE}" >&2
  echo "copy pinpoint-agent.env.example to pinpoint-agent.env first" >&2
  exit 1
fi

set -a
source "${ENV_FILE}"
set +a

JAVA_AGENT_OPTS=(
  "-javaagent:${PINPOINT_AGENT_PATH}"
  "-Dpinpoint.applicationName=${PINPOINT_APPLICATION_NAME}"
  "-Dpinpoint.agentId=${PINPOINT_AGENT_ID}"
  "-Dprofiler.transport.module=TCP"
  "-Dprofiler.collector.ip=${PINPOINT_COLLECTOR_HOST}"
  "-Dprofiler.collector.tcp.port=${PINPOINT_COLLECTOR_TCP_PORT}"
  "-Dprofiler.sampling.percent.rate=${PINPOINT_SAMPLING_PERCENT}"
)

printf '%s\n' "${JAVA_AGENT_OPTS[@]}"
