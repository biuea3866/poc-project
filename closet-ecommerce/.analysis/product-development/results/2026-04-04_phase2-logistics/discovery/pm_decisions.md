# Phase 2 PM 의사결정 최종 확정본

> 확정일: 2026-04-04
> 범위: Gap 분석 의사결정 14건(D-01~D-14) + 기존 PM 의사결정 22건(Q-01~Q-22) + 애매 표현 수치화 20건(A-01~A-20)
> 상태: 전건 [확정]

---

## 1. 아키텍처/범위 결정

### PD-01: WMS 구현 범위 [확정]
- **결정**: Phase 2.5로 분리 (Sprint 9~11). Phase 2 본편(Sprint 5~8)에서는 inventory INBOUND API만 추가합니다.
- **근거**: Gap D-01 + Q-06 통합. WMS 85파일 규모를 Phase 2에 포함하면 8주 일정 초과. Kafka 이벤트 인터페이스(wms.inbound.confirmed, wms.outbound.completed, wms.stocktake.adjusted)는 Phase 2에서 스키마 확정합니다.
- **상충 해소**: Q-06은 "Phase 2에 포함"이었으나, Gap 분석 결과 공수 초과가 확인되어 Phase 2.5 분리로 통일합니다.

### PD-02: Mock 서버 연동 방식 [확정]
- **결정**: 각 택배사 개별 API 직접 호출 + CarrierAdapter(Strategy) 패턴입니다.
- **근거**: D-02 + Q-01 일치. Mock 서버가 이미 CJ/로젠/롯데/우체국 4개 개별 API로 구현되어 있으며, 택배사별 연동 구현 경험이 학습 목적에 부합합니다.

### PD-03: 서비스 간 인증 [확정]
- **결정**: `X-Internal-Api-Key` 헤더 방식입니다.
- **근거**: Q-21 확정. Gateway에서 외부 요청의 내부 키 헤더를 제거하여 보안을 확보합니다. JWT claim에 `role`(BUYER/SELLER/ADMIN) 추가는 Gap N-04에 따라 Phase 2 착수 전 선행 구축합니다.

### PD-04: 이벤트 멱등성 패턴 [확정]
- **결정**: Transactional Outbox 패턴 + `processed_event` 테이블 + eventId 기반 중복 방지입니다.
- **근거**: Q-22 + Gap N-05 통합. Consumer: SELECT -> INSERT (UNIQUE KEY) -> 비즈니스 로직 -> COMMIT 순서로 처리합니다.

### PD-05: 포트 재배정 [확정]
- **결정**: shipping=8088, inventory=8089, search=8086, review=8087입니다.
- **근거**: Gap C-02. PRD 원안의 8084(shipping)/8085(inventory)가 기존 payment(8084)/bff(8085)와 충돌합니다.

### PD-06: 스키마 기준 [확정]
- **결정**: 기존 Flyway 스키마를 기준으로 구현합니다. PRD 비즈니스 요구사항은 반영하되, 데이터 모델은 기존 스키마 우선입니다.
- **근거**: Gap C-01. inventory의 3단 구조(total/available/reserved)가 PRD의 단일 quantity보다 정교하며, RESERVE/RELEASE 패턴에 필수입니다.

---

## 2. 배송 도메인 결정

### PD-07: 송장 등록 플로우 [확정]
- **결정**: 시스템 자동 채번 + 판매자 수동 입력 병행입니다.
- **근거**: Q-02 확정. "배송 시작" 버튼 -> shipping-service -> Mock 택배사 API 배송 등록 -> 송장번호 자동 수신. 기존 송장이 있는 경우 수동 입력도 허용합니다.
- **상충 해소**: D-03은 "판매자 수동 입력 (PRD 원안 유지)"이었으나, Q-02의 "시스템 자동 채번 + 수동 병행"이 Mock 서버 인프라 활용도와 UX 측면에서 우수하므로 Q-02로 통일합니다.

### PD-08: 송장번호 형식 검증 [확정]
- **결정**: `[A-Z]{0,4}[0-9]{10,15}` (prefix 허용)입니다.
- **근거**: 상충 해결 4.1. PRD 원안의 "숫자 10-15자리"에서 알파벳 prefix를 허용하도록 확장합니다.

### PD-09: 주문 상태 머신 [확정]
- **결정**: 기존 코드 기준 유지 + 확장입니다.
  ```
  PENDING -> STOCK_RESERVED -> PAID -> PREPARING -> SHIPPED -> DELIVERED -> CONFIRMED
  ```
- **근거**: Gap 5.2. PRD의 `SHIPPING` 용어는 사용하지 않습니다. `PREPARING`(송장 등록 = 배송 준비)과 `SHIPPED`(택배사 인수 = 배송 중)를 유지합니다.

### PD-10: 배송 상태 매핑 [확정]
- **결정**: PRD 3단계 + Mock 서버 상태 매핑입니다.

| ShippingStatus | Mock 서버 매핑 |
|----------------|---------------|
| READY | ACCEPTED |
| IN_TRANSIT | IN_TRANSIT, OUT_FOR_DELIVERY |
| DELIVERED | DELIVERED |

- **근거**: Gap 5.2 + 상충 해결 4.4. `shipping_tracking_log`에 Mock 서버의 세분화 상태를 원본 그대로 저장합니다.

### PD-11: 반품 사유별 배송비 부담 [확정]
- **결정**: 아래 매핑 테이블을 확정합니다.

| 사유 | 배송비 부담 | 금액 |
|------|-----------|------|
| DEFECTIVE (불량) | SELLER | 0원 |
| WRONG_ITEM (오배송) | SELLER | 0원 |
| SIZE_MISMATCH (사이즈 불일치) | BUYER | 3,000원 |
| CHANGE_OF_MIND (단순변심) | BUYER | 3,000원 |

- **근거**: Q-03 + D-06 통합. SIZE_MISMATCH는 기본 구매자 부담입니다(Gap A-05). 사이즈 표기 오류 분쟁은 CS로 처리합니다(Phase 2에서는 단순화).
- **상충 해소**: Gap A-05의 `SIZE_MISMATCH_BUYER/SIZE_MISMATCH_SELLER` 2분리안은 복잡도가 높으므로, Phase 2에서는 SIZE_MISMATCH = BUYER 단일 처리로 통일합니다.

### PD-12: 반품 배송비 공제 방식 [확정]
- **결정**: 환불 시 배송비 공제 후 환불입니다. `환불금액 = 결제금액 - 반품배송비(3,000원)`. PG사에 부분 취소(cancelAmount) 요청합니다.
- **근거**: Q-07 확정. Gap N-03에 따라 payment-service에 `POST /api/v1/payments/{id}/refund` (amount 파라미터 포함) 부분 환불 API를 추가합니다.

### PD-13: 반품 검수 기준 [확정]
- **결정**: 판매자 검수(물류센터 대행). 3영업일 내 승인/거절, 초과 시 자동 승인입니다.
- **근거**: Q-09 확정. 검수 기준: 태그 부착 여부, 사용 흔적, 포장 상태.

### PD-14: 교환 시 가격 차이 [확정]
- **결정**: 동일 상품 = 동일 가격만 교환 허용입니다. 가격 차이 있는 옵션은 "반품 후 재주문" 안내합니다.
- **근거**: Q-08 + D-08 일치. 차액 정산은 Phase 3에서 구현합니다.

### PD-15: 배송비 금액 관리 [확정]
- **결정**: `shipping_fee_policy` 테이블로 DB 관리합니다.
- **근거**: Q-15 + D-12 + Gap A-04 통합. 사유별(CHANGE_OF_MIND/DEFECTIVE/SIZE_MISMATCH), 유형별(RETURN/EXCHANGE) 매트릭스로 운영합니다. 초기값 배송비 3,000원, 교환 왕복 6,000원.

---

## 3. 구매확정/기한 결정

### PD-16: 구매확정/반품 기한 "7일" 기준 [확정]
- **결정**: 168시간(시각 기준). `delivered_at + INTERVAL 168 HOUR`으로 통일합니다.
- **근거**: Q-04 + D-04 일치. 반품/교환이 구매확정보다 우선합니다(168시간 만료 시점에 반품 신청이 접수 중이면 구매확정 배제).

### PD-17: 자동 구매확정 주기 [확정]
- **결정**: 매일 00:00 + 12:00 (2회) 배치 실행합니다.
- **근거**: A-04 확정. 배치 쿼리: `WHERE delivered_at <= NOW() - INTERVAL 168 HOUR AND status NOT IN ('RETURN_REQUESTED', 'EXCHANGE_REQUESTED')`.

---

## 4. 재고 도메인 결정

### PD-18: 재고 차감 시점 [확정]
- **결정**: 주문 생성 시 RESERVE -> 결제 완료 시 DEDUCT -> 결제 실패/취소 시 RELEASE 3단계입니다.
- **근거**: Q-05 + D-05 통합. 기존 Flyway 스키마의 3단 구조(total/available/reserved) 활용합니다. 15분 내 결제 미완료 시 자동 취소(주문 TTL).

### PD-19: 부분 재고 부족 시 처리 [확정]
- **결정**: All or Nothing (전체 주문 거절)입니다.
- **근거**: Q-10 + D-07 일치. `inventory.insufficient` 이벤트 발행 -> 에러 응답에 부족 SKU 정보 포함합니다.

### PD-20: 안전재고 기본값 [확정]
- **결정**: 카테고리별 차등 기본값입니다.

| 카테고리 | 기본값 |
|---------|--------|
| 상의/하의 | 10 |
| 아우터 | 5 |
| 신발 | 8 |
| 액세서리 | 15 |

- **근거**: Q-16 확정. SKU별 수동 오버라이드 가능합니다.
- **상충 해소**: D-13은 "기본값 10 유지(MVP)"였으나, Q-16의 카테고리별 차등이 더 현실적이므로 Q-16으로 통일합니다.

### PD-21: 재입고 알림 만료 [확정]
- **결정**: 90일 후 자동 만료(EXPIRED)입니다.
- **근거**: Q-17 + D-14 일치. 배치로 WAITING 상태 90일 초과 건 EXPIRED 처리합니다.

### PD-22: 동시성 목표 TPS [확정]
- **결정**: 프로덕션 목표 100 TPS (초당 100건 주문). 재고 차감 P99 200ms 이내입니다.
- **근거**: A-08 확정. 100스레드 테스트는 안전 마진용이며, 실 트래픽은 일 50건 수준입니다.

---

## 5. 검색 도메인 결정

### PD-23: 인기순(POPULAR) 정렬 기준 [확정]
- **결정**: 복합 점수 (최근 30일 기준)입니다.
  ```
  score = 판매량 * 0.4 + 리뷰 수 * 0.3 + 평균 별점 * 0.2 + 조회수 * 0.1
  ```
- **근거**: Q-11 + D-09 통합. Sprint 5에서는 판매량 기준 단순 정렬, Sprint 7에서 복합 점수로 고도화합니다.
- **상충 해소**: D-09의 가중치(0.4/0.3/0.3)에서 Q-11의 4요소 가중치(0.4/0.3/0.2/0.1)로 통일합니다.

### PD-24: 유의어 사전 관리 [확정]
- **결정**: DB `search_synonym` 테이블 + ES synonym filter입니다.
- **근거**: Q-12 확정. 초기 데이터: 패션 도메인 유의어 100개 seed. 관리자 API로 CRUD, 변경 시 ES 인덱스 리로드합니다.

### PD-25: 인기검색어 갱신 방식 [확정]
- **결정**: Redis Sorted Set + score=timestamp 방식 실시간 sliding window입니다.
- **근거**: Gap A-10 확정. 매 조회 시 1시간 이전 데이터를 ZREMRANGEBYSCORE로 정리합니다.
- **상충 해소**: Q-15의 "매시간 정각 배치" 대신 Gap A-10의 실시간 sliding window를 채택합니다. TPS 50 req/s와 Redis 10ms 이내 응답에 적합합니다.

### PD-26: 오타 교정 방식 [확정]
- **결정**: ES fuzzy query (fuzziness=AUTO). Phase 2 MVP입니다.
- **근거**: A-13 + Gap A-09 일치. 정교한 한글 오타 교정은 Phase 3에서 구현합니다.

### PD-27: 자동완성 응답 시간 [확정]
- **결정**: P99 기준 50ms 이내입니다.
- **근거**: A-14 확정.

### PD-28: 벌크 인덱싱 성능 기준 [확정]
- **결정**: 상품 10만개 기준 5분 이내입니다.
- **근거**: Q-18 확정. 100만개 시 별도 최적화.

### PD-29: 인덱싱 지연 기준 [확정]
- **결정**: P95 기준 3초 이내 (soft goal)입니다.
- **근거**: Gap A-08 확정. Kafka 10ms + Consumer 50ms + ES 200ms = ~260ms 추정이므로 여유있는 목표입니다.

### PD-30: 검색 Rate Limiting [확정]
- **결정**: IP 기준 분당 120회, 사용자 기준 분당 60회. Gateway에서 처리합니다.
- **근거**: Gap C-09 확정.

### PD-31: DLQ 재처리 [확정]
- **결정**: 최대 3회, 지수 백오프 (1분/5분/30분). 최종 실패 시 수동 큐입니다.
- **근거**: A-12 확정.

---

## 6. 리뷰 도메인 결정

### PD-32: 리뷰 수정 범위 [확정]
- **결정**: 텍스트 + 이미지 수정 가능, 별점 수정 불가입니다.
- **근거**: Q-13 + D-10 통합. 별점 변경 시 집계 재계산 복잡도가 높으므로 Phase 2에서는 별점 고정합니다.
- **수정 이력**: `review_edit_history` 테이블로 보존합니다 (Q-13 결정 유지).
- **상충 해소**: D-10은 "수정 이력 미보존(단순 덮어쓰기)"이었으나, Q-13의 "수정 이력 보존 + 최대 3회 제한"이 CS 대응에 유리하므로 Q-13으로 통일합니다.

### PD-33: 이미지 리사이즈 방식 [확정]
- **결정**: 서비스 내 동기 처리 (Thumbnailator, 400x400 썸네일)입니다.
- **근거**: Q-14 + D-11 일치. Lambda 인프라 추가는 학습 프로젝트에서 과도합니다. 원본 + 썸네일 모두 로컬 스토리지 저장(S3는 Phase 3).

### PD-34: 리뷰 이미지 크기 [확정]
- **결정**: 5MB/장, 요청 전체 30MB 이하입니다.
- **근거**: A-17 확정.

### PD-35: 관리자 리뷰 관리 [확정]
- **결정**: Phase 2에 포함 (HIDDEN 상태 + 관리자 API)입니다.
- **근거**: Q-19 + Gap N-10 통합. `PATCH /api/v1/admin/reviews/{id}/hide` API 추가. 리뷰 신고 기능은 Phase 3입니다.

### PD-36: 리뷰 포인트 회수 [확정]
- **결정**: 리뷰 삭제 시 포인트 회수. 잔액 부족 시 마이너스 잔액 허용(다음 적립에서 상계)합니다.
- **근거**: Q-20 확정.

### PD-37: 리뷰 포인트 일일 한도 [확정]
- **결정**: KST 00:00 리셋, 일 5,000P입니다.
- **근거**: A-20 + Gap A-12 일치. Redis 키 `review:daily_point:{memberId}` (TTL 24시간).

### PD-38: review 테이블 rating 타입 [확정]
- **결정**: `TINYINT UNSIGNED NOT NULL` (display width 미지정)으로 변경. COMMENT로 "별점 1-5" 명시합니다.
- **근거**: 상충 해결 4.5. `TINYINT(1)`은 boolean 전용 컨벤션이므로 구분합니다.

---

## 7. 금칙어/에러 처리 결정

### PD-39: 금칙어 관리 [확정]
- **결정**: DB 테이블 + 관리자 CRUD API입니다.
- **근거**: A-16 확정.

### PD-40: 에러 로깅 [확정]
- **결정**: ERROR 레벨, 연속 3회 실패 시 Slack 알림입니다.
- **근거**: A-03 확정.

### PD-41: 배송 추적 캐싱 [확정]
- **결정**: Redis TTL 5분. 캐시 miss 시 즉시 택배사 API 호출합니다.
- **근거**: A-01 확정. "실시간" 배송 추적은 최대 5분 지연으로 정의합니다.

---

## 8. 누락 요구사항 결정

### PD-42: 재고 입고(INBOUND) API [확정]
- **결정**: `POST /api/v1/inventories/{id}/inbound` API를 Phase 2에 추가합니다.
- **근거**: Gap N-02. 상품 등록 시 초기 재고 설정, 재입고 알림(US-604)의 트리거인 재입고 이벤트 발행에 필수입니다.

### PD-43: product-service Kafka 이벤트 발행 [확정]
- **결정**: closet-product에 `@TransactionalEventListener` + KafkaTemplate 추가합니다. product.created/updated/deleted 토픽으로 이벤트 발행합니다.
- **근거**: Gap N-06. 검색 인덱싱(US-701)의 전제 조건입니다. Phase 2 스코프에 포함 필수입니다.

### PD-44: orderId 기반 배송 추적 API [확정]
- **결정**: `GET /api/v1/shippings?orderId={orderId}` API를 추가합니다.
- **근거**: Gap N-07. 구매자 입장에서 orderId가 자연스럽습니다. BFF에서 orderId -> shippingId 변환 없이 직접 조회합니다.

### PD-45: 자동 구매확정 D-1 사전 알림 [확정]
- **결정**: 자동 구매확정 배치와 별도로 D-1 알림 배치를 추가합니다.
- **근거**: Gap N-08. CS 부담 경감 목적입니다. Kafka 이벤트 발행까지만 Phase 2 범위이며, 실제 알림 발송은 Phase 3입니다.

### PD-46: 알림 서비스 범위 [확정]
- **결정**: Phase 2에서는 Kafka 이벤트 발행까지만. 실제 알림 발송(이메일/SMS/푸시)은 Phase 3입니다. 이벤트 스키마는 Phase 2에서 확정합니다.
- **근거**: Gap N-09.

### PD-47: 판매자 리뷰 답변(댓글) [확정]
- **결정**: Phase 3 스코프입니다. review 테이블에 `seller_reply`, `seller_replied_at` 컬럼을 미리 설계합니다.
- **근거**: Gap N-11.

### PD-48: 최근 검색어 기능 [확정]
- **결정**: Phase 2에서 검색 서비스와 함께 구현합니다. Redis `List` 타입으로 `search:recent:{memberId}` 키 관리합니다.
- **근거**: Gap N-12.

---

## 9. 인프라/데이터 마이그레이션 결정

### PD-49: Phase 1 데이터 마이그레이션 [확정]
- **결정**: Feature Flag로 점진 활성화합니다.
  1. inventory 초기 데이터 생성
  2. ES 벌크 인덱싱
  3. 기존 PAID 주문은 Phase 2 배송 흐름 적용
  4. review_summary 초기화
- **근거**: Gap C-10 확정.

### PD-50: Kafka 파티션 수 [확정]
- **결정**: 3 파티션입니다.
- **근거**: Gap 부록 상충 해소. 단일 인스턴스 운영이므로 3이면 충분합니다. 향후 수평 확장 시 파티션 추가합니다.

### PD-51: ApplicationEvent -> Kafka 전환 [확정]
- **결정**: Transactional Outbox 패턴을 도입합니다. `@TransactionalEventListener`에서 outbox 테이블 INSERT -> 별도 폴러가 Kafka 발행합니다. 기존 인프로세스 이벤트는 유지합니다.
- **근거**: Gap C-03. 올리브영 GMS의 Outbox + Debezium CDC 패턴을 벤치마킹합니다.

### PD-52: 리뷰 이미지 S3 URL 보안 [확정]
- **결정**: Phase 2에서는 로컬 스토리지 저장이므로 해당 없습니다. Phase 3(S3 전환 시) CloudFront Signed URL 적용합니다.
- **근거**: Gap C-08. Phase 2에서는 로컬 저장으로 결정(PD-33)했으므로 Phase 3으로 연기합니다.

---

## 상충 해소 요약

| # | 상충 항목 | 해소 방법 | 최종 결정 |
|---|----------|----------|----------|
| 1 | Q-06(WMS Phase 2 포함) vs D-01(Phase 2.5 분리) | 공수 분석 결과 D-01 채택 | PD-01: Phase 2.5 분리 |
| 2 | Q-02(시스템 자동 채번) vs D-03(판매자 수동 입력) | Q-02의 병행안이 Mock 서버 활용도 높음 | PD-07: 자동 채번 + 수동 병행 |
| 3 | Gap A-05(SIZE_MISMATCH 2분리) vs Q-03(단일 BUYER) | Phase 2 단순화 목적으로 Q-03 채택 | PD-11: SIZE_MISMATCH = BUYER |
| 4 | D-09(가중치 3요소) vs Q-11(가중치 4요소) | Q-11이 더 세분화 | PD-23: 4요소 가중치 |
| 5 | D-10(수정이력 미보존) vs Q-13(수정이력 보존) | CS 대응 유리한 Q-13 채택 | PD-32: 수정이력 보존 + 최대 3회 |
| 6 | D-13(기본값 10 통일) vs Q-16(카테고리별 차등) | Q-16이 현실적 | PD-20: 카테고리별 차등 |
| 7 | A-15(매시간 배치) vs Gap A-10(실시간 sliding window) | Redis 성능에 적합한 A-10 채택 | PD-25: 실시간 sliding window |
