#!/bin/bash
# View Closet service logs
# Usage: bin/logs.sh [service|all]
# Examples:
#   bin/logs.sh          # all services (follow)
#   bin/logs.sh all      # all services (follow)
#   bin/logs.sh member   # member service only (follow)
#   bin/logs.sh gateway  # gateway service only (follow)

SERVICE=${1:-all}
LOG_DIR="/tmp/closet-logs"

if [ ! -d "${LOG_DIR}" ]; then
  echo "Log directory not found: ${LOG_DIR}"
  echo "Start services first: make start or make start-trace"
  exit 1
fi

if [ "${SERVICE}" = "all" ]; then
  echo "Tailing all service logs in ${LOG_DIR}..."
  echo "Press Ctrl+C to stop."
  echo ""
  tail -f "${LOG_DIR}"/*.log
else
  LOG_FILE="${LOG_DIR}/${SERVICE}.log"
  if [ ! -f "${LOG_FILE}" ]; then
    echo "Log file not found: ${LOG_FILE}"
    echo "Available logs:"
    ls "${LOG_DIR}"/*.log 2>/dev/null || echo "  (none)"
    exit 1
  fi
  echo "Tailing ${SERVICE} service log..."
  echo "Press Ctrl+C to stop."
  echo ""
  tail -f "${LOG_FILE}"
fi
