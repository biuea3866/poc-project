# [Ticket #24] greeting-new-back 연동 변경

## 개요
- TDD 참조: tdd.md 섹션 5.1 (greeting-new-back 수정), 5.2 (Kafka 이벤트 변경 — cdc.greeting.PlanOnGroup 폐기 예정)
- 선행 티켓: #21
- 크기: M

## 작업 내용

### 변경 사항

#### 1. 현재 상태 (AS-IS)
- greeting-new-back의 `PlanOnWorkspace` 엔티티는 CDC 이벤트(`cdc.greeting.PlanOnGroup`)를 통해 데이터 동기화
- payment-server의 PlanOnGroup 테이블 변경 → Debezium CDC → Kafka → greeting-new-back Consumer → PlanOnWorkspace 업데이트
- 문제: 신규 시스템에서 PlanOnGroup이 Subscription으로 대체되면 CDC 이벤트 구조가 변경됨

#### 2. 신규 Kafka Consumer 구현
- `subscription.changed.v1` 토픽을 소비하는 Consumer 생성
- SubscriptionChangedEvent를 수신하여 PlanOnWorkspace 업데이트
- Consumer Group: `greeting-new-back-subscription-consumer`

#### 3. 이벤트 → PlanOnWorkspace 매핑
```
SubscriptionChangedEvent → PlanOnWorkspace 매핑:
- workspaceId → PlanOnWorkspace.workspaceId
- planCode → PlanOnWorkspace.planType (PLAN_BASIC → BASIC, PLAN_STANDARD → STANDARD, PLAN_FREE → FREE)
- status → PlanOnWorkspace.status (ACTIVE → ACTIVE, EXPIRED → EXPIRED 등)
- periodStart → PlanOnWorkspace.startDate
- periodEnd → PlanOnWorkspace.endDate
- changeType → 처리 분기:
  - NEW/RENEWAL/UPGRADE: PlanOnWorkspace 생성 또는 업데이트
  - DOWNGRADE: PlanOnWorkspace 업데이트 (다음 기간 시작 시)
  - CANCEL: PlanOnWorkspace.autoRenew = false
  - EXPIRE: PlanOnWorkspace.status = EXPIRED
```

#### 4. Dual-Read 기간 지원 (마이그레이션 안전장치)
- **Phase 1 (이 티켓)**: 신규 Consumer 추가, 기존 CDC Consumer도 유지
  - 신규 Consumer가 최신 데이터 반영
  - 기존 CDC Consumer도 계속 동작 (중복 업데이트 허용, 멱등성 보장)
  - Feature flag: `payment.subscription-event.enabled=true` (신규 Consumer 활성화)
  - Feature flag: `payment.cdc-plan.enabled=true` (기존 CDC Consumer 유지)

- **Phase 2 (별도 작업)**: CDC Consumer 비활성화
  - `payment.cdc-plan.enabled=false`로 기존 CDC 비활성화
  - 모니터링 후 문제 없으면 코드 제거

- **Phase 3 (별도 작업)**: CDC Consumer 코드 완전 제거

#### 5. 멱등성 보장
- PlanOnWorkspace 업데이트 시 eventTimestamp 기반 순서 보장
  - 이미 더 최신 이벤트가 반영된 경우 skip
  - `lastEventTimestamp` 필드 추가하여 비교
- 동일 이벤트 재수신 시 동일 결과 (upsert)

#### 6. PlanOnWorkspace 엔티티 수정
- `lastEventTimestamp: Instant?` 필드 추가 (이벤트 순서 보장용)
- `lastEventSource: String?` 필드 추가 ("CDC" 또는 "SUBSCRIPTION_EVENT" — 디버깅용)

### 수정 파일 목록
| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting-new-back | domain | domain/plan/PlanOnWorkspace.kt | 수정 (lastEventTimestamp, lastEventSource 필드 추가) |
| greeting-new-back | infrastructure | infrastructure/kafka/consumer/SubscriptionChangedConsumer.kt | 신규 |
| greeting-new-back | infrastructure | infrastructure/kafka/dto/SubscriptionChangedEvent.kt | 신규 |
| greeting-new-back | application | application/plan/PlanSyncService.kt | 신규 (이벤트 → PlanOnWorkspace 변환 로직) |
| greeting-new-back | infrastructure | infrastructure/kafka/consumer/CdcPlanOnGroupConsumer.kt | 수정 (feature flag 조건 추가) |
| greeting-new-back | infrastructure | infrastructure/config/KafkaConsumerConfig.kt | 수정 (신규 Consumer Group 설정) |
| greeting-new-back | resources | application.yml | 수정 (feature flag 설정 추가) |
| greeting-new-back | infrastructure | infrastructure/repository/PlanOnWorkspaceRepository.kt | 수정 (upsert 쿼리 추가) |
| greeting-db-schema | - | migrations/V{next}__add_plan_event_tracking_columns.sql | 신규 (lastEventTimestamp, lastEventSource 컬럼 추가) |

## 테스트 케이스

### 정상 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T24-01 | 신규 구독 이벤트 수신 | workspace에 PlanOnWorkspace 없음 | subscription.changed.v1 (changeType=NEW, planCode=PLAN_STANDARD) | PlanOnWorkspace 생성 (planType=STANDARD, status=ACTIVE) |
| T24-02 | 구독 갱신 이벤트 수신 | 기존 PlanOnWorkspace(STANDARD, ACTIVE) | subscription.changed.v1 (changeType=RENEWAL) | PlanOnWorkspace 기간(startDate, endDate) 갱신 |
| T24-03 | 업그레이드 이벤트 수신 | PlanOnWorkspace(BASIC, ACTIVE) | subscription.changed.v1 (changeType=UPGRADE, planCode=PLAN_STANDARD) | PlanOnWorkspace planType=STANDARD로 변경 |
| T24-04 | 해지 이벤트 수신 | PlanOnWorkspace(STANDARD, ACTIVE, autoRenew=true) | subscription.changed.v1 (changeType=CANCEL) | PlanOnWorkspace autoRenew=false |
| T24-05 | 만료 이벤트 수신 | PlanOnWorkspace(STANDARD, ACTIVE) | subscription.changed.v1 (changeType=EXPIRE) | PlanOnWorkspace status=EXPIRED |
| T24-06 | Dual-Read: 양쪽 Consumer 동시 동작 | CDC + 신규 Consumer 모두 활성화 | CDC 이벤트 + subscription.changed.v1 동시 수신 | 최신 timestamp 기준으로 반영, 데이터 정합성 유지 |
| T24-07 | Feature flag로 신규 Consumer 비활성화 | payment.subscription-event.enabled=false | subscription.changed.v1 발행 | Consumer 미동작, 기존 CDC로만 동기화 |

### 예외/엣지 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T24-E01 | 순서 역전 이벤트 수신 | lastEventTimestamp=T2 | subscription.changed.v1 (timestamp=T1, T2보다 이전) | 무시 (skip), 로그 기록 |
| T24-E02 | 중복 이벤트 수신 | 동일 eventId 이미 처리됨 | 동일 이벤트 재수신 | 멱등하게 처리 (동일 결과) |
| T24-E03 | 알 수 없는 planCode | planCode=PLAN_UNKNOWN | subscription.changed.v1 수신 | 에러 로그 기록, DLQ 전송, 다음 이벤트 계속 처리 |
| T24-E04 | 이벤트 역직렬화 실패 | 잘못된 JSON 형식 | subscription.changed.v1 수신 | 에러 로그 + DLQ, Consumer offset 커밋 (블로킹 방지) |
| T24-E05 | DB 저장 실패 | PlanOnWorkspace UPDATE 중 DB 장애 | 이벤트 처리 중 | 재시도 (Spring Retry), 최종 실패 시 DLQ |

## 기대 결과 (AC)
- [ ] greeting-new-back이 subscription.changed.v1 토픽을 소비하여 PlanOnWorkspace를 업데이트한다
- [ ] 모든 changeType(NEW, RENEWAL, UPGRADE, DOWNGRADE, CANCEL, EXPIRE)에 대해 올바른 매핑이 동작한다
- [ ] Dual-Read 기간 동안 기존 CDC Consumer와 신규 Consumer가 동시에 동작하며 데이터 정합성이 유지된다
- [ ] Feature flag로 각 Consumer를 독립적으로 활성화/비활성화할 수 있다
- [ ] lastEventTimestamp 기반으로 이벤트 순서가 보장되고, 순서 역전 이벤트는 무시된다
- [ ] 이벤트 처리 실패 시 DLQ로 전송되고, 다음 이벤트 처리가 블로킹되지 않는다
