.PHONY: up down build test logs start start-apm stop pinpoint

up:
	docker compose -f docker/docker-compose.yml up -d

down:
	docker compose -f docker/docker-compose.yml down

build:
	./gradlew clean build -x test

test:
	./gradlew test

logs:
	docker compose -f docker/docker-compose.yml logs -f

start: ## Start all services (without Pinpoint)
	@bash bin/start-all.sh

start-apm: ## Start all services with Pinpoint APM
	@bash bin/start-with-pinpoint.sh

stop: ## Stop all services
	@bash bin/stop-all.sh

pinpoint: ## Open Pinpoint Web UI
	@open http://localhost:28080

# === 부하 테스트 ===
load-light: ## 가벼운 트래픽 (10 VU, 5분)
	k6 run load-test/realistic-traffic.js

load-medium: ## 중간 트래픽 (50 VU, 30분)
	k6 run --duration 30m -e BROWSERS=30 -e SHOPPERS=13 -e BUYERS=7 load-test/realistic-traffic.js

load-heavy: ## 높은 트래픽 (100 VU, 1시간)
	k6 run --duration 1h -e BROWSERS=60 -e SHOPPERS=25 -e BUYERS=15 load-test/realistic-traffic.js

load-bg: ## 백그라운드 지속 트래픽 (10 VU, 24시간)
	nohup k6 run --duration 24h -e BROWSERS=5 -e SHOPPERS=3 -e BUYERS=2 load-test/realistic-traffic.js > /tmp/closet-logs/k6.log 2>&1 &
	@echo "백그라운드 실행 중. 로그: /tmp/closet-logs/k6.log"

load-stop: ## 부하 테스트 중지
	pkill -f "k6 run" 2>/dev/null || true
	@echo "부하 테스트 중지됨"
