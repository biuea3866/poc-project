# Phase 2 KPI 정의서

> 확정일: 2026-04-04
> 프레임워크: AARRR (Acquisition, Activation, Retention, Revenue, Referral)
> 범위: 배송, 재고, 검색, 리뷰, WMS 5개 도메인

---

## AARRR 프레임워크 개요

```
Acquisition ─── 사용자 유입
    │
    ▼
Activation ──── 핵심 가치 경험 (검색 -> 상품 발견, 배송 추적 확인)
    │
    ▼
Retention ───── 재방문/재구매 (리뷰 작성, 재입고 알림, 사이즈 후기)
    │
    ▼
Revenue ─────── 매출 (주문 전환, 교환 vs 반품, 재고 가용성)
    │
    ▼
Referral ────── 추천/바이럴 (리뷰 공유, 포토 리뷰)
```

---

## 1. 배송 도메인 KPI

### 1.1 Activation

| 지표명 | 정의 | 측정 방법 | 목표 수치 | 측정 주기 |
|--------|------|----------|----------|----------|
| **배송 추적 조회율** | 주문 건 중 배송 추적을 1회 이상 조회한 비율 | `COUNT(DISTINCT shipping_id WHERE tracking_log > 0) / COUNT(shipping)` | >= 60% | 주간 |
| **배송 추적 평균 응답 시간** | 배송 추적 API P99 응답 시간 | Grafana: shipping_tracking_api_duration_p99 | < 500ms | 일간 |
| **송장 등록 완료율** | PAID 주문 건 중 24시간 내 송장 등록 완료 비율 | `COUNT(shipping WHERE created_at - order.paid_at < 24h) / COUNT(order WHERE status = 'PAID')` | >= 90% | 주간 |

### 1.2 Retention

| 지표명 | 정의 | 측정 방법 | 목표 수치 | 측정 주기 |
|--------|------|----------|----------|----------|
| **자동 구매확정 비율** | 전체 구매확정 중 자동(배치) 구매확정 비율 | `COUNT(auto_confirmed) / COUNT(confirmed)` | 60~80% | 주간 |
| **수동 구매확정 비율** | 구매자가 직접 구매확정한 비율 (배송 만족도 프록시) | `COUNT(manual_confirmed) / COUNT(confirmed)` | >= 20% | 주간 |
| **D-1 알림 후 수동 확인 전환율** | D-1 알림 수신 후 구매자가 직접 구매확정한 비율 | `COUNT(manual_confirmed WHERE d1_alert_sent = true) / COUNT(d1_alert_sent)` | >= 15% | 주간 |

### 1.3 Revenue

| 지표명 | 정의 | 측정 방법 | 목표 수치 | 측정 주기 |
|--------|------|----------|----------|----------|
| **반품률** | 배송 완료 건 중 반품 신청 비율 | `COUNT(return_request) / COUNT(shipping WHERE status = 'DELIVERED')` | < 10% | 주간 |
| **반품 사유별 분포** | 사유별 반품 비율 (DEFECTIVE/WRONG_ITEM/SIZE_MISMATCH/CHANGE_OF_MIND) | `GROUP BY reason` | SIZE_MISMATCH < 30% | 월간 |
| **반품 처리 평균 소요일** | 반품 신청부터 환불 완료까지 평균 일수 | `AVG(completed_at - requested_at)` | < 5영업일 | 주간 |
| **교환 전환율** | 반품 대신 교환을 선택한 비율 (매출 보존 지표) | `COUNT(exchange) / (COUNT(exchange) + COUNT(return))` | >= 20% | 월간 |
| **반품 자동 승인율** | 3영업일 초과 자동 승인 비율 (검수 효율성) | `COUNT(auto_approved) / COUNT(inspecting)` | < 10% | 월간 |

---

## 2. 재고 도메인 KPI

### 2.1 Activation

| 지표명 | 정의 | 측정 방법 | 목표 수치 | 측정 주기 |
|--------|------|----------|----------|----------|
| **재고 차감 성공률** | 주문 생성 시 재고 RESERVE 성공 비율 | `COUNT(reserve_success) / COUNT(reserve_attempt)` | >= 99% | 일간 |
| **재고 차감 P99 응답 시간** | 재고 차감 API P99 레이턴시 | Grafana: inventory_deduct_duration_p99 | < 200ms | 일간 |
| **분산 락 경합률** | 락 획득 실패 후 재시도가 필요했던 비율 | `COUNT(lock_retry) / COUNT(lock_attempt)` | < 5% | 일간 |

### 2.2 Revenue

| 지표명 | 정의 | 측정 방법 | 목표 수치 | 측정 주기 |
|--------|------|----------|----------|----------|
| **재고 부족 주문 거절률** | 재고 부족으로 주문이 거절된 비율 | `COUNT(inventory.insufficient) / COUNT(order.created)` | < 3% | 주간 |
| **SKU 품절률** | 전체 활성 SKU 중 재고 0인 비율 | `COUNT(available_quantity = 0) / COUNT(active_sku)` | < 5% | 일간 |
| **안전재고 도달 SKU 비율** | 안전재고 이하로 떨어진 SKU 비율 | `COUNT(available_quantity <= safety_stock) / COUNT(active_sku)` | < 15% | 일간 |
| **재고 회전율** | 월간 판매 수량 / 평균 재고 수량 | `SUM(monthly_sales) / AVG(total_quantity)` | >= 2.0 | 월간 |
| **결제 실패 재고 복구 성공률** | 결제 실패/주문 취소 시 재고 RELEASE 성공 비율 | `COUNT(release_success) / COUNT(release_attempt)` | 100% | 일간 |

### 2.3 Retention

| 지표명 | 정의 | 측정 방법 | 목표 수치 | 측정 주기 |
|--------|------|----------|----------|----------|
| **재입고 알림 신청율** | 품절 상품 조회 중 재입고 알림을 신청한 비율 | `COUNT(restock_notification) / COUNT(out_of_stock_view)` | >= 10% | 월간 |
| **재입고 알림 -> 구매 전환율** | 재입고 알림 수신 후 해당 상품을 구매한 비율 | `COUNT(purchased_after_restock_alert) / COUNT(restock_notified)` | >= 15% | 월간 |
| **재입고 알림 만료율** | 90일 만료되는 알림 비율 (상품 매력도 프록시) | `COUNT(expired) / COUNT(total_notification)` | < 30% | 월간 |

---

## 3. 검색 도메인 KPI

### 3.1 Acquisition

| 지표명 | 정의 | 측정 방법 | 목표 수치 | 측정 주기 |
|--------|------|----------|----------|----------|
| **검색 사용률** | 전체 세션 중 검색을 1회 이상 사용한 비율 | `COUNT(search_session) / COUNT(total_session)` | >= 40% | 주간 |
| **인기 검색어 클릭률** | 인기 검색어 노출 중 클릭 비율 | `COUNT(popular_keyword_click) / COUNT(popular_keyword_impression)` | >= 15% | 주간 |

### 3.2 Activation

| 지표명 | 정의 | 측정 방법 | 목표 수치 | 측정 주기 |
|--------|------|----------|----------|----------|
| **검색 결과 클릭률 (CTR)** | 검색 결과 노출 대비 상품 클릭 비율 | `COUNT(product_click_from_search) / COUNT(search_result_impression)` | >= 25% | 주간 |
| **제로 결과 비율** | 검색 결과가 0건인 검색 비율 | `COUNT(search WHERE result_count = 0) / COUNT(search)` | < 5% | 주간 |
| **자동완성 사용률** | 검색 중 자동완성을 선택한 비율 | `COUNT(autocomplete_selected) / COUNT(search)` | >= 30% | 주간 |
| **자동완성 P99 응답 시간** | 자동완성 API P99 레이턴시 | Grafana: search_autocomplete_duration_p99 | < 50ms | 일간 |
| **필터 사용률** | 검색 후 필터를 1개 이상 적용한 비율 | `COUNT(search_with_filter) / COUNT(search)` | >= 20% | 주간 |
| **인덱싱 지연 P95** | 상품 변경 -> ES 인덱스 반영까지 P95 소요 시간 | Grafana: search_indexing_lag_p95 | < 3초 | 일간 |

### 3.3 Revenue

| 지표명 | 정의 | 측정 방법 | 목표 수치 | 측정 주기 |
|--------|------|----------|----------|----------|
| **검색 -> 구매 전환율** | 검색 후 상품을 구매한 비율 | `COUNT(order_from_search) / COUNT(search_session)` | >= 5% | 주간 |
| **검색 매출 기여율** | 전체 매출 중 검색 경유 매출 비율 | `SUM(revenue_from_search) / SUM(total_revenue)` | >= 30% | 월간 |
| **평균 검색 깊이** | 검색 결과에서 상품 클릭 전 스크롤/페이지 이동 수 | `AVG(clicked_position)` | < 15 (첫 페이지 내) | 주간 |

### 3.4 Retention

| 지표명 | 정의 | 측정 방법 | 목표 수치 | 측정 주기 |
|--------|------|----------|----------|----------|
| **최근 검색어 재사용률** | 최근 검색어에서 동일 키워드를 재검색한 비율 | `COUNT(recent_keyword_reuse) / COUNT(total_search)` | >= 10% | 월간 |
| **검색 재방문율** | 검색 사용 후 7일 내 재방문한 사용자 비율 | `COUNT(revisit_within_7d WHERE searched = true) / COUNT(search_user)` | >= 40% | 월간 |

---

## 4. 리뷰 도메인 KPI

### 4.1 Activation

| 지표명 | 정의 | 측정 방법 | 목표 수치 | 측정 주기 |
|--------|------|----------|----------|----------|
| **리뷰 작성률** | 구매확정 건 중 리뷰를 작성한 비율 | `COUNT(review) / COUNT(order WHERE status = 'CONFIRMED')` | >= 15% | 월간 |
| **포토 리뷰 비율** | 전체 리뷰 중 포토 리뷰 비율 | `COUNT(review WHERE has_photo = 1) / COUNT(review)` | >= 30% | 월간 |
| **사이즈 후기 작성률** | 전체 리뷰 중 사이즈 정보를 입력한 비율 | `COUNT(review_size_info) / COUNT(review)` | >= 25% | 월간 |

### 4.2 Retention

| 지표명 | 정의 | 측정 방법 | 목표 수치 | 측정 주기 |
|--------|------|----------|----------|----------|
| **리뷰 조회율** | 상품 상세 페이지 방문 중 리뷰 섹션을 조회한 비율 | `COUNT(review_section_view) / COUNT(product_detail_view)` | >= 50% | 주간 |
| **리뷰 열람 -> 구매 전환율** | 리뷰를 열람한 사용자의 구매 전환율 | `COUNT(purchase_after_review_view) / COUNT(review_viewer)` | >= 8% | 월간 |
| **사이즈 필터 사용률** | "나와 비슷한 체형" 필터 사용 비율 | `COUNT(size_filter_used) / COUNT(review_section_view)` | >= 10% | 월간 |
| **리뷰 "도움이 됐어요" 비율** | 리뷰 조회 중 "도움이 됐어요"를 클릭한 비율 | `COUNT(helpful_click) / COUNT(review_view)` | >= 5% | 월간 |

### 4.3 Revenue

| 지표명 | 정의 | 측정 방법 | 목표 수치 | 측정 주기 |
|--------|------|----------|----------|----------|
| **리뷰 포인트 적립 총액** | 월간 리뷰 포인트 적립 합계 | `SUM(point WHERE reference_type = 'REVIEW')` | 모니터링 (상한 관리) | 월간 |
| **리뷰 포인트 일일 한도 도달률** | 일일 5,000P 한도에 도달한 회원 비율 | `COUNT(member WHERE daily_point >= 5000) / COUNT(active_member)` | < 1% | 주간 |
| **평균 별점** | 전체 상품 평균 별점 | `AVG(rating)` | 3.5~4.5 (건전한 분포) | 월간 |
| **별점 분포 엔트로피** | 별점 분포의 균등도 (조작 탐지) | Shannon entropy of rating distribution | > 1.5 | 월간 |

### 4.4 Referral

| 지표명 | 정의 | 측정 방법 | 목표 수치 | 측정 주기 |
|--------|------|----------|----------|----------|
| **포토 리뷰 공유율** | 포토 리뷰 조회 중 공유(복사/SNS) 비율 | `COUNT(review_share) / COUNT(photo_review_view)` | >= 2% | 월간 |
| **리뷰 기반 유입률** | 외부에서 리뷰 링크를 통해 유입된 트래픽 비율 | `COUNT(referral_from_review_link) / COUNT(total_referral)` | >= 5% | 월간 |

---

## 5. WMS 도메인 KPI (Phase 2.5)

### 5.1 Activation (운영 효율)

| 지표명 | 정의 | 측정 방법 | 목표 수치 | 측정 주기 |
|--------|------|----------|----------|----------|
| **입고 처리 평균 소요 시간** | ASN 등록부터 입고 확정까지 평균 시간 | `AVG(confirmed_at - created_at)` | < 4시간 | 주간 |
| **입고 검수 불합격률** | 입고 검수에서 불량/불합격 비율 | `COUNT(rejected_items) / COUNT(inspected_items)` | < 3% | 월간 |
| **피킹 완료율** | 생성된 피킹 태스크 중 정상 완료 비율 | `COUNT(picking_task WHERE status = 'COMPLETED') / COUNT(picking_task)` | >= 98% | 일간 |
| **피킹 SHORT 발생률** | 피킹 중 재고 부족(SHORT) 발생 비율 | `COUNT(picking_task WHERE status = 'SHORT') / COUNT(picking_task)` | < 2% | 주간 |
| **출고 처리 평균 소요 시간** | 출고 지시 생성부터 출고 완료까지 평균 시간 | `AVG(completed_at - created_at) for outbound_order` | < 6시간 | 주간 |

### 5.2 Revenue (비용 효율)

| 지표명 | 정의 | 측정 방법 | 목표 수치 | 측정 주기 |
|--------|------|----------|----------|----------|
| **재고 정확도** | 시스템 재고 vs 실사 재고 일치율 | `COUNT(matched_sku) / COUNT(stocktake_sku)` | >= 98% | 월간 |
| **재고 실사 차이 건수** | 실사 시 불일치 발견 SKU 수 | `COUNT(stocktake_result WHERE system_qty != actual_qty)` | 감소 추세 | 월간 |
| **로케이션 활용률** | 전체 로케이션 중 사용 중인 비율 | `COUNT(location WHERE used = true) / COUNT(location)` | 60~85% | 월간 |
| **반품 재입고 비율** | 반품 입고 중 양품 재입고(재판매 가능) 비율 | `COUNT(return_restocked) / COUNT(return_received)` | >= 80% | 월간 |

### 5.3 Retention (서비스 안정성)

| 지표명 | 정의 | 측정 방법 | 목표 수치 | 측정 주기 |
|--------|------|----------|----------|----------|
| **WMS -> inventory 이벤트 동기화 성공률** | wms.inbound.confirmed 등 이벤트 처리 성공률 | `COUNT(processed_success) / COUNT(event_received)` | >= 99.9% | 일간 |
| **출고 -> 배송 연동 지연** | 출고 완료 -> shipping-service 송장 등록까지 지연 | `AVG(shipping.created_at - outbound.completed_at)` | < 10분 | 일간 |
| **WMS API P99 응답 시간** | WMS 전체 API P99 레이턴시 | Grafana: wms_api_duration_p99 | < 500ms | 일간 |

---

## 6. 크로스 도메인 종합 KPI

### 6.1 End-to-End 경험

| 지표명 | 정의 | 측정 방법 | 목표 수치 | 측정 주기 |
|--------|------|----------|----------|----------|
| **주문 -> 배송 완료 평균 일수** | 주문 생성부터 배송 완료까지 평균 소요 일수 | `AVG(delivered_at - order.created_at)` | < 3일 | 주간 |
| **전체 주문 완료율** | 주문 생성 -> 구매확정까지 도달하는 비율 | `COUNT(confirmed) / COUNT(order_created)` | >= 85% | 월간 |
| **Phase 2 기능 채택률** | Phase 2 신규 기능(배송추적/검색/리뷰) 중 1개 이상 사용한 MAU 비율 | `COUNT(used_phase2_feature) / COUNT(MAU)` | >= 70% | 월간 |

### 6.2 시스템 안정성

| 지표명 | 정의 | 측정 방법 | 목표 수치 | 측정 주기 |
|--------|------|----------|----------|----------|
| **Kafka 이벤트 처리 성공률** | 전체 Kafka 이벤트 중 성공 처리 비율 | `COUNT(processed) / COUNT(consumed)` | >= 99.9% | 일간 |
| **DLQ 적재 건수** | Dead Letter Queue에 적재된 미처리 이벤트 수 | `COUNT(dlq_messages)` | < 10건/일 | 일간 |
| **전체 서비스 가용성** | Phase 2 서비스 4개 + WMS 합산 uptime | Grafana: service_uptime_ratio | >= 99.5% | 월간 |
| **Canary 롤백 횟수** | Phase 2 배포 기간 중 롤백 발생 횟수 | 배포 로그 | 0회 | 스프린트 |

### 6.3 비즈니스 임팩트

| 지표명 | 정의 | 측정 방법 | 목표 수치 | 측정 주기 |
|--------|------|----------|----------|----------|
| **재구매율** | 첫 구매 후 30일 내 재구매한 회원 비율 | `COUNT(repurchased_within_30d) / COUNT(first_purchase)` | >= 20% | 월간 |
| **NPS (Net Promoter Score)** | 구매 후 만족도 설문 (0-10점) | 인앱 설문 | >= 40 | 분기 |
| **CS 인입 증가율** | Phase 2 출시 전후 CS 인입량 변화 | `(CS_after - CS_before) / CS_before` | < 10% 증가 | 월간 |
| **검색 기반 매출 비중** | 전체 매출 중 검색 -> 구매 경로 비중 | Revenue attribution | >= 30% | 월간 |

---

## 측정 인프라

### 데이터 수집 계층

| 계층 | 도구 | 수집 대상 |
|------|------|----------|
| 서비스 메트릭 | Prometheus + Micrometer | API 레이턴시, 에러율, TPS |
| 시각화 | Grafana | 대시보드, 알림 |
| 비즈니스 이벤트 | Kafka 이벤트 로그 | 주문/배송/리뷰 이벤트 |
| 사용자 행동 | 서비스 로그 (JSON) | 검색 쿼리, 클릭, 필터 사용 |
| DB 집계 | 배치 쿼리 (매일 새벽) | 일간/주간/월간 집계 테이블 |

### 알림 임계값

| 지표 | WARNING | CRITICAL | 알림 채널 |
|------|---------|----------|----------|
| API 에러율 | > 1% | > 5% | Slack #alert |
| P99 레이턴시 | > 500ms | > 1000ms | Slack #alert |
| 재고 부족 거절률 | > 3% | > 10% | Slack #biz-alert |
| DLQ 적재 | > 5건/시간 | > 20건/시간 | Slack #alert + PagerDuty |
| Kafka 처리 실패율 | > 0.1% | > 1% | Slack #alert |
| ES 인덱싱 지연 | > 5초 | > 30초 | Slack #alert |
