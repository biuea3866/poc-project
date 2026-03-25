# [Ticket #23] 기존 API 하위호환 어댑터

## 개요
- TDD 참조: tdd.md 섹션 5.1 (영향 범위), 2.1 (현재 아키텍처), 8.4 (설계 결정 - 어댑터 패턴)
- 선행 티켓: #17, #18, #19
- 크기: M

## 작업 내용

### 변경 사항

#### 1. 레거시 엔드포인트 매핑
기존 payment-server의 API를 신규 도메인 서비스로 내부 라우팅. FE/다른 서비스가 기존 엔드포인트를 호출해도 동작하도록 보장.

| 기존 엔드포인트 | 내부 매핑 | 설명 |
|----------------|----------|------|
| `POST /api/plan/upgrade` | OrderService.createOrder(UPGRADE) → PaymentService.processPayment() → SubscriptionService.upgrade() | 플랜 업그레이드 (주문+결제+구독 변경 한 번에) |
| `POST /api/plan/downgrade` | SubscriptionService.scheduleDowngrade() | 다운그레이드 예약 |
| `POST /api/plan/cancel` | SubscriptionService.cancel() | 구독 해지 |
| `GET /api/plan/current` | SubscriptionService.getCurrentSubscription() | 현재 플랜 조회 |
| `POST /api/plan/subscribe` | OrderService.createOrder(NEW) → PaymentService → SubscriptionService | 신규 구독 |
| `POST /api/message-point/charge` | CreditService.charge() (내부 Order 생성) | SMS 포인트 충전 |
| `GET /api/message-point/balance` | CreditService.getBalance(SMS) | SMS 포인트 잔액 |
| `GET /api/message-point/logs` | CreditService.getTransactions(SMS) | SMS 사용 이력 |
| `POST /api/card/register` | BillingKeyService.register() | 카드(빌링키) 등록 |
| `DELETE /api/card/delete` | BillingKeyService.delete() | 카드(빌링키) 삭제 |
| `GET /api/card/info` | BillingKeyService.getPrimary() | 카드 정보 조회 |

#### 2. LegacyPlanController (어댑터)
- 기존 `/api/plan/*` 엔드포인트의 Request/Response 형식 유지
- 내부적으로 신규 서비스 호출 후 기존 응답 스키마로 변환
- 기존 PlanService → 신규 SubscriptionService + OrderService + PaymentService 위임

#### 3. LegacyMessagePointController (어댑터)
- 기존 `/api/message-point/*` 엔드포인트 유지
- 내부적으로 CreditService 호출
- 기존 MessagePointService → 신규 CreditService 위임

#### 4. LegacyCardController (어댑터)
- 기존 `/api/card/*` 엔드포인트 유지
- 내부적으로 BillingKeyService 호출

#### 5. Deprecation 헤더 추가
- 모든 레거시 엔드포인트에 `Deprecation` HTTP 헤더 추가
  - `Deprecation: true`
  - `Sunset: 2026-08-31` (3개월 유예)
  - `Link: </api/v1/orders>; rel="successor-version"` (신규 API 링크)
- 레거시 API 호출 시 로그 기록 (호출 빈도 모니터링용)

#### 6. 응답 스키마 변환
- 기존 응답 DTO는 그대로 유지 (FE 깨지지 않도록)
- 내부 도메인 모델 → 기존 응답 DTO 변환 매퍼 구현
  - SubscriptionToLegacyPlanMapper: Subscription → 기존 PlanOnGroup 응답 형식
  - CreditToLegacyMessagePointMapper: CreditBalance/CreditLedger → 기존 MessagePoint 응답 형식

#### 7. 점진적 마이그레이션 계획
- Phase 1 (이 티켓): 어댑터 구현 + Deprecation 헤더
- Phase 2 (별도): FE에서 신규 API(/api/v1/*)로 전환
- Phase 3 (별도): 레거시 엔드포인트 제거

### 수정 파일 목록
| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting_payment-server | presentation | presentation/legacy/LegacyPlanController.kt | 신규 |
| greeting_payment-server | presentation | presentation/legacy/LegacyMessagePointController.kt | 신규 |
| greeting_payment-server | presentation | presentation/legacy/LegacyCardController.kt | 신규 |
| greeting_payment-server | presentation | presentation/legacy/mapper/SubscriptionToLegacyPlanMapper.kt | 신규 |
| greeting_payment-server | presentation | presentation/legacy/mapper/CreditToLegacyMessagePointMapper.kt | 신규 |
| greeting_payment-server | presentation | presentation/legacy/dto/ (기존 응답 DTO 재사용) | 수정 (필요 시) |
| greeting_payment-server | presentation | presentation/config/DeprecationHeaderFilter.kt | 신규 |
| greeting_payment-server | application | application/PlanService.kt | 수정 (기존 로직 제거, 신규 서비스 위임으로 전환) |
| greeting_payment-server | application | application/MessagePointService.kt | 수정 (기존 로직 제거, CreditService 위임) |
| greeting_payment-server | infrastructure | infrastructure/config/WebMvcConfig.kt | 수정 (DeprecationHeaderFilter 등록) |

## 테스트 케이스

### 정상 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T23-01 | 기존 플랜 업그레이드 API | Basic 구독 중, 빌링키 존재 | POST /api/plan/upgrade (기존 형식) | 200 OK, 기존 응답 형식으로 반환, 내부적으로 Order+Payment+Subscription 처리 |
| T23-02 | 기존 플랜 조회 API | Standard 구독 중 | GET /api/plan/current (기존 형식) | 200 OK, 기존 PlanOnGroup 응답 형식 |
| T23-03 | 기존 SMS 충전 API | SMS_PACK_1000 충전 | POST /api/message-point/charge (기존 형식) | 200 OK, 기존 응답 형식, 내부적으로 Order+Payment+Credit 처리 |
| T23-04 | 기존 잔액 조회 API | SMS balance=500 | GET /api/message-point/balance | 200 OK, 기존 응답 형식 |
| T23-05 | Deprecation 헤더 포함 | 레거시 엔드포인트 호출 | GET /api/plan/current | 응답에 Deprecation: true, Sunset, Link 헤더 포함 |
| T23-06 | 기존 카드 정보 조회 | 빌링키 존재 | GET /api/card/info | 200 OK, 기존 CardInfoOnGroup 응답 형식 |

### 예외/엣지 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T23-E01 | 기존 요청 형식 호환성 | 기존 FE가 보내는 정확한 JSON 형식 | POST /api/plan/upgrade (기존 필드명) | 정상 파싱 및 처리 (필드명 매핑) |
| T23-E02 | 신규 에러를 기존 에러 형식으로 | ProductNotFoundException 발생 | POST /api/plan/upgrade (잘못된 플랜) | 기존 에러 응답 형식으로 반환 |
| T23-E03 | 신규 API와 동시 호출 | 기존 /api/plan/upgrade와 신규 /api/v1/subscriptions/upgrade 동시 | 동시 호출 | 둘 다 정상 처리 (같은 도메인 서비스 호출) |
| T23-E04 | 기존 API에 없던 에러 케이스 | Optimistic Lock 충돌 | POST /api/plan/upgrade | 기존 에러 형식으로 500 반환 + 재시도 안내 |

## 기대 결과 (AC)
- [ ] 기존 /api/plan/*, /api/message-point/*, /api/card/* 엔드포인트가 기존 Request/Response 형식 그대로 동작한다
- [ ] 내부적으로는 신규 도메인 서비스(OrderService, PaymentService, SubscriptionService, CreditService)를 호출한다
- [ ] 모든 레거시 엔드포인트 응답에 Deprecation, Sunset, Link 헤더가 포함된다
- [ ] 레거시 API 호출 빈도가 로그로 기록되어 마이그레이션 진행 상황을 모니터링할 수 있다
- [ ] 기존 PlanService, MessagePointService의 직접 로직이 신규 서비스 위임으로 전환된다
- [ ] FE 변경 없이 기존 기능이 동일하게 동작한다
