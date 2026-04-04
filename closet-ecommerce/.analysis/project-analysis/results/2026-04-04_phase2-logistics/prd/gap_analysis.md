# 요구사항 Gap 분석

> PRD: Phase 2 상세 PRD (성장 단계) | 분석일: 2026-04-04
> 분석 소스: 기능요구사항, 비기능요구사항, 모호성, 기술실현성, 작업범위 (5개 에이전트 분석 종합)

---

## 1. 애매한 요구사항

5개 분석 결과에서 도출된 모호한 표현을 크로스 분석하여 우선순위별로 정리합니다.

| # | PRD 원문 | 애매한 점 | 해석 옵션 | 추천 |
|---|----------|----------|-----------|------|
| A-01 | "PAID -> SHIPPING 상태 변경" (US-501) | 기존 코드에 `SHIPPING` 상태 없음. 기존 `OrderStatus`는 `PAID -> PREPARING -> SHIPPED -> DELIVERED -> CONFIRMED` 흐름. **기능분석 + 기술실현성 + 모호성 3개 분석에서 공통 지적** | (A) PRD의 `SHIPPING`을 기존 `SHIPPED`에 매핑 / (B) `PREPARING`과 `SHIPPED` 사이 신규 상태 추가 / (C) `PAID -> PREPARING`(송장등록) -> `SHIPPED`(택배사 발송확인) 2단계 유지 | **(C) 기존 코드 2단계 유지 + PRD AC를 `PAID -> PREPARING -> SHIPPED`로 수정**. 기존 전이 규칙이 더 정교하며, 기술실현성 분석도 동일 결론 |
| A-02 | "택배사 API(스마트택배 등)를 통해" (US-502) | "등"이 범위를 모호하게 만듦. Mock 서버에는 CJ/로젠/롯데/우체국 4개 개별 API 구현되어 있으나 PRD는 "스마트택배"(통합 API)를 언급. **모호성 + 기술실현성 2개 분석에서 공통 지적** | (A) Mock 서버의 개별 택배사 API 직접 호출 / (B) Mock 서버에 통합 조회 API 추가 / (C) 스마트택배 외부 연동 | **(A) Mock 서버 개별 API 활용 + 배송 상태 매핑 어댑터 구현**. Phase 2는 학습 프로젝트이므로 기존 인프라 최대 활용. shipping-service 내에 CarrierAdapter 패턴으로 택배사별 상태를 PRD 상태로 변환 |
| A-03 | "자동으로 구매확정 - 매일 00:00" (US-503) + "배송 완료 후 7일 이내 반품" (US-504) | 7일 기산점이 시각(168시간)인지 일자(자정 기준)인지 불명확. **모호성 + 기능분석 2개에서 지적되었고, 상충 분석에서도 경계 충돌로 재확인됨** | (A) 7일 = 168시간 (시각 기준) / (B) 7일째 자정까지 (일자 기준) | **(A) 168시간(시각 기준) 채택**. `delivered_at + 168h`으로 통일하면 경계 충돌이 해소됨. 배치는 00:00 실행이지만 168시간 미경과 건은 스킵. 반품도 동일 기준 적용 |
| A-04 | "반품 배송비(3,000원)" (US-504), "왕복 배송비(6,000원)" (US-505) | 금액이 하드코딩. 택배사별 차등 없음. 교환 6,000원 산출 근거 모호 (3,000 x 2인지, 교환 전용인지). **모호성 분석에서 A-05, A-06으로 이중 지적** | (A) 하드코딩 유지 / (B) DB 설정 테이블 관리 / (C) 택배사별 차등 요금 | **(B) `shipping_fee_policy` 테이블로 관리**. 사유별(CHANGE_OF_MIND/DEFECTIVE/SIZE_MISMATCH), 유형별(RETURN/EXCHANGE) 매트릭스로 운영. 초기값은 3,000/6,000 고정이되, 변경 가능한 구조 |
| A-05 | SIZE_MISMATCH 반품 시 배송비 부담 주체 미분류 (US-504) | AC에서 CHANGE_OF_MIND=구매자, DEFECTIVE/WRONG_ITEM=판매자만 명시. SIZE_MISMATCH는 API 예시에서 `BUYER`이나 AC 누락. **모호성 분석 2.1에서 HIGH로 지적** | (A) 구매자 귀책 (단순변심과 동일) / (B) 상품 사이즈 표기 오류면 판매자 귀책 | **(A) 기본은 구매자 부담 + 사이즈 표기 오류 시 판매자 부담으로 분기**. `SIZE_MISMATCH_BUYER`, `SIZE_MISMATCH_SELLER` 두 사유로 분리하여 배송비 정책 매핑 |
| A-06 | "재고가 부족하면 주문을 거절" - 차감 시점 (US-601) | `order.created` 이벤트로 비동기 차감이면 결제 전 차감. 결제 실패 시 복구 필요. **모호성 A-07 + 기술실현성 4.3 상충 분석에서 동시 지적** | (A) 주문 생성 시 즉시 차감 (비동기) / (B) 결제 완료 시 차감 / (C) 주문 생성 시 예약(RESERVE) -> 결제 완료 시 확정(DEDUCT) | **(C) 기존 Flyway 스키마의 3단 구조(total/available/reserved) 활용**. RESERVE -> DEDUCT 2단계가 기존 스키마와 가장 일치하며, 결제 실패 시 RELEASE로 안전하게 복구 |
| A-07 | "100개 스레드 동시성 테스트" (US-602) | 테스트 기준만 있고, 프로덕션 목표 TPS 미정의. **모호성 A-08 + 비기능분석에서 동시 지적** | (A) 100스레드 = 프로덕션 목표 / (B) 별도 TPS 목표 정의 | **(B) 프로덕션 목표 TPS 30 req/s 설정 (비기능분석 근거: 일 50건 x 피크 x10)**. 100스레드 테스트는 안전 마진용이며, 프로덕션 SLA는 재고 차감 P99 200ms 이내 |
| A-08 | "인덱싱 지연은 3초 이내를 목표로 한다" (US-701) | hard requirement인지 soft goal인지 불명확. Percentile 기준 없음. **모호성 A-11에서 지적** | (A) P99 기준 hard requirement / (B) 평균 기준 soft goal | **(B) P95 기준 soft goal로 정의**. 비기능분석의 추정(Kafka 10ms + Consumer 50ms + ES 200ms = ~260ms)에 따르면 P95 3초는 충분히 달성 가능 |
| A-09 | "오타 교정 제안을 제공한다" (US-702) | 구현 방식 미정의. **모호성 A-13 + 기능분석에서 동시 지적** | (A) ES fuzzy query / (B) ES suggest API / (C) 커스텀 사전 기반 | **(A) ES fuzzy query (fuzziness=AUTO)**. Phase 2 MVP에서는 한글 오타 교정 범위가 제한적이므로 fuzzy query로 시작하고, 유의어 사전으로 보완. 정교한 한글 오타 교정은 Phase 3 |
| A-10 | "1시간 단위로 갱신 (sliding window)" - 인기 검색어 (US-705) | 실시간 sliding window인지, 매시간 정각 스냅샷인지 불명확. **모호성 A-15에서 지적** | (A) 매시간 정각 배치 재계산 / (B) 실시간 Redis Sorted Set sliding window | **(B) Redis Sorted Set + score=timestamp 방식 실시간 sliding window**. 비기능분석의 인기 검색어 TPS 50 req/s와 Redis 10ms 이내 응답 목표에 적합. 매 조회 시 1시간 이전 데이터를 ZREMRANGEBYSCORE로 정리 |
| A-11 | "리사이즈(400x400 썸네일) 처리한다" - 동기/비동기 미결정 (US-801) | Lambda vs 서비스 내 처리 미확정. **모호성 A-18 + 기술실현성 6.5에서 동시 지적** | (A) AWS Lambda 비동기 / (B) 서비스 내 동기 (Thumbnailator) | **(B) 서비스 내 Thumbnailator 동기 처리**. 기술실현성 분석에서 "표준 패턴, 실현성 높음"으로 평가. Phase 2에서는 Lambda 인프라 추가 없이 서비스 내 처리가 간결. TPS 5 req/s로 부하 미미 |
| A-12 | "하루 최대 리뷰 포인트 적립 한도: 5,000P" (US-803) | "하루" 기준 불명확. **모호성 A-20에서 지적** | (A) KST 00:00 리셋 / (B) 24시간 rolling window | **(A) KST 00:00 리셋**. 비기능분석의 Redis 키 설계 `review:daily_point:{memberId}` (TTL 24시간)와 일치. 자정 기준이 사용자 직관에 부합 |

---

## 2. 누락된 요구사항

5개 분석에서 공통으로 지적된 누락 항목을 영향도 순으로 정리합니다.

| # | 누락 항목 | 필요 이유 | 영향도 | 제안 |
|---|----------|-----------|--------|------|
| N-01 | **WMS(창고관리시스템) 전체 PRD** | 프로젝트 계획에 "WMS 상세 구현(올리브영 GMS 벤치마킹)" 명시되나 PRD에 전혀 없음. **모호성(3.1 CRITICAL) + 기능분석(6.1) + 작업범위(E. closet-wms ~85파일) 3개 분석에서 공통 지적** | CRITICAL | Phase 2.5로 분리하여 별도 WMS PRD 작성. 단, closet-inventory와의 이벤트 인터페이스(wms.inbound.confirmed, wms.outbound.completed, wms.stocktake.adjusted)는 Phase 2에서 미리 정의 |
| N-02 | **재고 입고(INBOUND) API** | 상품 등록 시 초기 재고 설정 방법 없음. 재입고 알림(US-604)의 트리거인 재입고 이벤트도 발행 불가. **기능분석(US-601 이슈3, US-604 이슈1) + 모호성(3.1) 동시 지적** | HIGH | `POST /api/v1/inventories/{id}/inbound` API 추가. `ChangeType.INBOUND` 이미 작업범위에 정의됨. 판매자/WMS의 입고 등록 엔드포인트 필수 |
| N-03 | **결제 취소/환불 API 인터페이스 상세** | US-504 반품 환불에서 payment-service 호출하나, 부분 환불 지원 여부, 배송비 공제 방식, PG사별 정책 전혀 미정의. **모호성(3.3 HIGH) + 기능분석(US-504 이슈3, 7) 동시 지적** | HIGH | Phase 1 payment-service에 `POST /api/v1/payments/{id}/refund` (amount 파라미터 포함) 부분 환불 API 추가. 환불 금액 = 원결제 - 반품배송비(구매자 부담 시) |
| N-04 | **서비스 간 인증/인가 (RBAC)** | 판매자/구매자/관리자 역할 구분 없이 JWT 검증만 존재. **비기능분석(2.1 Gap) + 기술실현성(5.3 리스크) + 모호성(3.5 MEDIUM) 3개 분석에서 공통 지적** | HIGH | JWT claim에 `role` 추가 (BUYER/SELLER/ADMIN). Gateway에 `RoleAuthorizationFilter` 추가. Phase 2 착수 전 인가 인프라 선행 구축 필요 |
| N-05 | **Kafka Saga/보상 트랜잭션 패턴** | 재고 차감 성공 -> 결제 실패 -> 재고 복구 시나리오, 이벤트 중복 수신 멱등성 등 상세 설계 없음. **모호성(3.6 MEDIUM) + 비기능분석(6.1 멱등성) 동시 지적** | HIGH | Transactional Outbox 패턴 + `processed_event` 테이블(비기능분석 6.1 설계) 도입. 재고/포인트 이중 처리 방지를 위한 멱등성 키 필수 |
| N-06 | **product-service Kafka 이벤트 발행** | 검색 인덱싱(US-701)의 전제조건이나, Phase 1 product-service에서 Kafka 이벤트 미발행. 현재 `ApplicationEventPublisher`만 사용. **기능분석(US-701 이슈1) + 기술실현성(2.2 GAP) 동시 지적** | HIGH | closet-product에 `@TransactionalEventListener` + KafkaTemplate 추가. product.created/updated/deleted 토픽으로 이벤트 발행. Phase 2 스코프에 포함 필수 |
| N-07 | **orderId 기반 배송 추적 API** | US-502는 shippingId로만 조회하나, 구매자 입장에서는 orderId가 자연스러움. **기능분석(US-502 이슈5)에서 지적** | MEDIUM | `GET /api/v1/shippings?orderId={orderId}` API 추가. BFF에서 orderId -> shippingId 변환 없이 직접 조회 가능하도록 |
| N-08 | **자동 구매확정 D-1 사전 알림** | 구매자에게 "내일 자동 구매확정 됩니다" 알림 없음. CS 부담 증가 우려. **기능분석(US-503 이슈3) + Happy/Unhappy Path에서 누락으로 지적** | MEDIUM | 자동 구매확정 배치와 별도로 D-1 알림 배치 추가. notification-service 연동 (inventory.low_stock과 동일 패턴으로 Kafka 이벤트 발행) |
| N-09 | **알림 서비스(notification-service) PRD** | 안전재고/재입고/배송완료/구매확정 등 알림 이벤트를 수신하는 서비스 PRD 자체가 없음. **기능분석(US-603 이슈1, 6.5) + 작업범위(closet-notification 빈 모듈) 동시 지적** | MEDIUM | Phase 2 범위에서는 Kafka 이벤트 발행까지만. 실제 알림 발송(이메일/SMS/푸시)은 Phase 3. 단, 이벤트 스키마는 Phase 2에서 확정 |
| N-10 | **리뷰 신고/블라인드/관리자 관리 기능** | 부적절한 리뷰 대응 수단 없음. ReviewStatus에 HIDDEN은 있으나 관리 API 없음. **기능분석(US-801 이슈3, 3.3 역할 누락) + 모호성(2.6 LOW) 동시 지적** | MEDIUM | `PATCH /api/v1/admin/reviews/{id}/hide` 관리자 블라인드 API 추가. 리뷰 신고는 Phase 3 |
| N-11 | **판매자 리뷰 답변(댓글) 기능** | 무신사 벤치마킹 시 판매자 리뷰 답변은 필수. **기능분석(3.4 역할 누락)에서 지적** | LOW | Phase 3 스코프. review 테이블에 `seller_reply`, `seller_replied_at` 컬럼을 미리 설계하면 후속 확장 용이 |
| N-12 | **최근 검색어 기능** | 사용자별 최근 검색어 저장/조회/삭제. 무신사 벤치마킹 시 필수. **기능분석(US-704 이슈2, 6.4)에서 지적** | LOW | Redis `List` 타입으로 `search:recent:{memberId}` 키 관리. Phase 2에서 검색 서비스와 함께 구현 권장 |

---

## 3. 추가 고려사항

| # | 고려사항 | 카테고리 | 상세 | 제안 | 벤치마킹 참조 |
|---|---------|----------|------|------|-------------|
| C-01 | **PRD 스키마 vs 기존 Flyway 스키마 불일치** | 데이터 모델 | PRD inventory 테이블은 `quantity` 단일 필드이나, 기존 스키마는 `total_quantity/available_quantity/reserved_quantity` 3단 구조. shipping 테이블도 PRD에 seller_id/수령인 정보 없으나 기존 스키마에 존재. **기능분석(US-601 이슈1, US-501 이슈2,3) + 기술실현성(3.1) 공통 지적** | **기존 Flyway 스키마를 기준으로 구현**. PRD의 비즈니스 요구사항은 반영하되 데이터 모델은 기존 스키마 우선 | 쿠팡 WMS: available/reserved/damaged 3단 재고 구조 표준 |
| C-02 | **포트 충돌** | 인프라 | PRD 지정 포트 8084(shipping), 8085(inventory)가 기존 payment(8084), bff(8085)와 충돌. **기술실현성(5.2)에서 지적** | shipping=8088, inventory=8089로 재배정. 작업범위 분석의 docker-compose 포트(8085~8089) 참고 | - |
| C-03 | **ApplicationEvent -> Kafka 전환** | 아키텍처 | closet-order에서 현재 Spring ApplicationEventPublisher만 사용. Kafka Producer 미사용. 분산 이벤트 전환 필요. **기술실현성(2.2 GAP)에서 지적** | Transactional Outbox 패턴 도입. `@TransactionalEventListener`에서 outbox 테이블 INSERT -> 별도 폴러가 Kafka 발행. 기존 인프로세스 이벤트는 유지 | 올리브영 GMS: Outbox + Debezium CDC 패턴 |
| C-04 | **closet-shipping, closet-inventory src 복구** | 기술 부채 | build/ 아티팩트만 남아있고 src/ 없음. settings.gradle.kts에도 미등록. **기술실현성(1.1 핵심 발견)에서 지적** | 재개발 결정. build 아티팩트의 클래스 구조(ShipmentService, InventoryLockService 등)를 참고하되, Phase 2 PRD 기준으로 새로 구현 | - |
| C-05 | **review 테이블 rating TINYINT(1) 컨벤션 위반** | 데이터 모델 | 프로젝트 컨벤션에서 `TINYINT(1)`은 boolean 전용. 별점 1-5 저장에 사용하면 혼동. **모호성(4.5 LOW)에서 지적** | `TINYINT UNSIGNED NOT NULL` (display width 미지정)으로 변경하여 boolean 컨벤션과 구분 | - |
| C-06 | **부분 재고 부족 시 주문 처리 정책** | 비즈니스 로직 | 주문에 여러 상품 포함 시 일부만 재고 부족인 경우 처리 미정의. **모호성(2.4 MEDIUM) + 기능분석(US-601 이슈5) 동시 지적** | All-or-Nothing 정책 채택 (전체 주문 거절). 부분 처리는 CS 복잡도 과도. 재고 부족 SKU 정보를 에러 응답에 포함 | 무신사: All-or-Nothing. 쿠팡: 부분 취소 허용 (Phase 3 고려) |
| C-07 | **교환 시 가격 차이 처리** | 비즈니스 로직 | 동일 상품 다른 옵션에 additionalPrice 차이 가능. 처리 규칙 없음. **모호성(2.3 MEDIUM) + 기능분석(US-505 이슈1) 동시 지적** | Phase 2에서는 가격 차이 있는 옵션 교환 불가 (동일 가격 옵션만 교환 허용). 차액 정산은 Phase 3 | 무신사: 동일 가격 옵션만 교환. 차액 발생 시 반품 후 재주문 안내 |
| C-08 | **리뷰 이미지 S3 URL 보안** | 보안 | S3 URL 직접 노출 시 URL 유추로 타인 이미지 접근 가능. **비기능분석(2.2 Gap)에서 지적** | CloudFront Signed URL (만료시간 1시간) 또는 S3 presigned URL 적용 | 무신사: CDN + Signed URL |
| C-09 | **검색 Rate Limiting** | 보안 | 검색 API 무차별 호출 방지 규칙 없음. **비기능분석(2.1 Gap)에서 지적** | IP 기준 분당 120회, 사용자 기준 분당 60회. Gateway에서 처리 | - |
| C-10 | **Phase 1 데이터 마이그레이션** | 운영 | 기존 주문/상품 데이터의 재고 매핑, ES 인덱싱, 미발송 주문 처리 방안 없음. **모호성(3.7 MEDIUM) + 작업범위(4.1~4.3) 동시 지적** | 작업범위 분석의 마이그레이션 절차 채택: (1) inventory 초기 데이터 생성 (2) ES 벌크 인덱싱 (3) 기존 PAID 주문은 Phase 2 배송 흐름 적용 (4) review_summary 초기화. Feature Flag로 점진 활성화 | - |

---

## 4. 영향 범위 요약

| 파트 | 예상 작업 항목 | 비고 |
|------|--------------|------|
| **신규 서비스 (4개)** | closet-shipping (~30파일), closet-inventory (~25파일), closet-search (~22파일), closet-review (~25파일) | 총 ~102파일. WMS 제외 시 |
| **신규 서비스 (WMS)** | closet-wms (~85파일): 입고/보관/피킹/출고/실사 6개 서브도메인 | Phase 2.5 분리 권장 |
| **기존 서비스 수정 (6개)** | closet-order (상태 확장 + Kafka), closet-product (이벤트 발행), closet-payment (부분 환불), closet-member (포인트 컨슈머), closet-bff (~15파일 신규), closet-common (공통 설정) | 총 ~35파일 수정 |
| **인프라** | Kafka 토픽 17개, Redis 키 패턴 10개, ES 인덱스 1개, docker-compose 5개 서비스 추가, settings.gradle.kts 수정 | - |
| **DB** | 신규 테이블 11개(WMS 제외) / 30개(WMS 포함), Flyway 마이그레이션 파일, processed_event 멱등성 테이블 | 기존 테이블 DDL 변경 없음 (VARCHAR enum) |
| **QA** | 배송 20, 재고 15, 검색 16, 리뷰 17, WMS 25, 크로스도메인 8 = 총 101개 시나리오 | WMS 제외 시 76개 |
| **공수 (WMS 제외)** | 순수 개발 ~14.5주 + 리뷰/버그 +20% + QA 2주 = **~19.4주 (1인 기준)** | 3명 기준 ~8주 (PRD 목표 달성 가능) |
| **공수 (WMS 포함)** | 순수 개발 ~19.5주 + 리뷰/버그 +20% + QA 2주 = **~25.4주 (1인 기준)** | 4명 기준 ~8주 |

---

## 5. 주문 상태 머신 통일

PRD와 기존 코드 사이의 주문/배송 상태 불일치가 **3개 분석(기능분석, 기술실현성, 모호성)**에서 공통 지적되었습니다.

### 5.1 불일치 현황

| 구분 | PRD 표기 | 기존 코드 (OrderStatus.kt) | Mock 서버 (CarrierService) |
|------|---------|--------------------------|--------------------------|
| 송장 등록 시 | `PAID -> SHIPPING` | `PAID -> PREPARING -> SHIPPED` | - |
| 배송 중 | `SHIPPING` | `SHIPPED` | `ACCEPTED -> IN_TRANSIT -> OUT_FOR_DELIVERY` |
| 배송 완료 | `DELIVERED` | `DELIVERED` | `DELIVERED` |
| 구매 확정 | `CONFIRMED` | `CONFIRMED` | - |
| 반품 요청 | - (return_request 별도) | `OrderItemStatus.RETURN_REQUESTED` | - |
| 배송 상태 | `READY -> IN_TRANSIT -> DELIVERED` (3단계) | - | `ACCEPTED -> IN_TRANSIT -> OUT_FOR_DELIVERY -> DELIVERED` (4단계+) |

### 5.2 통일 제안

#### A. 주문 상태 (OrderStatus) -- 기존 코드 기준 유지 + 확장

```
PENDING -> STOCK_RESERVED -> PAID -> PREPARING -> SHIPPED -> DELIVERED -> CONFIRMED
                                 \-> CANCELLED                         \-> RETURN_REQUESTED
                                                                       \-> EXCHANGE_REQUESTED
```

- `SHIPPING` 용어는 사용하지 않습니다. 기존 `PREPARING`(송장 등록 = 배송 준비)과 `SHIPPED`(택배사 인수 = 배송 중)를 유지합니다.
- `RETURN_REQUESTED`, `EXCHANGE_REQUESTED`는 주문 아이템 레벨(`OrderItemStatus`)로 관리하는 것을 권장합니다. 부분 반품/교환 시 주문 전체 상태가 아닌 아이템별 상태가 필요하기 때문입니다.

#### B. 배송 상태 (ShippingStatus) -- PRD 3단계 + Mock 서버 매핑

| ShippingStatus (DB 저장) | 설명 | Mock 서버 매핑 |
|--------------------------|------|---------------|
| `READY` | 송장 등록 완료, 택배사 인수 대기 | `ACCEPTED` |
| `IN_TRANSIT` | 배송 중 | `IN_TRANSIT`, `OUT_FOR_DELIVERY` |
| `DELIVERED` | 배송 완료 | `DELIVERED` |

- `shipping_tracking_log` 테이블에 Mock 서버의 세분화 상태(`ACCEPTED`, `OUT_FOR_DELIVERY` 등)를 원본 그대로 저장합니다.
- `ShippingStatus`는 PRD의 3단계를 유지하되, 상세 추적은 로그에서 확인합니다.

#### C. 상태 전이 트리거 매핑

| 전이 | 트리거 | 서비스 | 이벤트 |
|------|--------|--------|--------|
| `PAID -> PREPARING` | 판매자 송장 등록 | shipping-service | `order.status.changed` (PREPARING) |
| `PREPARING -> SHIPPED` | 택배사 API에서 ACCEPTED/IN_TRANSIT 감지 | shipping-service (스케줄러) | `order.status.changed` (SHIPPED) |
| `SHIPPED -> DELIVERED` | 택배사 API에서 DELIVERED 감지 | shipping-service (스케줄러) | `order.status.changed` (DELIVERED) |
| `DELIVERED -> CONFIRMED` | 수동 확인 / 7일 자동 배치 | order-service / shipping-service (배치) | `order.confirmed` |

---

## 6. WMS 상세 요구사항 보충

PRD에 전혀 없으나, 프로젝트 계획과 작업범위 분석(closet-wms ~85파일)에서 올리브영 GMS 벤치마킹 기반 WMS 요구사항이 도출되었습니다. 6개 서브도메인별 기능 요구사항을 보충합니다.

### 6.1 입고 관리 (Inbound/Receiving)

> 올리브영 GMS: ASN(Advanced Shipping Notice) 기반 입고 관리. 검수 -> 입고 확정 -> 재고 반영 자동화.

| 기능 | 설명 | 상태 흐름 | inventory 연동 |
|------|------|-----------|---------------|
| 입고 예정(ASN) 등록 | 셀러/브랜드가 물류센터 발송 예정 정보 등록. 품목/수량/예상 도착일 | `EXPECTED` | - |
| 도착 확인 | 물류센터에서 실물 도착 확인 | `EXPECTED -> ARRIVED` | - |
| 입고 검수 | 수량 확인, 불량 검수, 바코드 스캔. 양품/불량/부분합격 판정 | `ARRIVED -> INSPECTING -> INSPECTION_COMPLETED` | - |
| 입고 확정 | 검수 통과 상품 재고 반영 | `INSPECTION_COMPLETED -> CONFIRMED` | `wms.inbound.confirmed` Kafka 이벤트 -> inventory 재고 증가 |
| 입고 취소/반려 | 불량/오배송 상품 셀러 반송 | `-> CANCELLED` | - |

**필요 테이블**: `inbound_order`, `inbound_order_item`, `inbound_receipt`, `inbound_receipt_item`

### 6.2 보관/로케이션 (Storage/Location)

> 올리브영 GMS: Zone > Aisle > Rack > Shelf > Bin 5레벨 로케이션 체계. ABC 분석 기반 최적 배치.

| 기능 | 설명 |
|------|------|
| 로케이션 체계 | `W01-Z01-A01-R01-C01` 형식 (Warehouse > Zone > Aisle > Rack > Cell) |
| 로케이션 유형 | GENERAL(일반), COLD(냉장), HAZARDOUS(위험물), VALUABLE(귀중품) |
| 적치(Putaway) | 입고 검수 완료 후 보관 로케이션 이동. 자동 위치 추천(빈 공간 + 동일 SKU 인접) |
| 로케이션별 재고 | 동일 SKU가 여러 로케이션에 분산 보관 가능. `location_inventory` 테이블 |

**필요 테이블**: `warehouse`, `location`, `location_inventory`, `putaway_task`

**적치 상태**: `PENDING -> IN_PROGRESS -> COMPLETED`

### 6.3 피킹 (Picking)

> 올리브영 GMS: 웨이브 피킹 + FIFO 전략. 바코드 스캔 기반 실시간 피킹 확인.

| 기능 | 설명 | 상태 흐름 |
|------|------|-----------|
| 피킹 웨이브 생성 | 다건 주문을 시간대/구역별로 묶어 피킹 지시 | `CREATED -> ASSIGNED -> IN_PROGRESS -> COMPLETED` |
| 피킹 작업 지시 | SKU + 로케이션 + 수량 조합의 피킹 태스크 생성 | `PENDING -> ASSIGNED -> IN_PROGRESS -> COMPLETED / SHORT` |
| 피킹 전략 | FIFO(선입선출), FEFO(선만료선출), CLOSEST_LOCATION(최근거리) | 전략 enum으로 관리 |
| 부분 피킹(SHORT) | 로케이션 재고 부족 시 가용 수량만 피킹. 부족분 알림 | SHORT 상태 -> 재고 실사 트리거 가능 |
| 피킹 경로 최적화 | 로케이션 코드 순서 기반 동선 최적화 | - |

**필요 테이블**: `picking_wave`, `picking_task`, `picking_task_item`

### 6.4 출고 (Outbound/Packing)

> 올리브영 GMS: 피킹 -> 패킹 -> 검수 -> 송장 발행 -> 택배사 인수 일관 프로세스.

| 기능 | 설명 | 상태 흐름 |
|------|------|-----------|
| 출고 지시 자동 생성 | `order.paid` Kafka 이벤트 수신 시 출고 지시 자동 생성 | `CREATED` |
| 포장(Packing) | 피킹 완료 상품 검품 + 포장. 포장재 선택 | `PICKING -> PICKED -> PACKING -> PACKED` |
| 출고 검수 | 상품/수량 재확인, 바코드 검증 | 포장 중 동시 수행 |
| 송장 발행 | 택배사 API 연동 자동 송장 생성 | `PACKED -> SHIPPED` |
| 출고 완료 | 택배사 인수 확인 | `SHIPPED -> COMPLETED`. `wms.outbound.completed` 이벤트 -> shipping-service |

**필요 테이블**: `outbound_order`, `outbound_order_item`, `packing_task`, `shipment_request`

### 6.5 재고 실사 (Stocktaking/Cycle Count)

> 올리브영 GMS: 일간 순환 실사(Cycle Count) + 분기 전수 실사. ABC 분석 기반 빈도 차등.

| 기능 | 설명 | 상태 흐름 |
|------|------|-----------|
| 실사 유형 | FULL(전수), CYCLE(순환 - ABC 분석 기반), SPOT(스팟 - 특정 로케이션) | - |
| 실사 요청 | 관리자가 대상 Zone/로케이션/SKU 지정 | `REQUESTED` |
| 실사 진행 | 작업자가 실물 수량 입력 | `REQUESTED -> IN_PROGRESS -> COMPLETED` |
| 차이 분석 | 시스템 수량 vs 실사 수량 비교. 불일치 보고서 생성 | `COMPLETED` 시점 |
| 차이 조정 | 관리자 승인 후 재고 수량 보정 | `COMPLETED -> ADJUSTED`. `wms.stocktake.adjusted` 이벤트 -> closet-inventory 재고 동기화 |

**필요 테이블**: `stocktake_order`, `stocktake_order_item`, `stocktake_result`

### 6.6 반품 입고 (Return Receiving)

> 올리브영 GMS: 반품 수거 -> 검수 -> 재판매 판정 -> 재입고 or 폐기 분기.

| 기능 | 설명 | inventory 연동 |
|------|------|---------------|
| 반품 입고 검수 | 반품 상품 상태 확인 (정상/파손/오염) | - |
| 재판매 판정 | 재판매 가능 여부 판단 | - |
| 양품 재입고 | 재판매 가능 시 가용 재고 복구. 로케이션 재배치 | inventory `RESTORE` + putaway_task 생성 |
| 불량 처리 | 재판매 불가 시 폐기 or B급 상품 전환 | inventory `DAMAGED` 상태 |
| 교환 출고 | 교환 건의 신규 상품 출고 연결 | outbound_order 자동 생성 |

### 6.7 WMS 일정 제안

| Sprint | 내용 | 공수 |
|--------|------|------|
| Sprint 9 (Phase 2.5) | 입고(ASN/검수/확정) + 보관(로케이션/적치) + 기초 inventory 동기화 | 2주 |
| Sprint 10 (Phase 2.5) | 피킹(웨이브/태스크/전략) + 출고(포장/송장/완료) | 2주 |
| Sprint 11 (Phase 2.5) | 재고 실사(전수/순환/조정) + 반품 입고 + WMS 대시보드 + E2E 통합 테스트 | 2주 |

---

## 7. 의사결정 필요 항목

5개 분석 결과의 PM 확인 질문(22건), 상충 분석(5건), 애매 항목(20건)을 종합하여 의사결정 필요 항목을 정리합니다.

| # | 질문 | 대상 | 우선순위 | 디폴트 제안 |
|---|------|------|---------|------------|
| D-01 | **WMS를 Phase 2에 포함할지 Phase 2.5로 분리할지?** inventory-service와 WMS의 경계는? | PM | **CRITICAL** | Phase 2.5로 분리. Phase 2에서는 inventory-service의 INBOUND API만 추가하고, WMS 6개 서브도메인은 Sprint 9~11에서 구현. 단, Kafka 이벤트 인터페이스(wms.inbound.confirmed 등)는 Phase 2에서 확정 |
| D-02 | **Mock 서버 연동 방식**: shipping-service가 개별 택배사 API를 직접 호출할지, 통합 API를 추가할지? | PM/TL | **HIGH** | 개별 택배사 API 직접 호출 + CarrierAdapter 패턴. Mock 서버 수정 최소화하면서 학습 목적(택배사별 연동 구현 경험)에도 부합 |
| D-03 | **송장 등록 플로우**: 판매자 수동 입력 vs 시스템 자동 채번? Mock 서버는 자동 채번이나 PRD는 수동 입력. 기능분석 + 모호성 + 기술실현성 3개 분석에서 상충으로 지적 | PM | **HIGH** | 판매자 수동 입력 (PRD 원안 유지). Mock 서버는 조회용으로만 활용. 판매자가 실제 택배사에서 발급받은 송장번호를 입력하는 시나리오. 송장번호 형식 검증은 택배사별 regex로 유연하게 처리 |
| D-04 | **구매확정/반품 기한 "7일" 기준**: 168시간(시각) vs 7일째 자정(일자)? 기능분석 + 모호성 2개 분석에서 경계 충돌 지적 | PM | **HIGH** | 168시간 (시각 기준). `delivered_at + INTERVAL 168 HOUR` 조건으로 통일. 배치 실행 시 이 조건으로 필터링하면 경계 충돌 해소 |
| D-05 | **재고 차감 시점**: 주문 생성 시 즉시 차감(비동기) vs 예약 후 결제 완료 시 확정? 모호성 + 기술실현성에서 상충 지적 | TL | **HIGH** | 기존 Flyway 스키마의 3단 구조 활용: 주문 생성 시 RESERVE -> 결제 완료 시 DEDUCT -> 결제 실패/취소 시 RELEASE. 이 패턴이 보상 트랜잭션 최소화에 가장 유리 |
| D-06 | **반품 SIZE_MISMATCH 배송비 부담 주체**: 구매자? 판매자? 조건부 분기? | PM | **HIGH** | 기본 구매자 부담. shipping_fee_policy 테이블에서 사유별 매핑. 사이즈 표기 오류 분쟁은 CS로 처리 (Phase 2에서는 단순화) |
| D-07 | **부분 재고 부족 시 주문 처리**: All-or-Nothing vs 부분 처리? 모호성 2.4에서 지적 | PM | **MEDIUM** | All-or-Nothing (전체 주문 거절). 부분 처리는 UX 복잡도 과도. 에러 응답에 부족 SKU 정보 포함하여 사용자가 수량 조정 후 재주문 유도 |
| D-08 | **교환 시 가격 차이 처리**: 차액 정산 vs 동일 가격만 교환? 모호성 2.3에서 지적 | PM | **MEDIUM** | Phase 2에서는 동일 가격 옵션만 교환 허용. 가격 차이 있는 옵션은 "반품 후 재주문" 안내. 차액 정산은 Phase 3 |
| D-09 | **인기순(POPULAR) 정렬 기준**: 판매량? 리뷰 수? 복합? 기간? 모호성 2.5에서 지적 | PM | **MEDIUM** | 복합 점수 = (최근 30일 판매량 x 0.4) + (리뷰 수 x 0.3) + (평균 별점 x 0.3). ES function_score 쿼리로 구현 |
| D-10 | **리뷰 수정 범위**: 별점/텍스트/이미지 모두 수정 가능? 수정 이력 보존? 모호성에서 A-19로 지적 | PM | **MEDIUM** | 텍스트 + 이미지 수정 가능, 별점은 수정 불가 (집계 재계산 비용 최소화). 수정 이력은 Phase 2에서 미보존 (단순 덮어쓰기) |
| D-11 | **이미지 리사이즈 방식**: Lambda(비동기) vs 서비스 내(동기)? 모호성 A-18 + 기술실현성 6.5에서 지적 | TL | **MEDIUM** | 서비스 내 Thumbnailator 동기 처리. TPS 5 req/s에서 Lambda 인프라 추가는 오버엔지니어링 |
| D-12 | **배송비/반품비 금액 관리**: 하드코딩 vs DB 설정? 모호성 A-05/A-06에서 지적 | PM | **LOW** | DB shipping_fee_policy 테이블. 초기값 배송비 3,000원, 교환 왕복 6,000원. 택배사별 차등은 Phase 3 |
| D-13 | **안전재고 기본값 10의 근거**: 모든 SKU 동일? 카테고리별 차등? 모호성 A-09에서 지적 | PM | **LOW** | 기본값 10 유지 (MVP). 판매자가 SKU별로 개별 설정 가능하므로 적정값은 판매자 자율. 카테고리별 기본값 차등은 데이터 축적 후 Phase 3 |
| D-14 | **재입고 알림 만료 정책**: WAITING 상태 무기한 유지? 모호성 A-10에서 지적 | PM | **LOW** | 90일 후 자동 만료 (CANCELLED). 배치 스케줄러로 처리. restock_notification 테이블에 `expired_at` 컬럼 추가 |

---

## 부록: 분석 간 상충 사항 및 해소

| 상충 항목 | 분석A 의견 | 분석B 의견 | 해소 결론 |
|-----------|-----------|-----------|-----------|
| 인덱싱 지연 3초 기준 | 비기능분석: Kafka 10ms + Consumer 50ms + ES 200ms = ~260ms로 충분 여유 | 모호성분석: P99/P95/평균 중 어느 기준인지 불명확 | **P95 기준 soft goal**. 260ms 추정이 맞으므로 3초는 여유있는 목표 |
| 재고 스키마 | 기능분석: PRD의 단일 quantity가 간결 | 기술실현성: 기존 Flyway의 3단 구조가 더 정교 | **기존 Flyway 3단 구조(total/available/reserved) 채택**. RESERVE/RELEASE 패턴이 보상 트랜잭션에 필수 |
| Kafka 파티션 수 | 기술실현성: 토픽별 6 파티션 권장 | 작업범위: 일 50건 수준이면 3 파티션으로 충분 | **3 파티션 채택**. 단일 인스턴스 운영이므로 3이면 충분. 향후 수평 확장 시 파티션 추가 |
| Sprint 6 범위 | 작업범위: 반품+교환+재고알림을 Sprint 6에 배정 | 기술실현성: Sprint 6이 가장 위험, 반품/교환 일부를 Sprint 7로 이동 권장 | **기술실현성 의견 채택**. 교환(US-505)을 Sprint 7로 이동하여 Sprint 6 부담 완화 |
| WMS 포함 여부 | 기능분석: Phase 2에 WMS 상세 PRD 별도 작성 | 작업범위: WMS를 Sprint 9~11에 배치 (Phase 2 확장) | **Phase 2.5로 분리하되 Sprint 9~11 일정 유지**. Phase 2 본 범위(Sprint 5~8)에서는 inventory INBOUND API만 추가 |
