.PHONY: up down build test logs start start-apm start-trace stop pinpoint zipkin \
       signoz-up signoz-down signoz \
       logs-all logs-member logs-product logs-order logs-payment logs-gateway logs-bff logs-errors

up: ## Start infra only (MySQL, Redis, Kafka, ES, monitoring)
	docker compose -f docker/docker-compose.yml up -d mysql redis kafka elasticsearch prometheus grafana tempo loki zipkin mysql-exporter redis-exporter kafka-exporter node-exporter

down: ## Stop all
	docker compose -f docker/docker-compose.yml down

docker-build: ## Build all 17 service Docker images
	./gradlew bootJar -x test
	docker compose -f docker/docker-compose.yml build

docker-up: ## Start ALL (infra + 17 services) in Docker
	docker compose -f docker/docker-compose.yml up -d

docker-down: ## Stop ALL
	docker compose -f docker/docker-compose.yml down

docker-restart: ## Rebuild + restart all
	./gradlew bootJar -x test
	docker compose -f docker/docker-compose.yml up -d --build

docker-logs: ## Follow all container logs
	docker compose -f docker/docker-compose.yml logs -f

docker-ps: ## Show all container status
	docker compose -f docker/docker-compose.yml ps

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

start-trace: ## Start all services with OpenTelemetry Agent (Jaeger tracing)
	@bash bin/start-with-jaeger.sh

stop: ## Stop all services
	@bash bin/stop-all.sh

pinpoint: ## Open Pinpoint Web UI
	@open http://localhost:28080

zipkin: ## Open Zipkin UI
	@open http://localhost:9411

signoz-up: ## Start SigNoz APM
	docker compose -f docker/signoz/docker-compose.yml up -d

signoz-down: ## Stop SigNoz APM
	docker compose -f docker/signoz/docker-compose.yml down

signoz: ## Open SigNoz Web UI
	@open http://localhost:3301

# === 부하 테스트 ===
load-realistic: ## 현실적 트래픽 (50 VU, 10분) — 8가지 유저 행동 패턴
	k6 run load-test/realistic-traffic.js

load-sustained: ## 지속 트래픽 (50 VU, 1시간)
	k6 run -e DURATION=1h load-test/realistic-traffic.js

load-spike: ## 스파이크 트래픽 (ramp up/down, 타임세일 시뮬레이션)
	k6 run load-test/spike-traffic.js

load-bg: ## 백그라운드 지속 트래픽 (50 VU, 1시간)
	nohup k6 run -e DURATION=1h load-test/realistic-traffic.js > /tmp/closet-logs/k6.log 2>&1 &
	@echo "백그라운드 실행 중. 로그: /tmp/closet-logs/k6.log"

load-stop: ## 부하 테스트 중지
	pkill -f "k6 run" 2>/dev/null || true
	@echo "부하 테스트 중지됨"

# === 로그 조회 ===
logs-all: ## View all service logs (follow)
	@tail -f /tmp/closet-logs/*.log

logs-member: ## View member service logs
	@tail -50 /tmp/closet-logs/member.log

logs-product: ## View product service logs
	@tail -50 /tmp/closet-logs/product.log

logs-order: ## View order service logs
	@tail -50 /tmp/closet-logs/order.log

logs-payment: ## View payment service logs
	@tail -50 /tmp/closet-logs/payment.log

logs-gateway: ## View gateway service logs
	@tail -50 /tmp/closet-logs/gateway.log

logs-bff: ## View BFF service logs
	@tail -50 /tmp/closet-logs/bff.log

logs-errors: ## View errors across all services
	@grep -h "ERROR\|Exception" /tmp/closet-logs/*.log | tail -50

logs-svc: ## View specific service logs (follow). Usage: make logs-svc SVC=member
	@bash bin/logs.sh $(SVC)
