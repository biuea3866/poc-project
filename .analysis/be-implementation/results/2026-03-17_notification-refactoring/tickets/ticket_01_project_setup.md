# [GRT-4001] greeting-notification-service 프로젝트 셋업

## 개요
- PRD: https://doodlin.atlassian.net/wiki/x/SICjdg
- Phase: 1 (서비스 구축)
- 예상 공수: 3d
- 의존성: 없음

**범위:** Node.js notification_server + alert-server 대체 신규 Spring Boot 서비스 생성 (Hexagonal Architecture, Gradle Multi-module)

## 작업 내용

### 1. Gradle Multi-module 프로젝트 구조 생성

```
greeting-notification-service/
├── settings.gradle.kts
├── build.gradle.kts                    # root (공통 의존성, 플러그인)
├── bootstrap/
│   ├── build.gradle.kts                # Spring Boot 실행 모듈
│   └── src/main/kotlin/.../GreetingNotificationServiceApplication.kt
├── domain/
│   ├── build.gradle.kts                # 순수 Kotlin (Spring 의존성 없음)
│   └── src/main/kotlin/.../
├── application/
│   ├── build.gradle.kts                # UseCase, Port 인터페이스
│   └── src/main/kotlin/.../
├── infrastructure/
│   ├── build.gradle.kts                # JPA, Kafka, Redis, WebSocket
│   └── src/main/kotlin/.../
└── presentation/
    ├── build.gradle.kts                # REST Controller
    └── src/main/kotlin/.../
```

### 2. root build.gradle.kts 설정

- Kotlin 1.9.x + Spring Boot 3.2.x + Java 17
- 공통 플러그인: kotlin-jvm, kotlin-spring, kotlin-jpa, ktlint
- 공통 의존성: kotlin-stdlib, kotlin-reflect, jackson-module-kotlin
- 하위 모듈별 의존성 분리

### 3. 모듈별 build.gradle.kts

| 모듈 | 주요 의존성 |
|------|-----------|
| domain | 없음 (순수 Kotlin) |
| application | domain |
| infrastructure | application, spring-boot-starter-data-jpa, spring-kafka, spring-data-redis, netty-socketio |
| presentation | application, spring-boot-starter-web |
| bootstrap | infrastructure, presentation, spring-boot-starter |

### 4. application.yml 설정

```yaml
spring:
  profiles:
    active: local
  datasource:
    url: jdbc:mysql://localhost:3306/greeting
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      read-only: false
  datasource-read:
    url: jdbc:mysql://localhost:3307/greeting
    hikari:
      maximum-pool-size: 30
      read-only: true
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: notification-consumer
      auto-offset-reset: earliest
      enable-auto-commit: false
    producer:
      acks: all
      retries: 3
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}

server:
  port: 8080

socketio:
  host: 0.0.0.0
  port: 9092

jwt:
  secret: ${JWT_SECRET}
  algorithm: HS256

logging:
  level:
    root: INFO
    com.doodlin.greeting.notification: DEBUG
```

### 5. Dockerfile

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY bootstrap/build/libs/bootstrap-*.jar app.jar
EXPOSE 8080 9092
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 6. Helm Chart

- `charts/greeting-notification-service/`
- values.yaml: replicas, resources, env, ingress, serviceAccount
- deployment.yaml: liveness/readiness probe (`/actuator/health`)
- service.yaml: 8080 (HTTP) + 9092 (WebSocket)
- ingress.yaml: `/service/notification/*` 라우팅

### 7. CI/CD 파이프라인

- GitHub Actions workflow: build → test → docker build → push → helm deploy
- dev/stage/prod 환경별 values 파일 분리

### 수정 파일 목록

| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting-notification-service | root | settings.gradle.kts | 신규 |
| greeting-notification-service | root | build.gradle.kts | 신규 |
| greeting-notification-service | bootstrap | build.gradle.kts | 신규 |
| greeting-notification-service | bootstrap | src/.../GreetingNotificationServiceApplication.kt | 신규 |
| greeting-notification-service | domain | build.gradle.kts | 신규 |
| greeting-notification-service | application | build.gradle.kts | 신규 |
| greeting-notification-service | infrastructure | build.gradle.kts | 신규 |
| greeting-notification-service | presentation | build.gradle.kts | 신규 |
| greeting-notification-service | bootstrap | src/main/resources/application.yml | 신규 |
| greeting-notification-service | bootstrap | src/main/resources/application-local.yml | 신규 |
| greeting-notification-service | bootstrap | src/main/resources/application-dev.yml | 신규 |
| greeting-notification-service | root | Dockerfile | 신규 |
| greeting-notification-service | root | charts/ | 신규 |
| greeting-notification-service | root | .github/workflows/ci.yml | 신규 |

## 영향 범위

- 신규 레포지토리 생성, 기존 서비스 영향 없음
- GitHub Organization에 새 레포 생성 필요
- Kubernetes 클러스터에 새 Namespace/ServiceAccount 생성 필요
- Container Registry 이미지 푸시 권한 설정 필요

## 테스트 케이스

| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| TC-01-01 | Gradle 빌드 성공 | 프로젝트 구조 완성 | `./gradlew build` 실행 | BUILD SUCCESSFUL, 모든 모듈 컴파일 성공 |
| TC-01-02 | 로컬 실행 확인 | application-local.yml 설정, 외부 의존성 미연결 | `./gradlew :bootstrap:bootRun` 실행 | 애플리케이션 기동, /actuator/health 200 OK |
| TC-01-03 | Docker 이미지 빌드 | Gradle 빌드 완료 | `docker build -t greeting-notification-service .` | 이미지 빌드 성공, 컨테이너 실행 시 8080 포트 리슨 |
| TC-01-04 | 모듈 의존성 격리 확인 | domain 모듈 | domain에서 Spring 클래스 import 시도 | 컴파일 에러 발생 (Spring 의존성 없음) |
| TC-01-05 | Read/Write DB 분리 | DataSource 설정 완료 | 읽기/쓰기 쿼리 실행 | 읽기는 read DataSource, 쓰기는 write DataSource 사용 |

## 기대 결과 (AC)

- [ ] `./gradlew build` 성공 (모든 모듈 컴파일 + 테스트 통과)
- [ ] `./gradlew :bootstrap:bootRun`으로 로컬 실행 시 `/actuator/health` 200 OK
- [ ] Docker 이미지 빌드 성공 및 컨테이너 정상 기동
- [ ] domain 모듈에 Spring Framework 의존성이 존재하지 않음
- [ ] Read/Write DataSource 분리 설정 확인
- [ ] Helm chart 렌더링 성공 (`helm template` 에러 없음)

## 체크리스트

- [ ] Kotlin 코딩 컨벤션 (ktlint) 설정
- [ ] .gitignore 설정 (build/, .gradle/, .idea/)
- [ ] 패키지 네이밍: `com.doodlin.greeting.notification`
- [ ] Spring Boot Actuator 포함 (health, info, prometheus)
- [ ] logback-spring.xml 설정 (JSON 포맷, Datadog 연동)
- [ ] GitHub repository 생성 + branch protection rule 설정
