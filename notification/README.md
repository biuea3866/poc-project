# Ecommerce Notification System (Spring Boot)

통합 알림 시스템 패턴을 이커머스 도메인으로 구현한 예제 프로젝트.
Spring Boot 3.3 + Kotlin 1.9 + JPA (H2) 기반.

## 목적

알림 고도화 (V2)의 핵심 패턴을 이해하기 쉬운 이커머스 도메인으로 구현하여,
레거시 -> 신규 시스템 -> 하위호환 브릿지의 3단계 전환 과정을 보여준다.

## 기술 스택

| 영역 | 기술 |
|------|------|
| 언어 | Kotlin 1.9, Java 17 |
| 프레임워크 | Spring Boot 3.3 |
| ORM | Spring Data JPA (Hibernate) |
| DB | H2 (인메모리) |
| 빌드 | Gradle (Kotlin DSL) |
| 테스트 | Kotest 5.8 (BehaviorSpec), MockK 1.13 |

## 3단계 구조

### Phase 1: 레거시 (`legacy/`)
- 채널별 이벤트 클래스 폭발 (알림 1종 = 이벤트 N개 + 핸들러 N개)
- 유저별 채널 ON/OFF만 지원 (매장/상품 스코프 없음)
- 채널 추가 시 모든 UseCase 수정 필요

### Phase 2: 신규 시스템 (`domain/` + `application/`)
- 통합 이벤트 1건으로 모든 채널 발송
- 4계층 우선순위: Mandatory -> Store Policy -> User Rule -> System Default
- 3단계 스코프: GLOBAL -> STORE -> PRODUCT
- 벌크 배칭 (correlationId) + 멱등성 (idempotencyKey)

### Phase 3: 하위호환 브릿지 (`application/service/`)
- Feature Flag (DB 기반 SimpleRuntimeConfig)로 V2/레거시 분기
- `NotificationLegacyBridge.tryPublishV2()` -> true면 V2, false면 레거시 폴백
- 매장 단위 점진적 마이그레이션 가능

## 패키지 구조

```
src/main/kotlin/com/example/notification/
├── NotificationApplication.kt              # @SpringBootApplication
├── domain/
│   ├── enums/          # NotificationCategory, TriggerType, Channel, Priority, ScopeType, Frequency, LogStatus
│   ├── model/          # NotificationEvent, Payload, Rule, EffectiveRule, Recipient, Log, RenderedMessage
│   └── channel/        # NotificationChannelDispatcher (interface), Email/Push/Sms/InAppDispatcher (@Component)
├── application/
│   ├── port/           # NotificationRuleReader, LogWriter, RecipientReader (interface)
│   └── service/        # Orchestrator, Dispatcher, RuleResolver, RecipientResolver, MessageRenderer,
│                       # LegacyBridge, FeatureFlagService, OrderNotificationUseCaseV2,
│                       # NotificationRuleInitializer, MigrateLegacyConfigJob
├── infrastructure/
│   ├── entity/         # NotificationRuleEntity, NotificationLogEntity, InAppAlertEntity,
│   │                   # SimpleRuntimeConfigEntity, LegacyNotificationConfigEntity (@Entity)
│   └── repository/     # JPA Repositories (Spring Data)
└── legacy/             # LegacyEvents, LegacyHandlers (@Component), LegacyNotificationConfig,
                        # LegacyOrderNotificationUseCase (@Component)
```

## 실행 방법

```bash
# 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun

# 테스트 실행
./gradlew test
```

H2 콘솔: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:notification`)

## 테스트

Kotest BehaviorSpec (Given/When/Then) + MockK 단위 테스트:

- `NotificationEventTest.kt` - isCorrelated, idempotencyKey 생성
- `NotificationDispatcherTest.kt` - 단건/벌크/멱등성/실패
- `NotificationOrchestratorTest.kt` - 수신자 x 채널 조합, disabled 스킵
- `LegacyBridgeTest.kt` - Flag ON/OFF 분기
- `NotificationRuleInitializerTest.kt` - 디폴트 규칙 초기화 (신규/기존 유저, 오너/비오너)
- `MigrateLegacyConfigJobTest.kt` - 레거시 설정 이관 (정상/스킵/dryRun)

## JPA 엔티티

| 엔티티 | 테이블 | 설명 |
|--------|--------|------|
| `NotificationRuleEntity` | `notification_rules` | 알림 규칙 (Store Policy + User Rule) |
| `NotificationLogEntity` | `notification_logs` | 발송 로그 (멱등성 키 포함) |
| `InAppAlertEntity` | `in_app_alerts` | 인앱 알림 (IN_APP 채널) |
| `SimpleRuntimeConfigEntity` | `simple_runtime_config` | Feature Flag (런타임 ON/OFF) |
| `LegacyNotificationConfigEntity` | `legacy_notification_configs` | 레거시 알림 설정 |

## 디폴트 규칙 초기화 (NotificationRuleInitializer)

유저가 최초 진입 시 디폴트 알림 규칙을 자동 생성한다.

- `initializeIfAbsent(userId, storeId, isStoreOwner)`: GLOBAL 스코프 규칙 생성
  - 스토어 오너: 모든 카테고리/채널 활성화
  - 비오너: 시스템 디폴트 (카테고리별 기본 채널) 적용
  - 이미 규칙이 있으면 스킵
- `initializeProcessRules(userId, storeId, productId, isStoreOwner)`: PRODUCT 스코프 규칙 생성
  - 스토어 오너: 모든 상품 알림 활성화
  - 비오너: 비활성화 (필요 시 수동 활성화)

## 레거시 마이그레이션 (MigrateLegacyConfigJob)

레거시 `LegacyNotificationConfig` (채널 ON/OFF만) -> `notification_rules` 이관 배치.

- 시스템 디폴트와 다른 설정만 이관 (동일하면 스킵)
- dryRun 모드 지원: 실제 저장 없이 이관 대상 확인
- `MigrationResult`로 이관 결과 (총 건수, 이관/스킵 건수, 생성된 규칙) 반환

## Feature Flag 패턴

`FeatureFlagService`는 `simple_runtime_config` 테이블을 조회하여 Feature Flag 상태를 판단한다.

- key: `{TRIGGER_TYPE}:{storeId}` (매장별) 또는 `{TRIGGER_TYPE}` (글로벌)
- value: `"true"` / `"false"`

## 원본 매핑 테이블

| 이커머스 (이 프로젝트) | 원본 시스템 |
|----------------------|------------|
| **도메인** | |
| NotificationCategory (ORDER, SHIPMENT, PAYMENT, REVIEW, SYSTEM) | NotificationCategory (카테고리별 분류) |
| NotificationTriggerType (ORDER_PLACED, SHIPMENT_STARTED, ...) | NotificationTriggerType (트리거별 분류) |
| NotificationChannel (EMAIL, PUSH, SMS, IN_APP) | NotificationChannel (채널별 분류) |
| ScopeType (GLOBAL, STORE, PRODUCT) | ScopeType (스코프별 분류) |
| NotificationEvent.storeId | NotificationEvent.workspaceId |
| NotificationEvent.productId | NotificationEvent.openingId |
| NotificationEvent.orderId | NotificationEvent.applicantId |
| NotificationRecipient.isStoreOwner | NotificationRecipient.isManager |
| **서비스** | |
| NotificationOrchestrator | NotificationOrchestrator |
| NotificationDispatcher | NotificationDispatcher |
| NotificationRuleResolver | NotificationRuleResolver |
| NotificationRecipientResolver | NotificationRecipientResolver |
| NotificationMessageRenderer | NotificationMessageRenderer |
| NotificationLegacyBridge | NotificationLegacyBridge |
| OrderNotificationUseCaseV2 | V2 UseCase |
| FeatureFlagService | FeatureFlagService |
| **레거시** | |
| LegacyEvents (OrderPlacedEmailEvent, ...) | 채널별 이벤트 클래스 |
| LegacyHandlers | 채널별 핸들러 클래스 |
| LegacyNotificationConfig | alert_configs 테이블 |
| LegacyOrderNotificationUseCase | 레거시 UseCase |
