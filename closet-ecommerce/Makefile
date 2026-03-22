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
