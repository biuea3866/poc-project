#!/bin/bash
# Download Pinpoint Agent v2.5.1
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
AGENT_DIR="${SCRIPT_DIR}/pinpoint-agent-2.5.1"
AGENT_TAR="pinpoint-agent-2.5.1.tar.gz"
DOWNLOAD_URL="https://github.com/pinpoint-apm/pinpoint/releases/download/v2.5.1/${AGENT_TAR}"

if [ -d "${AGENT_DIR}" ]; then
  echo "Pinpoint agent already exists at ${AGENT_DIR}"
  exit 0
fi

echo "Downloading Pinpoint Agent v2.5.1..."
cd "${SCRIPT_DIR}"
curl -LO "${DOWNLOAD_URL}"

echo "Extracting..."
tar -xzf "${AGENT_TAR}"
rm -f "${AGENT_TAR}"

echo "Pinpoint agent installed at ${AGENT_DIR}"
