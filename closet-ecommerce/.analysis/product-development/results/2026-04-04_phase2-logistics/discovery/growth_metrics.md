# Closet Phase 2 Growth Metrics 체계

> 작성일: 2026-04-04
> 프로젝트: Closet E-commerce Phase 2 (배송/재고/검색/리뷰)
> 프레임워크: AARRR (Pirate Metrics)

---

## 1. AARRR 지표 체계

### 1.1 Acquisition (획득)

사용자를 플랫폼으로 유입시키는 단계입니다. Phase 2에서는 검색 서비스(ES)와 SEO 최적화가 핵심 레버입니다.

#### 핵심 지표 (KPI)

| 지표 | 정의 | 측정 방법 | 월간 목표 |
|------|------|----------|----------|
| **DAU (일간 활성 사용자)** | 하루 1회 이상 방문한 고유 사용자 수 | `COUNT(DISTINCT member_id) FROM access_log WHERE date = ?` | 5,000명 |
| **검색 유입률** | 전체 세션 중 검색을 통해 시작된 세션 비율 | `search_session / total_session * 100` | 35% |
| **신규 가입률** | DAU 대비 신규 가입 비율 | `new_member_count / dau * 100` | 8% |

#### 보조 지표

| 지표 | 정의 | 측정 방법 | 월간 목표 |
|------|------|----------|----------|
| SEO 유입 세션 수 | 검색 엔진(Google/Naver)에서 유입된 세션 | UTM 파라미터 `utm_source=organic` 카운트 | 1,500/일 |
| 인기 검색어 클릭률 | 인기 검색어 노출 대비 클릭 비율 | `popular_keyword_click / popular_keyword_impression * 100` | 15% |
| 자동완성 사용률 | 검색 시 자동완성 추천을 선택한 비율 | `autocomplete_select / search_total * 100` | 40% |
| 바운스율 | 첫 페이지만 보고 이탈한 세션 비율 | `single_page_session / total_session * 100` | < 45% |

#### Phase 2 기능 연결
- **검색 서비스 (US-701~705)**: nori 형태소 분석 + 자동완성 + 인기 검색어로 검색 UX 향상 -> 검색 유입률 증가
- **ES 인덱싱 (US-701)**: 상품 데이터 실시간 인덱싱으로 SEO 크롤링 품질 향상

---

### 1.2 Activation (활성화)

가입한 사용자가 핵심 가치를 경험하는 단계입니다. Closet에서는 **첫 주문 완료**와 **첫 리뷰 작성**이 Aha Moment입니다.

#### 핵심 지표 (KPI)

| 지표 | 정의 | 측정 방법 | 월간 목표 |
|------|------|----------|----------|
| **첫 주문 전환율** | 가입 후 7일 내 첫 주문 완료한 사용자 비율 | `first_order_7d_member / new_member * 100` | 12% |
| **검색→주문 전환율** | 검색 세션 중 주문까지 도달한 비율 | `search_to_order_session / search_session * 100` | 3.5% |
| **첫 리뷰 작성률** | 첫 구매확정 후 30일 내 리뷰를 작성한 비율 | `first_review_member / first_confirmed_member * 100` | 25% |

#### 보조 지표

| 지표 | 정의 | 측정 방법 | 월간 목표 |
|------|------|----------|----------|
| 가입→첫 검색 시간 | 가입 후 첫 검색까지 소요 시간 (중앙값) | `MEDIAN(first_search_at - registered_at)` | < 5분 |
| 장바구니 추가율 | 상품 상세 조회 후 장바구니 추가 비율 | `cart_add / pdp_view * 100` | 8% |
| 결제 완료율 | 주문 생성 대비 결제 완료 비율 | `paid_order / created_order * 100` | 85% |
| 평균 세션 시간 | 한 세션의 평균 지속 시간 | `AVG(session_duration)` | 6분 |

#### Phase 2 기능 연결
- **검색 필터 (US-703)**: 카테고리/브랜드/가격/색상/사이즈 필터로 상품 탐색 마찰 감소 -> 장바구니 추가율 향상
- **리뷰 포인트 (US-803)**: 100P/300P/350P 적립으로 첫 리뷰 작성 동기 부여
- **사이즈 후기 (US-802)**: 구매 의사결정 보조 -> 첫 주문 전환율 향상

---

### 1.3 Retention (유지)

활성화된 사용자가 반복적으로 방문/구매하는 단계입니다. Phase 2에서는 재입고 알림과 리뷰 시스템이 핵심입니다.

#### 핵심 지표 (KPI)

| 지표 | 정의 | 측정 방법 | 월간 목표 |
|------|------|----------|----------|
| **30일 재구매율** | 첫 구매 후 30일 내 재구매한 사용자 비율 | `repurchase_30d_member / first_purchase_member * 100` | 18% |
| **WAU/MAU 비율** | 주간 활성 사용자 / 월간 활성 사용자 (스티키니스) | `wau / mau * 100` | 35% |
| **재입고 알림 전환율** | 재입고 알림 수신 후 24h 내 구매 전환 비율 | `restock_purchase / restock_notified * 100` | 20% |

#### 보조 지표

| 지표 | 정의 | 측정 방법 | 월간 목표 |
|------|------|----------|----------|
| D1/D7/D30 리텐션 | 가입 후 1/7/30일 재방문율 | 코호트 분석 | D1: 40%, D7: 25%, D30: 15% |
| 재입고 알림 신청 수 | 재입고 알림 신청 건수 | `COUNT(*) FROM restock_notification WHERE status = 'WAITING'` | 500건/월 |
| 리뷰 열람율 | 상품 상세 진입 후 리뷰 섹션 조회 비율 | `review_section_view / pdp_view * 100` | 60% |
| 구매확정 평균 소요일 | 배송 완료 후 구매확정까지 평균 일수 | `AVG(DATEDIFF(confirmed_at, delivered_at))` | < 4일 |

#### Phase 2 기능 연결
- **재입고 알림 (US-604)**: 품절 상품 재입고 시 즉시 알림 -> 재구매 촉진
- **배송 추적 (US-502)**: 실시간 배송 상태 확인으로 재방문 유도
- **리뷰 시스템 (US-801~804)**: 리뷰 열람으로 재방문 + 리뷰 작성으로 인게이지먼트 강화

---

### 1.4 Revenue (수익)

사용자가 결제하고 매출을 발생시키는 단계입니다. Phase 2에서는 결제 전환율과 배송비 수익이 핵심입니다.

#### 핵심 지표 (KPI)

| 지표 | 정의 | 측정 방법 | 월간 목표 |
|------|------|----------|----------|
| **AOV (평균 주문 금액)** | 주문 건당 평균 결제 금액 | `SUM(payment_amount) / COUNT(order_id)` | 55,000원 |
| **결제 전환율** | 장바구니 진입 대비 결제 완료 비율 | `paid_order / cart_checkout_start * 100` | 65% |
| **GMV (총 거래액)** | 월간 총 결제 금액 | `SUM(payment_amount) WHERE status = 'PAID'` | 2.7억원 |

#### 보조 지표

| 지표 | 정의 | 측정 방법 | 월간 목표 |
|------|------|----------|----------|
| 주문당 평균 상품 수 | 한 주문에 포함된 평균 상품 개수 | `AVG(item_count)` | 2.3개 |
| 반품률 | 총 주문 대비 반품 비율 | `return_count / order_count * 100` | < 5% |
| 배송비 수익 | 유료 배송비(3만원 미만 주문) 매출 | `SUM(shipping_fee) WHERE paid = true` | 1,200만원 |
| PG 수수료율 | 결제 금액 대비 PG 수수료 비율 | `SUM(pg_fee) / SUM(payment_amount) * 100` | < 2.5% |
| 환불률 | 결제 대비 환불 비율 | `refund_amount / payment_amount * 100` | < 3% |

#### Phase 2 기능 연결
- **다중 PG (Toss/Kakao/Naver/Danal)**: 결제 수단 다양화 -> 결제 전환율 향상
- **반품/교환 (US-504, US-505)**: 반품 배송비(3,000원/6,000원) 부과로 반품 억제 + 배송비 수익
- **사이즈 후기 (US-802)**: 정확한 사이즈 선택 유도 -> 사이즈 불일치 반품률 감소

---

### 1.5 Referral (추천)

기존 사용자가 새로운 사용자를 데려오는 단계입니다. Phase 2에서는 리뷰 공유가 핵심입니다.

#### 핵심 지표 (KPI)

| 지표 | 정의 | 측정 방법 | 월간 목표 |
|------|------|----------|----------|
| **리뷰 공유율** | 리뷰 작성자 중 SNS 공유한 비율 | `review_shared / review_created * 100` | 5% |
| **추천 코드 사용률** | 신규 가입 중 추천 코드를 사용한 비율 | `referral_signup / new_signup * 100` | 10% |
| **바이럴 계수 (K-factor)** | 한 사용자가 유입시키는 평균 신규 사용자 수 | `invitations_sent * conversion_rate` | 0.3 |

#### 보조 지표

| 지표 | 정의 | 측정 방법 | 월간 목표 |
|------|------|----------|----------|
| 포토 리뷰 비율 | 전체 리뷰 중 포토 리뷰 비율 | `photo_review / total_review * 100` | 35% |
| 리뷰 도움이 됐어요 클릭률 | 리뷰 노출 대비 도움이 됐어요 클릭 비율 | `helpful_click / review_impression * 100` | 8% |
| 사이즈 후기 작성률 | 리뷰 중 사이즈 정보를 포함한 비율 | `size_review / total_review * 100` | 45% |
| SNS 공유 클릭 수 | 상품/리뷰 SNS 공유 버튼 클릭 수 | 이벤트 카운트 | 300건/월 |

#### Phase 2 기능 연결
- **포토 리뷰 (US-801)**: 시각적 콘텐츠로 SNS 공유 유도
- **사이즈 후기 (US-802)**: 실용적 정보로 외부 공유 가치 제공
- **리뷰 포인트 (US-803)**: 300P/350P 인센티브로 리뷰 작성 촉진 -> 공유 가능 콘텐츠 증가

---

## 2. 이벤트 택소노미

Phase 2 기능에 필요한 전체 추적 이벤트를 정의합니다. 이벤트명은 `{도메인}.{액션}` 네이밍 규칙을 따릅니다.

### 2.1 검색 도메인 이벤트

| # | 이벤트명 | 트리거 조건 | 속성 (Properties) | 데이터 타입 |
|---|---------|-----------|-------------------|-----------|
| 1 | `search.query` | 사용자가 검색어를 입력하고 검색 실행 | `query: String`, `resultCount: Int`, `sort: String`, `sessionId: String` | String, Int, String, String |
| 2 | `search.result_click` | 검색 결과에서 상품 클릭 | `query: String`, `productId: Long`, `position: Int`, `resultCount: Int` | String, Long, Int, Int |
| 3 | `search.filter_apply` | 검색 필터 적용 | `query: String`, `filterType: String`, `filterValue: String`, `resultCount: Int` | String, String, String, Int |
| 4 | `search.autocomplete_show` | 자동완성 추천 목록 노출 | `prefix: String`, `suggestionCount: Int` | String, Int |
| 5 | `search.autocomplete_select` | 자동완성 추천 키워드 선택 | `prefix: String`, `selectedText: String`, `position: Int`, `type: String` | String, String, Int, String |
| 6 | `search.popular_keyword_click` | 인기 검색어 클릭 | `keyword: String`, `rank: Int` | String, Int |
| 7 | `search.no_result` | 검색 결과가 0건일 때 | `query: String`, `suggestion: String?` | String, String? |
| 8 | `search.sort_change` | 검색 결과 정렬 기준 변경 | `query: String`, `fromSort: String`, `toSort: String` | String, String, String |
| 9 | `search.page_view` | 검색 결과 페이지 이동 | `query: String`, `page: Int`, `size: Int` | String, Int, Int |

### 2.2 배송 도메인 이벤트

| # | 이벤트명 | 트리거 조건 | 속성 (Properties) | 데이터 타입 |
|---|---------|-----------|-------------------|-----------|
| 10 | `shipping.created` | 송장 등록 완료 | `shippingId: Long`, `orderId: Long`, `carrier: String`, `trackingNumber: String` | Long, Long, String, String |
| 11 | `shipping.tracking_view` | 배송 추적 페이지 조회 | `shippingId: Long`, `orderId: Long`, `currentStatus: String` | Long, Long, String |
| 12 | `shipping.status_changed` | 배송 상태 변경 | `shippingId: Long`, `orderId: Long`, `fromStatus: String`, `toStatus: String` | Long, Long, String, String |
| 13 | `shipping.delivered` | 배송 완료 | `shippingId: Long`, `orderId: Long`, `carrier: String`, `deliveryDays: Int` | Long, Long, String, Int |
| 14 | `shipping.auto_confirmed` | 자동 구매확정 (7일) | `orderId: Long`, `deliveredAt: DateTime`, `confirmedAt: DateTime` | Long, DateTime, DateTime |
| 15 | `shipping.manual_confirmed` | 수동 구매확정 | `orderId: Long`, `memberId: Long`, `daysAfterDelivery: Int` | Long, Long, Int |

### 2.3 반품/교환 도메인 이벤트

| # | 이벤트명 | 트리거 조건 | 속성 (Properties) | 데이터 타입 |
|---|---------|-----------|-------------------|-----------|
| 16 | `return.requested` | 반품 신청 | `returnId: Long`, `orderId: Long`, `orderItemId: Long`, `reason: String`, `shippingFeeBearer: String` | Long, Long, Long, String, String |
| 17 | `return.pickup_completed` | 반품 수거 완료 | `returnId: Long`, `orderId: Long` | Long, Long |
| 18 | `return.approved` | 반품 승인 (환불 처리) | `returnId: Long`, `orderId: Long`, `refundAmount: Int` | Long, Long, Int |
| 19 | `return.rejected` | 반품 거절 | `returnId: Long`, `orderId: Long`, `rejectReason: String` | Long, Long, String |
| 20 | `exchange.requested` | 교환 신청 | `exchangeId: Long`, `orderId: Long`, `orderItemId: Long`, `reason: String`, `exchangeOptionId: Long` | Long, Long, Long, String, Long |
| 21 | `exchange.completed` | 교환 완료 | `exchangeId: Long`, `orderId: Long`, `originalOptionId: Long`, `newOptionId: Long` | Long, Long, Long, Long |

### 2.4 재고 도메인 이벤트

| # | 이벤트명 | 트리거 조건 | 속성 (Properties) | 데이터 타입 |
|---|---------|-----------|-------------------|-----------|
| 22 | `inventory.deducted` | 재고 차감 (주문) | `inventoryId: Long`, `sku: String`, `quantity: Int`, `afterQuantity: Int`, `orderId: Long` | Long, String, Int, Int, Long |
| 23 | `inventory.restored` | 재고 복구 (취소/반품) | `inventoryId: Long`, `sku: String`, `quantity: Int`, `afterQuantity: Int`, `referenceType: String`, `referenceId: Long` | Long, String, Int, Int, String, Long |
| 24 | `inventory.low_stock` | 안전재고 이하로 감소 | `inventoryId: Long`, `sku: String`, `quantity: Int`, `safetyStock: Int`, `productId: Long` | Long, String, Int, Int, Long |
| 25 | `inventory.out_of_stock` | 재고 0 (품절) | `inventoryId: Long`, `sku: String`, `productId: Long`, `optionId: Long` | Long, String, Long, Long |
| 26 | `inventory.restocked` | 재입고 완료 | `inventoryId: Long`, `sku: String`, `quantity: Int`, `productId: Long` | Long, String, Int, Long |
| 27 | `inventory.restock_notification_requested` | 재입고 알림 신청 | `memberId: Long`, `productId: Long`, `optionId: Long` | Long, Long, Long |
| 28 | `inventory.restock_notification_sent` | 재입고 알림 발송 | `memberId: Long`, `productId: Long`, `optionId: Long`, `notifiedAt: DateTime` | Long, Long, Long, DateTime |

### 2.5 리뷰 도메인 이벤트

| # | 이벤트명 | 트리거 조건 | 속성 (Properties) | 데이터 타입 |
|---|---------|-----------|-------------------|-----------|
| 29 | `review.created` | 리뷰 작성 완료 | `reviewId: Long`, `memberId: Long`, `productId: Long`, `rating: Int`, `hasPhoto: Boolean`, `hasSizeInfo: Boolean`, `pointEarned: Int` | Long, Long, Long, Int, Boolean, Boolean, Int |
| 30 | `review.updated` | 리뷰 수정 | `reviewId: Long`, `memberId: Long`, `productId: Long`, `oldRating: Int`, `newRating: Int` | Long, Long, Long, Int, Int |
| 31 | `review.deleted` | 리뷰 삭제 | `reviewId: Long`, `memberId: Long`, `productId: Long`, `pointRevoked: Int` | Long, Long, Long, Int |
| 32 | `review.helpful_click` | 도움이 됐어요 클릭 | `reviewId: Long`, `productId: Long`, `clickedBy: Long` | Long, Long, Long |
| 33 | `review.photo_view` | 포토 리뷰 이미지 확대 조회 | `reviewId: Long`, `productId: Long`, `imageIndex: Int` | Long, Long, Int |
| 34 | `review.size_info_view` | 사이즈 후기 섹션 조회 | `productId: Long`, `memberId: Long` | Long, Long |
| 35 | `review.similar_body_filter` | "나와 비슷한 체형" 필터 적용 | `productId: Long`, `height: Int`, `weight: Int`, `resultCount: Int` | Long, Int, Int, Int |
| 36 | `review.share` | 리뷰 SNS 공유 | `reviewId: Long`, `productId: Long`, `platform: String` | Long, Long, String |
| 37 | `review.list_view` | 상품 상세에서 리뷰 리스트 조회 | `productId: Long`, `sort: String`, `photoOnly: Boolean`, `page: Int` | Long, String, Boolean, Int |

### 2.6 주문/결제 연관 이벤트

| # | 이벤트명 | 트리거 조건 | 속성 (Properties) | 데이터 타입 |
|---|---------|-----------|-------------------|-----------|
| 38 | `order.checkout_start` | 주문서(체크아웃) 페이지 진입 | `cartItemCount: Int`, `totalAmount: Int`, `memberId: Long` | Int, Int, Long |
| 39 | `order.pg_selected` | PG사(결제 수단) 선택 | `orderId: Long`, `pgProvider: String`, `paymentMethod: String` | Long, String, String |
| 40 | `order.payment_completed` | 결제 완료 | `orderId: Long`, `paymentId: Long`, `amount: Int`, `pgProvider: String`, `itemCount: Int` | Long, Long, Int, String, Int |
| 41 | `cart.add` | 장바구니 상품 추가 | `productId: Long`, `optionId: Long`, `quantity: Int`, `price: Int`, `source: String` | Long, Long, Int, Int, String |
| 42 | `cart.remove` | 장바구니 상품 제거 | `productId: Long`, `optionId: Long`, `quantity: Int` | Long, Long, Int |

### 2.7 상품 탐색 이벤트

| # | 이벤트명 | 트리거 조건 | 속성 (Properties) | 데이터 타입 |
|---|---------|-----------|-------------------|-----------|
| 43 | `product.view` | 상품 상세 페이지 조회 | `productId: Long`, `source: String`, `categoryId: Long?`, `brandId: Long?` | Long, String, Long?, Long? |
| 44 | `product.option_select` | 상품 옵션(사이즈/색상) 선택 | `productId: Long`, `optionId: Long`, `size: String`, `color: String` | Long, Long, String, String |
| 45 | `product.size_guide_view` | 사이즈 가이드 조회 | `productId: Long`, `memberId: Long` | Long, Long |

---

## 3. 대시보드 설계

### 3.1 경영진용 주간 리포트 대시보드

주 1회(매주 월요일 09:00) 자동 생성되는 요약 리포트입니다.

#### 레이아웃

```
┌──────────────────────────────────────────────────────────────┐
│                    Weekly Executive Summary                    │
│                   2026-W14 (03/30 ~ 04/05)                   │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐    │
│  │   GMV    │  │   DAU    │  │   AOV    │  │ 결제전환율 │    │
│  │  6,750만  │  │  4,820   │  │ 52,300원 │  │  62.3%   │    │
│  │ +12% WoW │  │ +8% WoW  │  │ +3% WoW  │  │ +1.5%p   │    │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘    │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ AARRR 퍼널 요약                                      │    │
│  │ Acquisition : 신규 가입 385명 (+15% WoW)             │    │
│  │ Activation  : 첫 주문 전환 11.2% (목표 12%)          │    │
│  │ Retention   : 30일 재구매율 16.8% (목표 18%)         │    │
│  │ Revenue     : GMV 6,750만 (목표 6,750만 ✅)          │    │
│  │ Referral    : 리뷰 공유율 4.2% (목표 5%)             │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌──────────────────────┐  ┌──────────────────────────┐    │
│  │  주간 GMV 트렌드      │  │  검색 vs 직접유입 비율    │    │
│  │  (7일 라인 차트)       │  │  (파이 차트)              │    │
│  └──────────────────────┘  └──────────────────────────┘    │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ 핵심 알림                                            │    │
│  │ - 반품률 4.8% (안정)                                  │    │
│  │ - 품절 SKU 23개 (전주 대비 +5개, 주의)                │    │
│  │ - PG 수수료율 2.3% (정상 범위)                        │    │
│  └─────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────┘
```

#### 지표 구성

| 섹션 | 지표 | 데이터 소스 | 갱신 주기 |
|------|------|-----------|----------|
| 상단 KPI 카드 | GMV, DAU, AOV, 결제 전환율 | MySQL (payment, access_log) | 일 1회 집계 |
| AARRR 요약 | 5단계 핵심 지표 + 목표 대비 달성률 | 전체 서비스 DB | 주 1회 |
| 트렌드 차트 | 일별 GMV/주문수 | MySQL (order, payment) | 일 1회 |
| 유입 경로 | 검색/직접/외부 비율 | access_log + UTM | 일 1회 |
| 핵심 알림 | 반품률, 품절 SKU, PG 수수료 | 계산 지표 | 일 1회 |

---

### 3.2 팀용 일일 운영 대시보드

운영팀이 실시간으로 모니터링하는 대시보드입니다.

#### 레이아웃

```
┌──────────────────────────────────────────────────────────────┐
│                   Daily Operations Dashboard                  │
├───────────────────────┬──────────────────────────────────────┤
│ 실시간 지표 (5분 갱신) │           오늘 누적 현황              │
│                       │                                      │
│  현재 접속자: 342명    │  주문: 287건 / 결제: 241건           │
│  검색 QPS: 45.2       │  GMV: 1,258만원                     │
│  결제 TPS: 3.8        │  신규가입: 58명                      │
│  API 평균 응답: 120ms │  리뷰 작성: 34건                     │
├───────────────────────┴──────────────────────────────────────┤
│                                                              │
│  ┌──────────────────────┐  ┌──────────────────────────┐    │
│  │  시간대별 주문 추이    │  │  PG사별 결제 현황         │    │
│  │  (막대 차트, 24h)     │  │  Toss: 52% Kakao: 28%   │    │
│  │                       │  │  Naver: 15% Danal: 5%   │    │
│  └──────────────────────┘  └──────────────────────────┘    │
│                                                              │
│  ┌──────────────────────┐  ┌──────────────────────────┐    │
│  │  택배사별 배송 현황    │  │  실시간 알림/이슈         │    │
│  │  CJ: 48건 배송중      │  │  ⚠ SKU-A102 재고 5개    │    │
│  │  로젠: 22건 배송중     │  │  ⚠ 반품 신청 +3건       │    │
│  │  롯데: 15건 배송중     │  │  ✓ 자동구매확정 12건     │    │
│  │  우체국: 8건 배송중    │  │                          │    │
│  └──────────────────────┘  └──────────────────────────┘    │
└──────────────────────────────────────────────────────────────┘
```

#### 지표 구성

| 섹션 | 지표 | 데이터 소스 | 갱신 주기 |
|------|------|-----------|----------|
| 실시간 | 동시 접속자, 검색 QPS, 결제 TPS, API 응답시간 | Redis, Prometheus | 5분 |
| 누적 현황 | 오늘 주문/결제/GMV/가입/리뷰 수 | MySQL 집계 | 5분 |
| 시간대별 추이 | 시간별 주문 건수 막대 차트 | MySQL (order) | 10분 |
| PG 현황 | PG사별 결제 건수/금액 비율 | MySQL (payment) | 10분 |
| 배송 현황 | 택배사별 배송중/완료 건수 | MySQL (shipping) | 10분 |
| 알림 | 저재고/품절/반품/자동확정 | Kafka 이벤트 | 실시간 |

---

### 3.3 도메인별 대시보드

#### 3.3.1 배송 대시보드

| 지표 | 정의 | SQL/소스 | 목표 |
|------|------|---------|------|
| 일 평균 배송 건수 | 당일 송장 등록 건수 | `SELECT COUNT(*) FROM shipping WHERE DATE(created_at) = CURDATE()` | 200건/일 |
| 평균 배송 소요일 | 송장 등록 ~ 배송 완료 평균 일수 | `AVG(DATEDIFF(delivered_at, shipped_at))` | < 2.5일 |
| 택배사별 배송 완료율 | 택배사별 배송 완료 비율 (3일 내) | `CASE carrier WHEN ... GROUP BY carrier` | > 85% |
| 자동 구매확정 비율 | 전체 구매확정 중 자동 비율 | `auto_confirmed / total_confirmed * 100` | 70% |
| 배송 추적 조회 횟수 | 일일 배송 추적 API 호출 수 | Redis 카운터 / API 로그 | 모니터링 |
| 배송 상태별 건수 | READY/IN_TRANSIT/DELIVERED 분포 | `GROUP BY status` | 모니터링 |

#### 3.3.2 재고 대시보드

| 지표 | 정의 | SQL/소스 | 목표 |
|------|------|---------|------|
| 품절 SKU 수 | 재고 0인 SKU 수 | `SELECT COUNT(*) FROM inventory WHERE quantity = 0` | < 20개 |
| 안전재고 이하 SKU 수 | 안전재고 미만 SKU 수 | `SELECT COUNT(*) FROM inventory WHERE quantity <= safety_stock AND quantity > 0` | < 50개 |
| 재고 회전율 | 월간 출고량 / 평균 재고 | `SUM(deduct_qty) / AVG(quantity)` | > 4회/월 |
| 재입고 알림 대기 건수 | WAITING 상태 알림 수 | `SELECT COUNT(*) FROM restock_notification WHERE status = 'WAITING'` | 모니터링 |
| 재입고 알림 전환율 | 알림 발송 후 24h 내 구매 비율 | 커스텀 집계 | 20% |
| 재고 차감 실패율 | 동시성 이슈 등으로 실패한 차감 비율 | 에러 로그 카운트 | < 0.1% |

#### 3.3.3 검색 대시보드

| 지표 | 정의 | SQL/소스 | 목표 |
|------|------|---------|------|
| 일 검색 건수 | 하루 검색 API 호출 수 | 이벤트 `search.query` 카운트 | 5,000건/일 |
| 검색 결과 클릭률 (CTR) | 검색 후 상품 클릭 비율 | `search.result_click / search.query * 100` | 35% |
| Zero Result Rate | 검색 결과 0건 비율 | `search.no_result / search.query * 100` | < 8% |
| 자동완성 사용률 | 자동완성 선택 비율 | `autocomplete_select / search_total * 100` | 40% |
| 검색→구매 전환율 | 검색에서 시작해 결제까지 완료한 비율 | 세션 기반 퍼널 | 3.5% |
| 평균 검색 응답 시간 | ES 검색 API 평균 응답 시간 | Prometheus `http_request_duration` | < 200ms |
| 인기 검색어 Top 10 | 실시간 인기 검색어 | Redis Sorted Set | 모니터링 |

#### 3.3.4 리뷰 대시보드

| 지표 | 정의 | SQL/소스 | 목표 |
|------|------|---------|------|
| 일 리뷰 작성 수 | 당일 작성된 리뷰 건수 | `SELECT COUNT(*) FROM review WHERE DATE(created_at) = CURDATE()` | 50건/일 |
| 포토 리뷰 비율 | 전체 리뷰 중 포토 리뷰 | `SUM(has_photo) / COUNT(*) * 100` | 35% |
| 사이즈 후기 작성률 | 리뷰 중 사이즈 정보 포함 비율 | `size_review_count / total_review * 100` | 45% |
| 평균 별점 | 전체 리뷰 평균 별점 | `AVG(rating)` | 4.0~4.5 |
| 리뷰 포인트 적립 총액 | 당일 리뷰 포인트 지급 총액 | Kafka `point.earn` 이벤트 집계 | 모니터링 |
| 리뷰 작성률 | 구매확정 대비 리뷰 작성 비율 | `review_count / confirmed_order * 100` | 30% |
| 도움이 됐어요 클릭률 | 리뷰 노출 대비 클릭 | `helpful_click / review_impression * 100` | 8% |

---

## 4. 지표 측정 인프라 권장 구성

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  Application │────>│    Kafka     │────>│   ClickHouse │
│  (이벤트 발행) │     │  (이벤트 버스) │     │  (이벤트 저장) │
└──────────────┘     └──────┬───────┘     └──────┬───────┘
                            │                     │
                     ┌──────▼───────┐     ┌──────▼───────┐
                     │    Flink     │     │   Grafana    │
                     │ (실시간 집계)  │     │  (대시보드)    │
                     └──────┬───────┘     └──────────────┘
                            │
                     ┌──────▼───────┐
                     │    Redis     │
                     │ (실시간 카운터)│
                     └──────────────┘
```

| 컴포넌트 | 역할 | Phase 2 적용 |
|---------|------|-------------|
| Kafka | 이벤트 발행/구독 (45개 이벤트) | 이미 구축 (docker-compose) |
| ClickHouse | 이벤트 영구 저장, 분석 쿼리 | 권장 (MySQL로 대체 가능) |
| Flink / Kafka Streams | 실시간 집계 (인기 검색어, 리뷰 통계) | 권장 (Spring Scheduler로 대체 가능) |
| Redis | 실시간 카운터, 캐시 (인기 검색어, 리뷰 집계) | 이미 구축 |
| Grafana | 대시보드 시각화 | 이미 구축 (Prometheus + Grafana) |

---

## 5. 지표 목표 종합 (Phase 2 완료 시점)

| AARRR | 핵심 KPI | Phase 2 이전 | Phase 2 목표 | 측정 시점 |
|-------|---------|-------------|-------------|----------|
| Acquisition | DAU | 2,000명 | 5,000명 | Phase 2 완료 +4주 |
| Acquisition | 검색 유입률 | 0% (미구현) | 35% | Phase 2 완료 +4주 |
| Activation | 첫 주문 전환율 | 8% | 12% | Phase 2 완료 +4주 |
| Activation | 검색→주문 전환율 | 0% (미구현) | 3.5% | Phase 2 완료 +4주 |
| Retention | 30일 재구매율 | 10% | 18% | Phase 2 완료 +8주 |
| Retention | 재입고 알림 전환율 | 0% (미구현) | 20% | Phase 2 완료 +4주 |
| Revenue | AOV | 48,000원 | 55,000원 | Phase 2 완료 +4주 |
| Revenue | 결제 전환율 | 58% | 65% | Phase 2 완료 +4주 |
| Revenue | GMV | 1.5억원/월 | 2.7억원/월 | Phase 2 완료 +4주 |
| Referral | 리뷰 공유율 | 0% (미구현) | 5% | Phase 2 완료 +8주 |
| Referral | 포토 리뷰 비율 | 0% (미구현) | 35% | Phase 2 완료 +8주 |
