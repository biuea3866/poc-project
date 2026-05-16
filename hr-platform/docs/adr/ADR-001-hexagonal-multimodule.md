# ADR-001 — hr-platform 멀티모듈 골격과 Hexagonal Architecture 채택

**상태**: Accepted
**일자**: 2026-05-16
**작성자**: 메인 세션 (오케스트레이터)
**관련 문서**: TDD-001-employee-service.md, ADR-002-employee-ssot-model.md

## Context

hr-platform은 7개 도메인 서비스(auth · employee · attendance · leave-approval · payroll · performance · notification)로 시작해 5개월 안에 MVP를 출시해야 합니다. 현재 스캐폴드는 6개 디렉토리만 존재하고 `src/`가 모두 비어 있으며, 루트 `build.gradle.kts` / `settings.gradle.kts`가 없어 빌드가 불가능합니다.

다음 두 결정이 첫 도메인(employee-service) 구현 이전에 필요합니다:
1. **빌드 골격**: 멀티모듈을 어떻게 쪼갤 것인가
2. **레이어 패턴**: 도메인·인프라·UseCase를 어떻게 분리할 것인가

이 ADR은 두 결정을 함께 다룹니다. 도메인별 ADR(예: ADR-002 Employee SSOT 모델)은 별도 문서로 분리합니다.

## Decision

### 1. 서비스별 독립 Spring Boot Application + 도메인 내부 4-layer

| 결정 항목 | 결정 |
|---|---|
| 빌드 도구 | Gradle 8.14 + Kotlin DSL |
| 멀티모듈 단위 | **서비스 1개 = Spring Boot Application 1개**. 모듈 안에서 4-layer 패키지 분리. |
| 레이어 | `presentation` · `application` · `domain` · `infrastructure` 4-layer (Hexagonal) |
| 공통 모듈 | `core` (BaseEntity·DomainEvent·공통 예외) + `common-kafka` (Avro/JSON Serializer 등) — 모든 서비스가 의존 |
| Kotlin | 2.0.x · JDK 21 |
| Spring Boot | 3.4.x · Spring Cloud 2024.x |
| ORM | Spring Data JPA + QueryDSL 5.x (`@Query` 금지) |
| DB | MySQL 8.0 + Flyway, 도메인별 스키마 분리, FK 미사용 (서비스 경계) |
| 시간 타입 | `ZonedDateTime` UTC 저장 (`LocalDateTime` 금지, 메모리 룰 `zoned_datetime`) |
| 이벤트 | Kafka — 토픽명 `event.hr.{domain}` |

### 2. 서비스별 모듈 구조 (employee-service 기준 예시)

```
hr-platform/
├── settings.gradle.kts              # 모든 서비스·공통 모듈 include
├── build.gradle.kts                 # 공통 plugin·dependencyManagement
├── gradle/libs.versions.toml        # 버전 카탈로그
├── core/                            # 모든 서비스가 의존
│   └── src/main/kotlin/com/hrplatform/core/
│       ├── domain/  (BaseEntity, DomainEvent, AggregateRoot)
│       ├── exception/  (BusinessException, NotFoundException)
│       └── util/  (시간·통화·국가 코드 유틸)
├── common-kafka/
│   └── src/main/kotlin/com/hrplatform/kafka/
│       ├── publisher/  (DomainEventPublisher interface — 각 서비스가 구현 주입)
│       └── consumer/  (BaseEventWorker — 도메인별 EventWorker가 상속)
└── employee-service/
    ├── build.gradle.kts             # core + common-kafka 의존
    └── src/main/kotlin/com/hrplatform/employee/
        ├── presentation/             # Controller, EventWorker
        ├── application/              # UseCase, Command, Response
        ├── domain/                   # Entity, DomainService, Repository interface, Gateway interface
        │   ├── person/
        │   ├── employment/
        │   ├── department/
        │   └── history/
        └── infrastructure/           # JpaRepository, RepositoryImpl(QueryDSL), KafkaDomainEventPublisher
```

### 3. 레이어 의존 방향

```
presentation → application → domain ← infrastructure
                              ↑
                            core
```

- `domain`은 어느 것도 import하지 않음 (Spring·JPA·Kafka 의존 0)
- `infrastructure`가 `domain`의 Repository / Gateway / DomainEventPublisher interface를 구현
- 도메인 패키지 간 참조 금지 (`domain.employment`에서 `domain.person` import 불가, 공통은 `domain.common`만)

### 4. UseCase 규칙 (`be-code-convention.md` 강제)

- UseCase 1개 = 행위 1개 = 클래스 1개, `execute()` 10줄 이내
- `@Transactional`은 UseCase에 선언 (메모리 룰 `transactional_location`)
- **DomainService만 호출** — Repository/Gateway/EventPublisher 직접 참조 금지 (메모리 룰 `usecase_domain_service`)
- if-throw 검증 나열 금지 — Entity 내부 메서드로 캡슐화

## Alternatives Considered

### A. Microservice 1개 = Git 레포 1개

- Greeting 조직에서 이미 채택한 패턴. CI/CD·배포 분리가 자연스러움.
- **미채택 사유**: 5개월 MVP·7개 서비스 동시 부트스트랩 부담이 너무 큼. 단일 레포 멀티모듈에서 시작해 트래픽·팀이 성장하면 서비스별 레포 분리는 후속 마이그레이션으로 진행. 메모리 룰 `dual_documentation`도 단일 레포가 유리.

### B. Modular Monolith (단일 Application + 도메인 모듈)

- 모든 도메인을 한 Spring Boot Application에 담고 Kafka 없이 in-process 이벤트.
- **미채택 사유**: PRD 5.1이 명시적으로 `event.hr.{domain}` Kafka 토픽 분리·서비스 경계를 요구. CEO/CFO API 외부 공개·API-first 포지셔닝과도 충돌.

### C. Hexagonal + Port/Adapter (Inbound/Outbound Port 인터페이스)

- 전통적 Hexagonal 패턴은 application 안에 `InboundPort`(UseCase interface)와 `OutboundPort`(Repository interface) 양쪽을 둠.
- **미채택 사유**: 메모리 룰 `be-code-convention.md`가 "OutputPort 패턴은 사용하지 않습니다. Domain layer에 Repository/Gateway interface를 직접 정의"로 강제. Port 인터페이스 추가는 위반.

### D. Onion Architecture (4단 동심원)

- 의존 방향은 같지만 모듈 경계가 더 엄격.
- **미채택 사유**: Spring 생태계와 부딪치는 부분이 많아 학습 곡선·생산성 손해.

## Rationale (채택 사유)

1. **단일 레포 멀티모듈**이 5개월 MVP에 가장 빠르게 부트스트랩되고, 공통 모듈(`core`, `common-kafka`) 공유가 자유로움.
2. **서비스별 독립 Application**으로 Kafka 토픽·DB 스키마·배포 단위를 분리 → PRD의 "API-first / 외부 공개" 요구와 정렬.
3. **4-layer Hexagonal**은 메모리 룰 `be-code-convention.md`와 정확히 일치 — Domain 순수성·UseCase 얇음·Infrastructure 분리.
4. **Port 인터페이스 미사용**은 기존 다른 프로젝트의 마이그레이션 학습(Greeting ATS) 결과 — 단순 위임 Adapter가 양산되며 Rich Domain Model로 흡수해야 함. 처음부터 안 만드는 게 안전.
5. **공통 모듈 2개(`core`, `common-kafka`)** 로 최소화 — 너무 잘게 쪼개면 모듈 간 의존이 거미줄.

## Consequences

### Positive
- 신규 서비스(`leave-approval-service`, `performance-service`)를 디렉토리 추가 + `settings.gradle.kts` include 1줄로 부트스트랩.
- 도메인 코드가 Spring·JPA·Kafka로부터 격리되어 단위 테스트가 가볍고 빠름.
- 메모리 룰 위반이 컴파일·정적분석(detekt 커스텀 룰)에서 자동 차단 가능.

### Negative
- 단일 레포가 커지면 IDE 인덱싱·CI 빌드 시간이 증가. 8개 서비스 도달 시점에 분리 검토 필요.
- `core` 모듈 변경이 모든 서비스 재컴파일 트리거 — `core`는 정말 안정된 abstraction만.
- Spring Boot Application이 7개로 늘면 로컬 개발 환경 관리(docker-compose) 부담. Phase 2에서 일부 통합 가능성 검토.

### Follow-up Decisions (별도 ADR)
- ADR-002: Employee SSOT 모델 (Person + Employment 분리)
- ADR-003 (예정): Kafka 토픽 명명·파티션·보존 정책
- ADR-004 (예정): 권한 (Auth 도입 시): JWT·RBAC·자동 범위 필터링

## Verification

ADR 채택이 실현됐는지 검증:

```bash
# 멀티모듈 골격
test -f hr-platform/settings.gradle.kts
test -f hr-platform/build.gradle.kts
test -f hr-platform/gradle/libs.versions.toml

# 공통 모듈
test -d hr-platform/core/src/main/kotlin/com/hrplatform/core
test -d hr-platform/common-kafka/src/main/kotlin/com/hrplatform/kafka

# 서비스별 4-layer
ls hr-platform/employee-service/src/main/kotlin/com/hrplatform/employee/
# → presentation, application, domain, infrastructure 4개 디렉토리

# Port 인터페이스 0건
grep -rn "interface .*Port" hr-platform/employee-service/src/main --include="*.kt" | wc -l   # → 0

# typealias 호환 layer 0건
find hr-platform -name "*TypeAliases.kt" -o -name "*Compat.kt" -o -name "*Aliases.kt" | wc -l   # → 0
```

위 7개 검증이 모두 통과해야 멀티모듈 부트스트랩 티켓(BS-01) 완료로 인정.
