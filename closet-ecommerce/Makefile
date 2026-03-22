.PHONY: up down build test logs

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
