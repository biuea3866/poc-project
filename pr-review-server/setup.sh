#!/bin/bash
set -e

cd "$(dirname "$0")"

echo "=== PR Review Server Setup ==="

# 1. Check Node.js
if ! command -v node &> /dev/null; then
  echo "ERROR: Node.js not found. Install Node.js 18+ first."
  exit 1
fi

echo "[1/4] Node.js $(node -v)"

# 2. Install dependencies
if [ ! -d "node_modules" ]; then
  echo "[2/4] Installing dependencies..."
  npm install
else
  echo "[2/4] Dependencies already installed"
fi

# 3. Create .env if not exists
if [ ! -f ".env" ]; then
  echo "[3/4] Creating .env from .env.example..."
  cp .env.example .env
  echo ""
  echo "  *** .env 파일을 편집해주세요 ***"
  echo "  필수: GITHUB_TOKEN, WEBHOOK_SECRET_PR_REVIEW"
  echo "  선택: SMEE_URL, ATLASSIAN_EMAIL, ATLASSIAN_API_TOKEN"
  echo ""
  exit 1
else
  echo "[3/4] .env exists"
fi

# 4. Validate required env vars
source <(grep -v '^#' .env | grep -v '^\s*$' | sed 's/^/export /')

if [ -z "$GITHUB_TOKEN" ] || [ "$GITHUB_TOKEN" = "ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" ]; then
  echo "ERROR: GITHUB_TOKEN이 설정되지 않았어요. .env 파일을 확인해주세요."
  exit 1
fi

if [ -z "$WEBHOOK_SECRET_PR_REVIEW" ] || [ "$WEBHOOK_SECRET_PR_REVIEW" = "your_webhook_secret_here" ]; then
  echo "ERROR: WEBHOOK_SECRET_PR_REVIEW가 설정되지 않았어요. .env 파일을 확인해주세요."
  exit 1
fi

echo "[4/4] Environment validated"

# Start server (smee proxy + poller are managed internally)
echo ""
echo "Starting PR Review Server..."
echo "  - smee proxy: 서버 내장 (SMEE_URL 설정 시 자동 연결, 끊기면 자동 재연결)"
echo "  - poller: 서버 내장 (놓친 PR 자동 감지)"
echo ""

npx tsx src/index.ts
