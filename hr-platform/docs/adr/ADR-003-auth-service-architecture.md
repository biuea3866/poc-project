# ADR-003 — auth-service 아키텍처 (모듈 구조 · 토큰 · 잠금 · 이벤트 계약)

**상태**: Accepted
**일자**: 2026-05-18
**작성자**: 메인 세션 (오케스트레이터)
**관련 문서**: ADR-001, ADR-002, TDD-002-auth-service.md, PRD §5.1·5.2·9.1·10.2·11.1

## Context

hr-platform M1의 두 번째 도메인인 auth-service를 신설합니다. employee-service M1(Person·Employment·Department·EmploymentHistory + `event.hr.employee.v1` 토픽 + KafkaDomainEventPublisher)이 main에 머지된 상태에서, auth-service는 **인증·인가의 SSOT(UserAccount·Role)** 로서 다음을 모두 책임집니다.

1. 15개 REST API (공개 5 + 본인 6 + 관리자 4) — JWT 발급/검증/2FA/비밀번호/역할/세션/API 토큰
2. 7개 Entity (UserAccount · Role · UserAccountRole · RefreshToken · LoginAttempt · TwoFactorBackupCode · ApiToken)
3. 11종 DomainEvent → 신규 토픽 `event.hr.auth.v1`
4. employee 4종 이벤트(`event.hr.employee.v1`) **구독** — Hired/Resigned/Suspended/Resumed → UserAccount 자동 동기화

PRD가 분실되어 메모리 컨텍스트에서 재구성한 명세(`auth-prd-reconstructed.md`)와 TPM 분석(`tpm-analysis.md`) · 검수(`tpm-review.md` NEEDS_REVISION p0 1건 + p1 3건)를 기준으로 본 ADR에서 다음 5개 결정을 확정합니다.

1. 모듈 구조 — ADR-001 4-layer 그대로 적용하되 employee 모듈 의존 0
2. 토큰·잠금·암호화 라이브러리 선택 (jjwt · bcrypt · TOTP · Redis · AES-GCM)
3. UserAccount 상태 머신 4상태 + employee status 매핑
4. DomainEvent 11종 페이로드 규약 + 토픽 라우팅 분리
5. employee 이벤트 구독 정책 (X1 시나리오: resigned → 세션 강제 종료 + DEACTIVATED)

ADR-002는 employee-service가 비밀번호·세션을 보유하지 **않음**을 명시했고, 본 ADR은 그 반대 방향의 경계(auth는 PII 보유 안 함, employmentId 참조만)를 확정합니다.

## Decision

### 1. 모듈 구조 — ADR-001 4-layer 재적용 + employee 의존 0

| 결정 항목 | 결정 |
|---|---|
| 모듈 단위 | `hr-platform/auth-service` 단일 Spring Boot Application (ADR-001 §1과 동일) |
| 4-layer 패키지 | `com.hrplatform.auth.{presentation,application,domain,infrastructure}` |
| 공통 모듈 의존 | `core` + `common-kafka` 만 의존. **employee-service 모듈 의존 0** (검증: `auth-service/build.gradle.kts`에 `:employee-service` 미선언) |
| Java/Kotlin | JDK 21 / Kotlin 2.0.x (ADR-001 동일) |
| Spring Boot | 3.4.x + Spring Security 6.4 + Spring Boot Starter Data Redis |
| 포트 | 로컬 8081 (employee 8080과 분리) |
| Flyway 디렉토리 | `auth-service/src/main/resources/db/migration/` (employee와 별도) |

#### 도메인 패키지 분리

```
com.hrplatform.auth/
├── presentation/
│   ├── controller/  (AuthApiController, MyAuthController, AdminAuthController)
│   └── consumer/    (EmployeeEventWorker)
├── application/
│   ├── auth/        (Login/Logout/Refresh/Me UseCase + Command/Response)
│   ├── twofactor/   (2FA enroll/verify UseCase)
│   ├── password/    (Reset/Change UseCase + NotificationGateway)
│   ├── admin/       (Role 관리/Unlock/SessionsTerminate/ApiToken UseCase)
│   └── sync/        (UserAccountSyncUseCase — Worker에서 호출)
├── domain/
│   ├── account/     (UserAccount Entity + Repository + Status enum)
│   ├── role/        (Role + UserAccountRole + RoleCode enum + Repository)
│   ├── token/       (RefreshToken + ApiToken + Repository)
│   ├── twofactor/   (TwoFactorBackupCode + Repository)
│   ├── login/       (LoginAttempt — append-only + Repository)
│   ├── event/       (11종 DomainEvent + DomainEventPublisher interface)
│   └── service/     (AuthDomainService · TwoFactorDomainService · RoleDomainService · AdminAuthDomainService)
└── infrastructure/
    ├── persistence/ (Jpa*Repository + *RepositoryImpl QueryDSL)
    ├── kafka/       (AuthKafkaDomainEventPublisher + AuthKafkaConfig)
    ├── security/    (JwtIssuer + JwtVerifier + JwtAuthenticationFilter + SecurityConfig + RedisJtiBlacklist)
    ├── crypto/      (BcryptPasswordHasher + TotpGenerator + AesGcmStringConverter 복제)
    ├── audit/       (SpringAuditorAware — auth-service 전용 복제)
    └── notification/(LogNotificationGateway — MVP stub)
```

### 2. 토큰·잠금·암호화 라이브러리 선택

| 항목 | 선택 | 버전 | 비고 |
|---|---|---|---|
| JWT | `io.jsonwebtoken:jjwt` | 0.12.6 | api/impl/jackson 3개 모듈. access 30분, refresh 14일 (PRD §10.2) |
| 비밀번호 해시 | `org.springframework.security:spring-security-crypto` | Spring Security 6.4 | bcrypt cost 12 (PRD §10.2) |
| 2FA TOTP | `dev.samstevens.totp:totp` | 1.7.1 | RFC 6238, 30초 윈도우, ±1 step 허용 |
| Redis | `spring-boot-starter-data-redis` (Lettuce 기본) | Spring Boot 3.4 | jti blacklist, 잠금 카운터, idempotency |
| AES-256-GCM | 직접 구현 (`AesGcmStringConverter` 복제) | JDK 21 표준 javax.crypto | 2FA secret 컬럼 암호화 전용. employee의 동일 클래스를 auth 패키지에 **복제** (모듈 의존 0 유지) |
| QueryDSL | 5.x | ADR-001 동일 | `@Query` 금지 |

#### 키·시크릿 관리

| 시크릿 | 보관 | 회전 |
|---|---|---|
| JWT HMAC secret (HS256) | `application-{profile}.yml` 환경변수 `HRPLATFORM_AUTH_JWT_SECRET` | Phase 2에서 RSA로 회전, MVP는 HS256 단일 키 |
| AES-GCM 마스터 키 | 환경변수 `HRPLATFORM_AUTH_AES_KEY` (base64 32바이트) | 회전 별도 ADR (employee와 동일 정책) |
| bcrypt salt | 자동 (cost 12 내장) | — |

### 3. UserAccount 4상태 머신 + employee status 매핑

#### 4상태

```
[*]  ─create→  ACTIVE  ─lock(5회 실패)→  LOCKED
                 │                        │
                 │                        └─unlock(수동 / 15분 백오프)→ ACTIVE
                 │
                 ├─suspend→  SUSPENDED  ─reactivate→ ACTIVE
                 │
                 └─deactivate→  DEACTIVATED  (종착, 재활성 불가)
```

| 전이 | DomainEvent | 호출 메서드 |
|---|---|---|
| (none) → ACTIVE | UserCreated | `UserAccount.create(...)` |
| ACTIVE → LOCKED | UserLocked | `UserAccount.lock(lockedUntil)` |
| LOCKED → ACTIVE (수동) | UserUnlocked(trigger=MANUAL) | `UserAccount.unlock(actor)` |
| LOCKED → ACTIVE (자동 15분) | UserUnlocked(trigger=AUTO) | `UserAccount.tryAutoUnlock(now)` |
| ACTIVE → SUSPENDED | UserSuspended | `UserAccount.suspend(reason)` |
| SUSPENDED → ACTIVE | UserReactivated | `UserAccount.reactivate()` |
| ACTIVE/LOCKED/SUSPENDED → DEACTIVATED | UserDeactivated | `UserAccount.deactivate(reason)` |

전이 규칙은 `UserAccountStatus.canTransitTo(target: UserAccountStatus)` enum 내부에 캡슐화. DEACTIVATED는 종착 상태로 어떤 전이도 거부합니다 (Entity 검증에서 `IllegalStateTransitionException`).

#### 자동 잠금 해제 — Lazy 체크

배경 스케줄러 도입 없이 `AuthDomainService.authenticate()`에서 `lockedUntil < now`이면 즉시 unlock + `UserUnlocked(trigger=AUTO)` 이벤트 발행 후 계속 진행 (TPM 검수 권고 반영). 인프라 비용 0 + 정확성 보장.

#### employee status ↔ UserAccount status 매핑 테이블 (P1-1 보강)

EmployeeSuspendedEvent의 `state.status="ON_LEAVE"` 등 employee enum 문자열을 UserAccount status로 변환하는 표:

| employee `state.status` | UserAccount 전이 | 호출 메서드 |
|---|---|---|
| `ACTIVE` (Hired 후) | (none) → ACTIVE | `UserAccount.create(...)` (UserCreated 발행) |
| `ACTIVE` (Resumed 후) | SUSPENDED → ACTIVE | `UserAccount.reactivate()` |
| `ON_LEAVE` (Suspended) | ACTIVE → SUSPENDED | `UserAccount.suspend("EMPLOYEE_ON_LEAVE")` |
| `RESIGNED` (Resigned) | * → DEACTIVATED + 세션 강제 종료 | `UserAccount.deactivate("EMPLOYEE_RESIGNED")` + `RefreshToken.revokeAll()` + Redis jti 일괄 blacklist |
| `PRE_HIRED` | (무시) | EmployeeHiredEvent의 state.status=ACTIVE 일 때만 UserAccount 생성. PRE_HIRED 단계의 hired 이벤트는 발생하지 않음 (Employment.activate 후 발행) |

`enum.valueOf` 직접 호출 금지. `EmployeeStatusToUserAccountStatus` value-object의 `mapHiredEvent / mapSuspendedEvent / mapResumedEvent / mapResignedEvent` 4 메서드로 변환.

### 4. DomainEvent 11종 + 토픽 라우팅 분리 (P0-1 해결)

#### 11종 이벤트 — action+state 규약

| eventType | aggregateType | action.type | state.status | 발생 시점 |
|---|---|---|---|---|
| UserCreated | UserAccount | CREATE | ACTIVE | employee.hired 수신 후 |
| UserLocked | UserAccount | LOCK | LOCKED | 비밀번호 5회 실패 시 |
| UserUnlocked | UserAccount | UNLOCK | ACTIVE | 관리자 수동 / 15분 백오프 자동. `action.details.trigger ∈ {AUTO, MANUAL}` 필수 |
| UserSuspended | UserAccount | SUSPEND | SUSPENDED | employee.suspended 수신 후 |
| UserReactivated | UserAccount | REACTIVATE | ACTIVE | employee.resumed 수신 후 |
| UserDeactivated | UserAccount | DEACTIVATE | DEACTIVATED | employee.resigned 수신 후 |
| UserRoleAssigned | UserAccount | ASSIGN_ROLE | ACTIVE | 역할 할당 API |
| UserRoleRevoked | UserAccount | REVOKE_ROLE | ACTIVE | 역할 취소 API |
| UserPasswordChanged | UserAccount | CHANGE_PASSWORD | ACTIVE | 본인 변경 / 재설정 |
| UserTwoFactorEnrolled | UserAccount | ENROLL_2FA | ACTIVE | 2FA enroll |
| UserTwoFactorDisabled | UserAccount | DISABLE_2FA | ACTIVE | 2FA 비활성 |

각 이벤트는 `core.DomainEvent` 추상을 구현하고 envelope (`eventId/eventType/eventVersion=1/occurredAt/aggregateType=UserAccount/action/state`) 으로 직렬화됩니다. 11종 모두 `event.hr.auth.v1` 토픽으로 발행. 검증 토픽 JSON Schema는 `infrastructure/kafka/schemas/auth/` 11개 파일에 정의.

#### 토픽 라우팅 분리 (P0-1)

employee-service의 `KafkaDomainEventPublisher`가 토픽을 `@Value("\${hrplatform.kafka.topics.employee:event.hr.employee.v1}")` 단일 키로 하드코딩하므로, auth가 그대로 재사용하면 11종 auth 이벤트가 모두 employee 토픽으로 발행됩니다 (TPM 검수 P0-1).

**결정**: auth-service 전용 `AuthKafkaDomainEventPublisher` 신설. employee 코드 변경 0.

- 위치: `infrastructure/kafka/AuthKafkaDomainEventPublisher.kt`
- 토픽 키: `@Value("\${hrplatform.kafka.topics.auth:event.hr.auth.v1}")`
- 인터페이스: `core.DomainEventPublisher` (employee와 동일 추상)
- aggregateType 검증: `require(event.aggregateType == "UserAccount")` — 잘못된 도메인 이벤트 발행 차단
- envelope 직렬화는 employee Publisher와 동일 코드 패턴 (Jackson ObjectMapper)

대안 (core 모듈로 aggregateType → topic 매핑 빈 추출)은 employee 모듈 수정이 동반되어 "변경 없음" 전제를 깨므로 미채택.

#### Kafka 인프라 신설 — Consumer (P1-2 해결)

employee 모듈의 `KafkaConfig`는 Producer만 정의합니다. auth는 처음으로 Consumer를 도입하므로 다음 인프라를 함께 신설:

| 컴포넌트 | 위치 | 책임 |
|---|---|---|
| `AuthKafkaConfig` | `infrastructure/kafka/AuthKafkaConfig.kt` | ProducerFactory + ConsumerFactory + ConcurrentKafkaListenerContainerFactory + DefaultErrorHandler(재시도 3회 + 백오프) + DeadLetterPublishingRecoverer |
| Consumer group-id | `application.yml` `hrplatform.kafka.consumer-groups.employee-sync: auth-service.employee.v1` (TPM Q5 권장) | employee 토픽 구독 그룹 명명 |
| DLQ 토픽 | `event.hr.auth.v1.dlq` (auth 발행 실패) + `event.hr.employee.v1.dlq` (consume 실패 시 재발행) | retention 30d |
| Idempotency | Redis SETNX `auth:idem:employee:{eventId}` TTL 7일 | 중복 eventId 1회만 처리 (TPM 권고 단일 선택) |

### 5. employee 이벤트 구독 정책

#### 구독 4종 이벤트

`event.hr.employee.v1` 토픽에서 다음 4 eventType만 처리. 나머지(promoted/transferred/salary_changed/department.changed/department.head_changed)는 라우팅 차단 후 무시 (별도 metric으로 카운트):

| eventType | 처리 메서드 | UserAccount 영향 |
|---|---|---|
| EmployeeHired | `UserAccountSyncUseCase.onHired(event)` | (none) → ACTIVE. 기본 role=EMPLOYEE 부여 |
| EmployeeSuspended | `UserAccountSyncUseCase.onSuspended(event)` | ACTIVE → SUSPENDED |
| EmployeeResumed | `UserAccountSyncUseCase.onResumed(event)` | SUSPENDED → ACTIVE |
| EmployeeResigned | `UserAccountSyncUseCase.onResigned(event)` | * → DEACTIVATED + 세션 강제 종료 (X1 시나리오) |

#### X1 시나리오 (resigned) 상세

`EmployeeResigned` 수신 시 한 트랜잭션에서 다음을 모두 수행:

1. `UserAccount.deactivate("EMPLOYEE_RESIGNED")` — 상태 전이 + DomainEvent 적재
2. `RefreshToken.revokeAll(userAccountId)` — 해당 사용자의 모든 RefreshToken `revokedAt = now`
3. Redis jti 일괄 blacklist — `auth:jti:blacklist:{jti}` SET TTL=access 토큰 남은 만료
4. `UserDeactivated` 이벤트 발행 (`event.hr.auth.v1` 토픽)

검증: `AuthDomainService.authenticate`가 DEACTIVATED 상태를 거부하므로, 동시에 진행 중인 access 토큰은 다음 토큰 검증 시점에 차단되고, refresh도 revoked로 거부됩니다.

### 6. ApiToken은 JWT와 다른 형태

외부 API용 ApiToken은 JWT와 형태·검증 경로가 다릅니다 (PRD §9.1 §11.1 보강 5개 API 중 2개).

| 항목 | JWT (사용자용) | ApiToken (외부 API용) |
|---|---|---|
| 발급 시점 | 로그인 (모든 사용자) | 관리자가 명시 발급 (HR_MANAGER+) |
| 형태 | JWS 토큰 (HS256) | random 32바이트 → base64url + prefix `hrp_` |
| DB 저장 | RefreshToken만 hash (SHA-256) | ApiToken Entity에 SHA-256 hash + scopes (JSON) + expiresAt + lastUsedAt |
| 노출 | response body | 발급 시 1회만 plain 노출, 이후 DB에 hash만 |
| 검증 | JwtVerifier (서명 검증, in-memory) | DB 조회 (SHA-256 hash 매칭) + scopes 검증 |
| 만료 | access 30분 / refresh 14일 | 발급 시 ttl 지정 (기본 90일) |
| 폐기 | logout API + DB revokedAt | DELETE API + DB revokedAt |
| 권한 모델 | role 기반 (UserAccountRole) | scopes 기반 (JSON 배열, 예: `["employee:read", "department:read"]`) |

검증 필터는 `JwtAuthenticationFilter` 내부에서 Bearer 토큰 prefix를 보고 분기:
- `Bearer hrp_*` → ApiToken 검증 경로 (DB 조회)
- `Bearer eyJ*` → JWT 검증 경로 (in-memory)

### 7. AuditorAware 빈 분리 (P1-3 해결)

employee-service의 `SpringAuditorAware`(`@Component("auditorAware")`, `com.hrplatform.employee.infrastructure.audit` 패키지)는 auth가 모듈 의존을 안 하므로 재사용 불가합니다.

**결정**: auth-service 내부에 동일 stub `SpringAuditorAware`를 복제 (방안 A). core 모듈 이동은 employee 코드 변동 동반이므로 미채택.

- 위치: `auth-service/src/main/kotlin/com/hrplatform/auth/infrastructure/audit/SpringAuditorAware.kt`
- MVP: SecurityContext 미연동 시 `Optional.of(0L)` 시스템 사용자
- AT-SEC 완료 후: `SecurityContextHolder`에서 `AuthPrincipal.userAccountId` 추출

### 8. JtiBlacklist 인터페이스 분리 (검수 잠재 위험 1건 보강)

`AuthDomainService`가 Redis를 직접 호출하면 "domain은 어느 것도 import하지 않는다" 원칙 위반. 다음과 같이 분리:

- `domain/token/JtiBlacklist.kt` interface — `add(jti, ttl)`, `contains(jti): Boolean`, `addAll(jtis, ttl)`
- `infrastructure/security/RedisJtiBlacklist.kt` — Redis 구현체
- AuthDomainService 생성자 주입은 interface만

### 9. 비밀번호 정책

| 항목 | 값 |
|---|---|
| 최소 길이 | 10자 (PRD §10.2) |
| 필수 조합 | 영문 대/소문자 + 숫자 + 특수문자 1개 이상씩 (regex 4종 매칭) |
| 금지 | 직전 비밀번호 3종과 동일 금지 (해시 비교) — MVP는 직전 1종, Phase 2에서 3종 |
| 잠금 | 5회 연속 실패 → 15분 잠금 (`failedLoginAttempts` 컬럼 + `lockedUntil` ZonedDateTime) |
| 잠금 알림 | UserLocked 이벤트 → notification-service가 메일 발송 (MVP는 LogStub) |
| 재설정 | 1회용 토큰(SHA-256 hash 저장, 30분 만료) 메일 발송 → 토큰 검증 + 새 비밀번호 정책 검증 |

`PasswordPolicy` value object가 검증 책임을 캡슐화. 위반 시 `WeakPasswordException` (HTTP 422).

## Alternatives Considered

### A. core 모듈에 `aggregateType → topic` 매핑 빈 도입

- 설명: `DomainEventPublisher` 계약에 `topicFor(aggregateType): String` 추가, employee/auth 양쪽이 공유.
- 미채택 사유: employee-service의 `KafkaDomainEventPublisher.kt` 수정 동반 → TPM 분석의 "영향 서비스: employee 변경 없음" 전제 위배. employee M1 PR이 main에 머지된 상태이므로 안정성 우선. 추후 도메인이 5개 이상 늘 때 (Phase 1.5) 리팩토링으로 도입.

### B. 모든 employee 이벤트를 auth에서 처리 (9종 전체 구독)

- 설명: `EmployeeTransferred`/`EmployeePromoted` 등도 구독해 UserAccount의 displayName/role을 자동 갱신.
- 미채택 사유: auth는 인증·인가만 책임 (ADR-002 §6.7 경계 원칙). 사용자 표시 정보는 BFF에서 employee API 조회로 해결. 4종 이벤트만 구독해 의존 최소화.

### C. Redis 없이 DB로 jti blacklist 관리

- 설명: `revoked_jtis` 테이블에 jti + expiresAt 저장, 만료된 jti는 배치로 삭제.
- 미채택 사유: 토큰 검증마다 DB I/O 발생 → PRD §10.2 "토큰 검증 < 10ms" 위반. Redis는 ms 미만 응답.

### D. Spring Authorization Server 활용 (OAuth2 표준)

- 설명: Spring 공식 OAuth2 Authorization Server (1.4.x)로 ID/refresh 토큰 모두 표준 발급.
- 미채택 사유: MVP는 사내 첫 사용자(HR_MANAGER + 직원) 한정으로 OAuth2 client/scope 모델까지 필요 없음. 학습·디버깅 비용이 jjwt 직접 사용보다 크고, 외부 OAuth client 통합은 Phase 1.5(회사 SSO) 이후 검토.

### E. Modular Monolith — auth를 employee 내부 패키지로

- 설명: 별도 모듈이 아닌 employee-service 안에 `com.hrplatform.employee.auth` 패키지 추가.
- 미채택 사유: ADR-001이 서비스 1개 = Spring Boot Application 1개 결정. PRD §5.1 "7개 도메인 분리"와 정렬. 토픽 분리·DB 스키마 분리·배포 단위 분리가 보안 회사 표준 (auth는 별도 deploy + 별도 DB 권한).

## Rationale

1. **모듈 의존 0 (employee 무참조)** 가 auth 독립 배포 + 잠재 employee 장애 격리. AesGcmStringConverter·SpringAuditorAware 복제 비용이 상호의존 비용보다 작음.
2. **AuthKafkaDomainEventPublisher 분리**가 P0-1 해결의 최소 변경. employee 코드 변경 0건.
3. **상태 머신 4상태 + 매핑 테이블**이 employee 이벤트 enum 변환 실수(런타임 valueOf 실패)를 컴파일 타임에 차단.
4. **11종 이벤트 + action+state 규약**이 core의 envelope 패턴과 정확히 정렬. notification-service · audit pipeline이 단일 스키마로 처리 가능.
5. **ApiToken vs JWT 분리**가 외부 API client (Phase 2 외부 공개)에 대한 권한 모델(scopes)을 미리 확보.
6. **Lazy 자동 unlock**이 별도 스케줄러 인프라 비용 0 + 동시성 문제(여러 노드의 스케줄러 중복 실행) 회피.
7. **JtiBlacklist interface 분리**가 be-code-convention의 "domain은 어느 것도 import하지 않는다" 원칙을 정적으로 강제.

## Consequences

### Positive

- auth-service 단독 배포 + 단독 롤백 가능 (employee와 분리).
- 11종 DomainEvent는 envelope 패턴 그대로라서 notification/audit pipeline에서 코드 변경 없이 즉시 처리.
- `event.hr.auth.v1` 토픽 분리로 auth 이벤트가 employee 컨슈머 그룹에 새지 않음.
- bcrypt(cost 12) + lazy unlock + DEACTIVATED 종착 패턴으로 보안 사고 대응(계정 영구 비활성) 단순.
- ApiToken scopes 모델이 Phase 2 외부 API 권한 정책에 그대로 확장 가능.

### Negative

- AesGcmStringConverter / SpringAuditorAware 코드 중복 (employee + auth 양쪽 동일 파일). 두 도메인이 모두 사용한다는 의미가 명확해지면 (Phase 1.5) core 이동.
- Redis 의존 추가 — 로컬 개발은 docker-compose로 부담 적으나, prod ElastiCache 비용 발생.
- 11종 JSON Schema 유지보수 부담 — eventType 추가 시 schema 추가 + Worker 라우팅 + 테스트 3곳 동시 갱신 필요. ADR-005 (Phase 1.5)에서 schema-registry 도입 검토.

### Risks

- 비밀번호 정책(영숫특 10자) 사용자 불만 가능성 — PRD가 명시했으므로 BE는 그대로 적용, UX 완화는 FE 별도 트랙.
- LogNotificationGateway는 MVP stub. 실제 메일 발송 (AC2 "5회 실패 시 알림") 검증은 LogStub 캡처로 만족 — 운영 메일 인프라는 Phase 1.5 (별도 ADR).
- Idempotency 키 (Redis SETNX) 가 Redis 장애 시 동작 불가 — fallback 없음. Phase 2에서 DB 기반 백업 검토.

## Verification

ADR이 코드에 반영됐는지:

```bash
# 1. 모듈 의존 검증 — employee 0
grep -n ":employee-service\|com.hrplatform.employee" \
  hr-platform/auth-service/build.gradle.kts \
  hr-platform/auth-service/src/main/kotlin -r --include="*.kt" \
  | wc -l   # → 0

# 2. 4-layer 디렉토리
ls hr-platform/auth-service/src/main/kotlin/com/hrplatform/auth/
# → presentation, application, domain, infrastructure 4개

# 3. 7 Entity + 4 DomainService 존재
find hr-platform/auth-service/src/main/kotlin/com/hrplatform/auth/domain \
  -name "UserAccount.kt" -o -name "Role.kt" -o -name "UserAccountRole.kt" \
  -o -name "RefreshToken.kt" -o -name "LoginAttempt.kt" \
  -o -name "TwoFactorBackupCode.kt" -o -name "ApiToken.kt" | wc -l   # → 7

grep -l "DomainService" \
  hr-platform/auth-service/src/main/kotlin/com/hrplatform/auth/domain/service/*.kt | wc -l   # → 4

# 4. AuthKafkaDomainEventPublisher 신설 (employee Publisher와 별개)
test -f hr-platform/auth-service/src/main/kotlin/com/hrplatform/auth/infrastructure/kafka/AuthKafkaDomainEventPublisher.kt
grep "event.hr.auth.v1" hr-platform/auth-service/src/main/kotlin/com/hrplatform/auth/infrastructure/kafka/AuthKafkaDomainEventPublisher.kt | wc -l   # ≥ 1

# 5. 11 JSON Schema
ls hr-platform/infrastructure/kafka/schemas/auth/*.json | wc -l   # → 11

# 6. UserAccountStatus.canTransitTo 캡슐화
grep "canTransitTo" \
  hr-platform/auth-service/src/main/kotlin/com/hrplatform/auth/domain/account/UserAccountStatus.kt | wc -l   # ≥ 1

# 7. JtiBlacklist interface(domain) + RedisJtiBlacklist 구현(infrastructure)
test -f hr-platform/auth-service/src/main/kotlin/com/hrplatform/auth/domain/token/JtiBlacklist.kt
test -f hr-platform/auth-service/src/main/kotlin/com/hrplatform/auth/infrastructure/security/RedisJtiBlacklist.kt

# 8. SpringAuditorAware 복제
test -f hr-platform/auth-service/src/main/kotlin/com/hrplatform/auth/infrastructure/audit/SpringAuditorAware.kt

# 9. Port 인터페이스 0건
grep -rn "interface .*Port" hr-platform/auth-service/src/main --include="*.kt" | wc -l   # → 0

# 10. typealias 호환 layer 0건
find hr-platform/auth-service/src -name "*TypeAliases.kt" -o -name "*Compat.kt" -o -name "*Aliases.kt" | wc -l   # → 0

# 11. @Query 0건
grep -rn "@Query" hr-platform/auth-service/src/main --include="*.kt" | wc -l   # → 0

# 12. !! 0건
grep -rn "!!" hr-platform/auth-service/src/main --include="*.kt" | wc -l   # → 0

# 13. LocalDateTime 0건
grep -rn "LocalDateTime" hr-platform/auth-service/src/main --include="*.kt" | wc -l   # → 0
```

13개 검증 모두 통과해야 auth-service M1 ADR-003 채택 완료로 인정.

## Document History

| 날짜 | 변경 내용 | 작성자 |
|---|---|---|
| 2026-05-18 | 초안 — TPM 검수 NEEDS_REVISION p0 1건 + p1 3건 모두 반영 (AuthKafkaDomainEventPublisher 분리 / employee status 매핑 / Consumer 인프라 / SpringAuditorAware 복제) + 잠재 위험 1건(JtiBlacklist 분리) | 메인 세션 |
