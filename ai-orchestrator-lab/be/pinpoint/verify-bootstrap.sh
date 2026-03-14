#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${ROOT_DIR}/pinpoint-agent.env"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "missing env file: ${ENV_FILE}" >&2
  exit 1
fi

set -a
source "${ENV_FILE}"
set +a

required_vars=(
  PINPOINT_AGENT_PATH
  PINPOINT_APPLICATION_NAME
  PINPOINT_AGENT_ID
  PINPOINT_COLLECTOR_HOST
  PINPOINT_COLLECTOR_TCP_PORT
  PINPOINT_SAMPLING_PERCENT
)

for var_name in "${required_vars[@]}"; do
  if [[ -z "${!var_name:-}" ]]; then
    echo "missing required variable: ${var_name}" >&2
    exit 1
  fi
done

cat <<EOF
Pinpoint bootstrap contract looks valid.
- applicationName: ${PINPOINT_APPLICATION_NAME}
- agentId: ${PINPOINT_AGENT_ID}
- collector: ${PINPOINT_COLLECTOR_HOST}:${PINPOINT_COLLECTOR_TCP_PORT}
- samplingPercent: ${PINPOINT_SAMPLING_PERCENT}
EOF
