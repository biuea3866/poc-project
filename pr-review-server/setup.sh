#!/bin/bash
set -e

cd "$(dirname "$0")"

echo "=== PR Review Server Setup ==="

# 1. Check Node.js
if ! command -v node &> /dev/null; then
  echo "ERROR: Node.js not found. Install Node.js 18+ first."
  exit 1
fi

echo "[1/5] Node.js $(node -v)"

# 2. Install dependencies
if [ ! -d "node_modules" ]; then
  echo "[2/5] Installing dependencies..."
  npm install
else
  echo "[2/5] Dependencies already installed"
fi

# 3. Create .env if not exists
if [ ! -f ".env" ]; then
  echo "[3/5] Creating .env from .env.example..."
  cp .env.example .env
  echo ""
  echo "  *** .env 파일을 편집해주세요 ***"
  echo "  필수: GITHUB_TOKEN, WEBHOOK_SECRET_PR_REVIEW"
  echo "  선택: ATLASSIAN_EMAIL, ATLASSIAN_API_TOKEN"
  echo "  smee: SMEE_URL (https://smee.io/new 에서 생성)"
  echo ""
  exit 1
else
  echo "[3/5] .env exists"
fi

# 4. Validate required env vars
source <(grep -v '^#' .env | sed 's/^/export /')

if [ -z "$GITHUB_TOKEN" ] || [ "$GITHUB_TOKEN" = "ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" ]; then
  echo "ERROR: GITHUB_TOKEN이 설정되지 않았어요. .env 파일을 확인해주세요."
  exit 1
fi

if [ -z "$WEBHOOK_SECRET_PR_REVIEW" ] || [ "$WEBHOOK_SECRET_PR_REVIEW" = "your_webhook_secret_here" ]; then
  echo "ERROR: WEBHOOK_SECRET_PR_REVIEW가 설정되지 않았어요. .env 파일을 확인해주세요."
  exit 1
fi

echo "[4/5] Environment validated"

# 5. Start
echo "[5/5] Starting..."

# Start smee proxy if SMEE_URL is set
if [ -n "$SMEE_URL" ] && [ "$SMEE_URL" != "https://smee.io/your-channel-id" ]; then
  echo "  Starting smee proxy: $SMEE_URL → http://127.0.0.1:${PORT:-3847}/webhook"
  npx smee -u "$SMEE_URL" -t "http://127.0.0.1:${PORT:-3847}/webhook" &
  SMEE_PID=$!
  sleep 1
fi

# Start server
echo "  Starting PR Review Server..."
npx tsx src/index.ts &
SERVER_PID=$!

# Trap to cleanup on exit
cleanup() {
  echo ""
  echo "Shutting down..."
  [ -n "$SMEE_PID" ] && kill $SMEE_PID 2>/dev/null
  [ -n "$SERVER_PID" ] && kill $SERVER_PID 2>/dev/null
  exit 0
}
trap cleanup SIGINT SIGTERM

echo ""
echo "=== PR Review Server Running ==="
echo "  Dashboard: http://127.0.0.1:${PORT:-3847}"
echo "  Webhook:   http://127.0.0.1:${PORT:-3847}/webhook"
echo ""
echo "  Ctrl+C to stop"
echo ""

# Wait for server process
wait $SERVER_PID
