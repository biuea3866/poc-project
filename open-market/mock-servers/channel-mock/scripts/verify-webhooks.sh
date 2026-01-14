#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

node "$ROOT_DIR/dist/app.js" > /tmp/channel-mock-webhook.log 2>&1 &
SERVER_PID=$!
trap 'kill "$SERVER_PID" 2>/dev/null || true' EXIT

sleep 1

health=$(curl -s http://localhost:8082/health)

st11_auth=$(curl -s -X POST http://localhost:8082/api/st11/auth/token \
  -H "Content-Type: application/json" \
  -d '{"openapiKey":"test","secretKey":"secret"}')

st11_token=$(printf "%s" "$st11_auth" | node -e 'const fs=require("fs"); const d=JSON.parse(fs.readFileSync(0,"utf8")); console.log(d.data.accessToken);')

naver_auth=$(curl -s -X POST http://localhost:8082/api/naver/oauth/token \
  -H "Content-Type: application/json" \
  -d '{"client_id":"test","client_secret":"secret"}')

naver_token=$(printf "%s" "$naver_auth" | node -e 'const fs=require("fs"); const d=JSON.parse(fs.readFileSync(0,"utf8")); console.log(d.access_token);')

kakao_auth=$(curl -s -X POST http://localhost:8082/api/kakao/oauth/token \
  -H "Content-Type: application/json" \
  -d '{"client_id":"test","client_secret":"secret"}')

kakao_token=$(printf "%s" "$kakao_auth" | node -e 'const fs=require("fs"); const d=JSON.parse(fs.readFileSync(0,"utf8")); console.log(d.access_token);')

toss_auth=$(curl -s -X POST http://localhost:8082/api/toss/v1/auth/token \
  -H "Content-Type: application/json" \
  -d '{"clientId":"test","clientSecret":"secret"}')

toss_token=$(printf "%s" "$toss_auth" | node -e 'const fs=require("fs"); const d=JSON.parse(fs.readFileSync(0,"utf8")); console.log(d.accessToken);')

curl -s -X POST http://localhost:8082/api/st11/webhooks/register \
  -H "Authorization: Bearer $st11_token" \
  -H "Content-Type: application/json" \
  -d '{"url":"http://localhost:8082/webhooks/receiver","events":["ORDER.CREATED"]}' > /dev/null

curl -s -X POST http://localhost:8082/api/naver/webhooks/register \
  -H "Authorization: Bearer $naver_token" \
  -H "Content-Type: application/json" \
  -d '{"url":"http://localhost:8082/webhooks/receiver","events":["ORDER.CREATED"]}' > /dev/null

curl -s -X POST http://localhost:8082/api/kakao/webhooks/register \
  -H "Authorization: Bearer $kakao_token" \
  -H "Content-Type: application/json" \
  -d '{"url":"http://localhost:8082/webhooks/receiver","events":["ORDER.CREATED"]}' > /dev/null

curl -s -X POST http://localhost:8082/api/toss/webhooks/register \
  -H "Authorization: Bearer $toss_token" \
  -H "Content-Type: application/json" \
  -d '{"url":"http://localhost:8082/webhooks/receiver","events":["ORDER.CREATED"]}' > /dev/null

curl -s -X POST http://localhost:8082/api/coupang/webhooks/register \
  -H "Content-Type: application/json" \
  -d '{"url":"http://localhost:8082/webhooks/receiver","events":["ORDER.CREATED"]}' > /dev/null

st11_trigger=$(curl -s -X POST http://localhost:8082/api/st11/webhooks/trigger \
  -H "Authorization: Bearer $st11_token" \
  -H "Content-Type: application/json" \
  -d '{"eventType":"ORDER.CREATED"}')

naver_trigger=$(curl -s -X POST http://localhost:8082/api/naver/webhooks/trigger \
  -H "Authorization: Bearer $naver_token" \
  -H "Content-Type: application/json" \
  -d '{"eventType":"ORDER.CREATED"}')

kakao_trigger=$(curl -s -X POST http://localhost:8082/api/kakao/webhooks/trigger \
  -H "Authorization: Bearer $kakao_token" \
  -H "Content-Type: application/json" \
  -d '{"eventType":"ORDER.CREATED"}')

toss_trigger=$(curl -s -X POST http://localhost:8082/api/toss/webhooks/trigger \
  -H "Authorization: Bearer $toss_token" \
  -H "Content-Type: application/json" \
  -d '{"eventType":"ORDER.CREATED"}')

coupang_trigger=$(curl -s -X POST http://localhost:8082/api/coupang/webhooks/trigger \
  -H "Content-Type: application/json" \
  -d '{"eventType":"ORDER.CREATED"}')

received=$(curl -s http://localhost:8082/webhooks/received?limit=10)

printf "HEALTH:%s\n" "$health"
printf "ST11_TRIGGER:%s\n" "$st11_trigger"
printf "NAVER_TRIGGER:%s\n" "$naver_trigger"
printf "KAKAO_TRIGGER:%s\n" "$kakao_trigger"
printf "TOSS_TRIGGER:%s\n" "$toss_trigger"
printf "COUPANG_TRIGGER:%s\n" "$coupang_trigger"
printf "RECEIVED:%s\n" "$received"
