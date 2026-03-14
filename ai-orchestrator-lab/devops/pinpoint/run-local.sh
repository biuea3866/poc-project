#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${ROOT_DIR}/.env"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "missing env file: ${ENV_FILE}" >&2
  echo "copy .env.example to .env first" >&2
  exit 1
fi

docker compose --env-file "${ENV_FILE}" -f "${ROOT_DIR}/docker-compose.yml" up -d
docker compose --env-file "${ENV_FILE}" -f "${ROOT_DIR}/docker-compose.yml" ps
