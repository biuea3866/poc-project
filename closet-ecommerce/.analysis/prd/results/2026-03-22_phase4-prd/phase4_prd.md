# Phase 4 상세 PRD: 고도화 단계

> 작성일: 2026-03-22
> 프로젝트: Closet E-commerce
> Phase: 4 (고도화)
> 도메인: 정산 + 알림 + 콘텐츠 + AI

---

## 1. Phase 4 개요

### 목표
Phase 3에서 구축한 마켓플레이스 기반(프로모션, 전시, 셀러, CS) 위에 **정산 시스템, 알림 인프라, 콘텐츠 플랫폼, AI 개인화** 기능을 추가하여 플랫폼의 수익 구조를 완성하고 사용자 경험을 고도화한다. 셀러 생태계의 신뢰성(정산)과 사용자 리텐션(알림/콘텐츠/AI)이 핵심 목표이다.

### 기간
- 예상 기간: 10주 (Phase 3 완료 후)
- Sprint 14-18

### 서비스 구성
| 서비스 | 포트 | 기술 스택 |
|--------|------|----------|
| settlement-service | 8094 | Kotlin, Spring Boot 3.x, JPA, Spring Batch |
| notification-service | 8095 | Kotlin, Spring Boot 3.x, Kafka, Redis |
| content-service | 8096 | Kotlin, Spring Boot 3.x, JPA |
| ai-service | 8097 | Kotlin, Spring Boot 3.x, Redis, ML Model |

### Phase 4 KPI

| 지표 | 목표 | 측정 방법 |
|------|------|----------|
| 정산 정확도 | 99.99% | 정산 오류 건수 / 전체 정산 건수 |
| 정산 주기 준수율 | 100% | 예정 지급일 대비 실제 지급일 |
| 알림 도달률 | 이메일 95%, SMS 99%, 푸시 85% | 발송 성공 / 발송 시도 |
| 재입고 알림 전환율 | 알림 수신 대비 20% 구매 | 알림 수신자 중 7일 내 구매 |
| 콘텐츠 페이지 체류 시간 | 평균 3분 이상 | 매거진/코디 페이지 체류 |
| AI 추천 클릭률 | 추천 노출 대비 10% | 추천 상품 노출 / 클릭 |
| OOTD 게시물 작성률 | MAU 대비 5% | 월 게시물 수 / MAU |

---

## 2. 정산 도메인 (settlement-service)

### US-1301: 정산 대상 집계

**As a** 시스템
**I want to** 구매확정된 주문을 일간 배치로 정산 대상에 집계하고 싶다
**So that** 셀러에게 정확한 금액을 지급할 수 있다

#### Acceptance Criteria
- [ ] 구매확정(`CONFIRMED`) 상태의 주문 건을 정산 대상으로 집계한다
- [ ] 집계 기준: `order.confirmed` Kafka 이벤트를 수신하여 정산 대상 테이블에 적재한다
- [ ] 일간 배치 (매일 02:00): 전일 구매확정 건을 정산 대상으로 확정한다
- [ ] 정산 대상 데이터: 주문 ID, 셀러 ID, 상품 ID, 판매 금액, 구매확정일
- [ ] 이미 집계된 주문 건은 중복 적재하지 않는다 (멱등성 보장)
- [ ] 반품/환불이 완료된 건은 정산 대상에서 차감한다
- [ ] 정산 대상 집계 완료 시 `settlement.aggregated` 이벤트를 발행한다
- [ ] Spring Batch를 사용하여 대량 데이터를 청크 단위(1000건)로 처리한다

#### 데이터 모델
```sql
CREATE TABLE settlement_item (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    order_id            BIGINT          NOT NULL COMMENT '주문 ID',
    order_item_id       BIGINT          NOT NULL COMMENT '주문 상품 ID',
    seller_id           BIGINT          NOT NULL COMMENT '셀러 ID',
    product_id          BIGINT          NOT NULL COMMENT '상품 ID',
    product_name        VARCHAR(200)    NOT NULL COMMENT '상품명',
    quantity            INT             NOT NULL COMMENT '수량',
    sale_amount         INT             NOT NULL COMMENT '판매 금액',
    discount_amount     INT             NOT NULL DEFAULT 0 COMMENT '할인 금액',
    coupon_amount       INT             NOT NULL DEFAULT 0 COMMENT '쿠폰 할인 금액',
    shipping_fee        INT             NOT NULL DEFAULT 0 COMMENT '배송비',
    net_amount          INT             NOT NULL COMMENT '순 매출 (판매-할인-쿠폰+배송비)',
    confirmed_at        DATETIME(6)     NOT NULL COMMENT '구매확정일',
    settlement_id       BIGINT          NULL COMMENT '정산서 ID (연결 후)',
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '상태: PENDING, SETTLED, REFUNDED',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_item (order_item_id),
    INDEX idx_seller_id (seller_id),
    INDEX idx_status (status),
    INDEX idx_confirmed_at (confirmed_at),
    INDEX idx_settlement_id (settlement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='정산 대상 항목';
```

#### API 스펙
```
GET /api/v1/settlements/items?sellerId={sellerId}&status={status}&startDate={start}&endDate={end}&page={page}&size={size}
Authorization: Bearer {token} (admin/seller)

Response: 200 OK
{
    "totalCount": 150,
    "totalNetAmount": 12500000,
    "items": [
        {
            "id": 1,
            "orderId": 12345,
            "productName": "오버핏 티셔츠 (BLACK/M)",
            "quantity": 2,
            "saleAmount": 79800,
            "discountAmount": 0,
            "couponAmount": 5000,
            "shippingFee": 3000,
            "netAmount": 77800,
            "confirmedAt": "2026-03-22T00:00:00",
            "status": "PENDING"
        }
    ]
}

POST /api/v1/settlements/aggregate
Authorization: Bearer {token} (admin)

-- 수동 집계 트리거 (배치 외 긴급 집계 시)
Response: 202 Accepted
{
    "message": "Settlement aggregation started",
    "targetDate": "2026-03-22"
}
```

---

### US-1302: 수수료 계산

**As a** 시스템
**I want to** 정산 대상에 대해 카테고리별 수수료를 자동 계산하고 싶다
**So that** 정확한 수수료를 차감한 정산 금액을 산출할 수 있다

#### Acceptance Criteria
- [ ] 수수료율은 카테고리별로 차등 적용한다 (10%~30%)
- [ ] 기본 수수료율: 상의 15%, 하의 15%, 아우터 12%, 신발 18%, 액세서리 20%
- [ ] 셀러별 개별 수수료율이 설정된 경우 개별 수수료율을 우선 적용한다
- [ ] 수수료 계산: 순 매출 x 수수료율 = 수수료 금액
- [ ] 수수료 금액은 원 단위 절사 (Math.floor)
- [ ] 수수료 정책 변경 이력을 관리한다
- [ ] 수수료율 변경은 변경일 이후 구매확정 건부터 적용된다

#### 데이터 모델
```sql
CREATE TABLE commission_policy (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    category_code       VARCHAR(30)     NOT NULL COMMENT '카테고리 코드',
    commission_rate     DECIMAL(5,2)    NOT NULL COMMENT '수수료율 (%)',
    is_active           TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '활성 여부',
    effective_from      DATETIME(6)     NOT NULL COMMENT '적용 시작일',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_category (category_code, is_active),
    INDEX idx_effective (effective_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='수수료 정책';

CREATE TABLE seller_commission (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    seller_id           BIGINT          NOT NULL COMMENT '셀러 ID',
    category_code       VARCHAR(30)     NULL COMMENT '카테고리 코드 (NULL=전체)',
    commission_rate     DECIMAL(5,2)    NOT NULL COMMENT '개별 수수료율 (%)',
    is_active           TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '활성 여부',
    effective_from      DATETIME(6)     NOT NULL COMMENT '적용 시작일',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_seller (seller_id, is_active),
    INDEX idx_effective (effective_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='셀러 개별 수수료';
```

#### API 스펙
```
GET /api/v1/settlements/commission-policies
Authorization: Bearer {token} (admin)

Response: 200 OK
{
    "items": [
        { "categoryCode": "TOP", "categoryName": "상의", "commissionRate": 15.0, "effectiveFrom": "2026-01-01T00:00:00" },
        { "categoryCode": "BOTTOM", "categoryName": "하의", "commissionRate": 15.0, "effectiveFrom": "2026-01-01T00:00:00" },
        { "categoryCode": "OUTER", "categoryName": "아우터", "commissionRate": 12.0, "effectiveFrom": "2026-01-01T00:00:00" },
        { "categoryCode": "SHOES", "categoryName": "신발", "commissionRate": 18.0, "effectiveFrom": "2026-01-01T00:00:00" },
        { "categoryCode": "ACCESSORY", "categoryName": "액세서리", "commissionRate": 20.0, "effectiveFrom": "2026-01-01T00:00:00" }
    ]
}

PUT /api/v1/settlements/commission-policies/{categoryCode}
Authorization: Bearer {token} (admin)
Content-Type: application/json

Request:
{
    "commissionRate": 13.0,
    "effectiveFrom": "2026-04-01T00:00:00"
}

Response: 200 OK
{
    "categoryCode": "OUTER",
    "commissionRate": 13.0,
    "effectiveFrom": "2026-04-01T00:00:00"
}

PUT /api/v1/settlements/sellers/{sellerId}/commission
Authorization: Bearer {token} (admin)
Content-Type: application/json

Request:
{
    "categoryCode": null,
    "commissionRate": 10.0,
    "effectiveFrom": "2026-04-01T00:00:00"
}

Response: 200 OK
{
    "sellerId": 1,
    "commissionRate": 10.0,
    "effectiveFrom": "2026-04-01T00:00:00"
}
```

---

### US-1303: 정산서 생성

**As a** 시스템
**I want to** 주간 단위로 셀러별 정산서를 자동 생성하고 싶다
**So that** 셀러가 정산 내역을 확인하고 지급을 받을 수 있다

#### Acceptance Criteria
- [ ] 주간 배치 (매주 월요일 03:00): 전주 정산 대상을 셀러별로 집계하여 정산서를 생성한다
- [ ] 정산서 구성: 총 매출 - 수수료 - 환불 = 정산액
- [ ] 정산서 항목별 상세: 상품별 매출, 수수료, 환불 내역
- [ ] 정산서 상태: `CREATED`(생성) → `CONFIRMED`(확정) → `PAID`(지급 완료)
- [ ] 셀러가 정산서를 확인(열람)할 수 있다
- [ ] 정산서 생성 시 `settlement.created` 이벤트를 발행한다
- [ ] 정산서 PDF 다운로드 기능 (향후)
- [ ] 환불 건은 해당 정산 주기의 정산서에서 차감한다

#### 데이터 모델
```sql
CREATE TABLE settlement (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    seller_id           BIGINT          NOT NULL COMMENT '셀러 ID',
    settlement_number   VARCHAR(50)     NOT NULL COMMENT '정산서 번호',
    period_start        DATE            NOT NULL COMMENT '정산 기간 시작일',
    period_end          DATE            NOT NULL COMMENT '정산 기간 종료일',
    total_sale_amount   BIGINT          NOT NULL DEFAULT 0 COMMENT '총 매출',
    total_commission    BIGINT          NOT NULL DEFAULT 0 COMMENT '총 수수료',
    total_refund_amount BIGINT          NOT NULL DEFAULT 0 COMMENT '총 환불',
    settlement_amount   BIGINT          NOT NULL DEFAULT 0 COMMENT '정산액 (매출-수수료-환불)',
    item_count          INT             NOT NULL DEFAULT 0 COMMENT '정산 항목 수',
    status              VARCHAR(20)     NOT NULL DEFAULT 'CREATED' COMMENT '상태',
    paid_at             DATETIME(6)     NULL COMMENT '지급일',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_settlement_number (settlement_number),
    INDEX idx_seller_id (seller_id),
    INDEX idx_status (status),
    INDEX idx_period (period_start, period_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='정산서';

CREATE TABLE settlement_detail (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    settlement_id       BIGINT          NOT NULL COMMENT '정산서 ID',
    settlement_item_id  BIGINT          NOT NULL COMMENT '정산 항목 ID',
    category_code       VARCHAR(30)     NOT NULL COMMENT '카테고리',
    sale_amount         INT             NOT NULL COMMENT '매출',
    commission_rate     DECIMAL(5,2)    NOT NULL COMMENT '적용 수수료율',
    commission_amount   INT             NOT NULL COMMENT '수수료 금액',
    refund_amount       INT             NOT NULL DEFAULT 0 COMMENT '환불 금액',
    net_settlement      INT             NOT NULL COMMENT '순 정산액',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_settlement_id (settlement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='정산서 상세';
```

#### API 스펙
```
GET /api/v1/settlements?sellerId={sellerId}&status={status}&page={page}&size={size}
Authorization: Bearer {token} (admin/seller)

Response: 200 OK
{
    "totalCount": 12,
    "items": [
        {
            "id": 1,
            "settlementNumber": "STL-2026-W12-001",
            "periodStart": "2026-03-16",
            "periodEnd": "2026-03-22",
            "totalSaleAmount": 2500000,
            "totalCommission": 375000,
            "totalRefundAmount": 50000,
            "settlementAmount": 2075000,
            "itemCount": 85,
            "status": "CREATED",
            "createdAt": "2026-03-23T03:00:00"
        }
    ]
}

GET /api/v1/settlements/{settlementId}
Authorization: Bearer {token} (admin/seller)

Response: 200 OK
{
    "id": 1,
    "settlementNumber": "STL-2026-W12-001",
    "sellerName": "스트리트 브랜드",
    "periodStart": "2026-03-16",
    "periodEnd": "2026-03-22",
    "totalSaleAmount": 2500000,
    "totalCommission": 375000,
    "totalRefundAmount": 50000,
    "settlementAmount": 2075000,
    "status": "CREATED",
    "details": [
        {
            "productName": "오버핏 티셔츠",
            "categoryCode": "TOP",
            "saleAmount": 399000,
            "commissionRate": 15.0,
            "commissionAmount": 59850,
            "refundAmount": 0,
            "netSettlement": 339150
        }
    ]
}
```

---

### US-1304: 정산 지급

**As a** 운영 관리자
**I want to** 확정된 정산서에 대해 주 1회 정산금을 지급하고 싶다
**So that** 셀러가 판매 대금을 정기적으로 수령할 수 있다

#### Acceptance Criteria
- [ ] 정산 지급일: 매주 수요일 (정산서 확정 후)
- [ ] 관리자가 정산서를 확인하고 `CONFIRMED` 상태로 전환한다
- [ ] 확정된 정산서에 대해 지급 처리를 실행한다
- [ ] 지급 상태 관리: `CONFIRMED` → `PAYING`(지급중) → `PAID`(지급 완료) / `FAILED`(지급 실패)
- [ ] 지급 실패 시 재시도(최대 3회) 후 관리자에게 알림
- [ ] 셀러의 정산 계좌 정보가 등록되어 있어야 지급 가능하다
- [ ] 정산 지급 완료 시 `settlement.paid` 이벤트를 발행한다
- [ ] 셀러에게 정산 완료 알림을 발송한다

#### API 스펙
```
POST /api/v1/settlements/{settlementId}/confirm
Authorization: Bearer {token} (admin)

Response: 200 OK
{
    "settlementId": 1,
    "status": "CONFIRMED",
    "confirmedAt": "2026-03-24T10:00:00"
}

POST /api/v1/settlements/{settlementId}/pay
Authorization: Bearer {token} (admin)

Response: 200 OK
{
    "settlementId": 1,
    "status": "PAID",
    "settlementAmount": 2075000,
    "paidAt": "2026-03-25T10:00:00"
}

POST /api/v1/settlements/batch-pay
Authorization: Bearer {token} (admin)
Content-Type: application/json

Request:
{
    "settlementIds": [1, 2, 3, 4, 5]
}

Response: 200 OK
{
    "totalCount": 5,
    "successCount": 5,
    "failedCount": 0,
    "totalPaidAmount": 15250000,
    "results": [
        { "settlementId": 1, "status": "PAID", "amount": 2075000 },
        { "settlementId": 2, "status": "PAID", "amount": 3200000 }
    ]
}
```

---

### US-1305: 정산 리포트

**As a** 셀러/관리자
**I want to** 기간별/셀러별 정산 현황을 조회하고 싶다
**So that** 매출과 정산 이력을 파악하고 경영 의사결정에 활용할 수 있다

#### Acceptance Criteria
- [ ] 셀러별 정산 이력 조회 (기간별 필터)
- [ ] 월별 정산 요약: 총 매출, 총 수수료, 총 환불, 총 정산액
- [ ] 카테고리별 매출 비중 분석
- [ ] 전체 셀러 정산 현황 요약 (관리자용)
- [ ] 정산 리포트 엑셀 다운로드 기능
- [ ] 전월 대비 증감률 표시

#### API 스펙
```
GET /api/v1/settlements/reports/seller?sellerId={sellerId}&startDate={start}&endDate={end}&unit={unit}
Authorization: Bearer {token} (admin/seller)

Parameters:
- unit: WEEK (기본), MONTH

Response: 200 OK
{
    "sellerId": 1,
    "sellerName": "스트리트 브랜드",
    "period": { "startDate": "2026-01-01", "endDate": "2026-03-22" },
    "summary": {
        "totalSaleAmount": 45000000,
        "totalCommission": 6750000,
        "totalRefundAmount": 1500000,
        "totalSettlement": 36750000,
        "monthOverMonthGrowth": 12.5
    },
    "timeline": [
        { "period": "2026-W10", "saleAmount": 2500000, "commission": 375000, "refund": 50000, "settlement": 2075000 },
        { "period": "2026-W11", "saleAmount": 2800000, "commission": 420000, "refund": 80000, "settlement": 2300000 }
    ],
    "categoryBreakdown": [
        { "categoryCode": "TOP", "categoryName": "상의", "saleAmount": 20000000, "ratio": 44.4 },
        { "categoryCode": "BOTTOM", "categoryName": "하의", "saleAmount": 15000000, "ratio": 33.3 },
        { "categoryCode": "OUTER", "categoryName": "아우터", "saleAmount": 10000000, "ratio": 22.3 }
    ]
}

GET /api/v1/settlements/reports/overview?startDate={start}&endDate={end}
Authorization: Bearer {token} (admin)

Response: 200 OK
{
    "period": { "startDate": "2026-03-01", "endDate": "2026-03-22" },
    "totalSaleAmount": 150000000,
    "totalCommission": 22500000,
    "totalRefundAmount": 5000000,
    "totalSettlement": 122500000,
    "activeSellers": 45,
    "settledCount": 180,
    "pendingCount": 15
}

GET /api/v1/settlements/reports/export?sellerId={sellerId}&startDate={start}&endDate={end}
Authorization: Bearer {token} (admin/seller)

Response: 200 OK (application/vnd.openxmlformats-officedocument.spreadsheetml.sheet)
-- 엑셀 파일 다운로드
```

---

## 3. 알림 도메인 (notification-service)

### US-1401: 주문/배송 알림

**As a** 구매자
**I want to** 주문 상태가 변경될 때마다 이메일/SMS/푸시 알림을 받고 싶다
**So that** 주문 진행 상황을 실시간으로 파악할 수 있다

#### Acceptance Criteria
- [ ] 알림 트리거 이벤트: `order.created`(주문 완료), `order.status.changed`(상태 변경), `order.confirmed`(구매 확정)
- [ ] 알림 채널: 이메일, SMS, 앱 푸시 (회원 설정에 따라)
- [ ] 각 이벤트별 알림 템플릿을 사용한다
- [ ] Kafka 이벤트를 수신하여 비동기로 알림을 발송한다
- [ ] 알림 발송 실패 시 재시도 (최대 3회, 지수 백오프)
- [ ] 알림 발송 이력을 저장한다 (발송 시간, 채널, 상태)
- [ ] 야간 시간(22:00~08:00) 마케팅 알림은 발송하지 않는다 (주문/배송 알림은 예외)

#### 데이터 모델
```sql
CREATE TABLE notification (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    member_id           BIGINT          NOT NULL COMMENT '수신자 ID',
    type                VARCHAR(30)     NOT NULL COMMENT '알림 유형: ORDER, SHIPPING, MARKETING, RESTOCK, SETTLEMENT',
    channel             VARCHAR(20)     NOT NULL COMMENT '채널: EMAIL, SMS, PUSH',
    template_code       VARCHAR(50)     NOT NULL COMMENT '템플릿 코드',
    title               VARCHAR(200)    NOT NULL COMMENT '알림 제목',
    content             TEXT            NOT NULL COMMENT '알림 내용',
    reference_type      VARCHAR(30)     NULL COMMENT '참조 유형: ORDER, PRODUCT, SETTLEMENT',
    reference_id        BIGINT          NULL COMMENT '참조 ID',
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '상태: PENDING, SENT, FAILED, READ',
    sent_at             DATETIME(6)     NULL COMMENT '발송 일시',
    read_at             DATETIME(6)     NULL COMMENT '읽은 일시',
    retry_count         INT             NOT NULL DEFAULT 0 COMMENT '재시도 횟수',
    error_message       VARCHAR(500)    NULL COMMENT '에러 메시지',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_member_id (member_id),
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_reference (reference_type, reference_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='알림';
```

#### API 스펙
```
GET /api/v1/notifications/my?type={type}&page={page}&size={size}
Authorization: Bearer {token}

Response: 200 OK
{
    "totalCount": 25,
    "unreadCount": 3,
    "items": [
        {
            "id": 1,
            "type": "ORDER",
            "title": "주문이 완료되었습니다",
            "content": "주문번호 ORD-2026032200001의 결제가 완료되었습니다. 빠른 시일 내에 발송해 드리겠습니다.",
            "referenceType": "ORDER",
            "referenceId": 12345,
            "status": "READ",
            "sentAt": "2026-03-22T10:00:00",
            "readAt": "2026-03-22T10:05:00"
        },
        {
            "id": 2,
            "type": "SHIPPING",
            "title": "상품이 발송되었습니다",
            "content": "주문하신 상품이 CJ대한통운으로 발송되었습니다. 송장번호: 1234567890123",
            "referenceType": "ORDER",
            "referenceId": 12345,
            "status": "SENT",
            "sentAt": "2026-03-22T14:00:00"
        }
    ]
}

POST /api/v1/notifications/{notificationId}/read
Authorization: Bearer {token}

Response: 204 No Content

POST /api/v1/notifications/read-all
Authorization: Bearer {token}

Response: 204 No Content
```

---

### US-1402: 재입고 알림

**As a** 구매자
**I want to** 품절된 상품이 재입고되면 즉시 알림을 받고 싶다
**So that** 원하는 상품을 놓치지 않고 구매할 수 있다

#### Acceptance Criteria
- [ ] `inventory.restocked` Kafka 이벤트를 수신하여 재입고 알림을 발송한다
- [ ] 알림 대상: restock_notification 테이블에서 해당 상품/옵션의 `WAITING` 상태 구독자 조회
- [ ] 알림 채널: 앱 푸시 + 이메일 동시 발송
- [ ] 알림 발송 후 구독 상태를 `NOTIFIED`로 변경한다
- [ ] 대량 재입고 시 알림 발송을 청크 단위(100건)로 분산 처리한다
- [ ] 재입고 알림 발송 후 7일 내 구매 전환율을 추적한다
- [ ] 알림에 상품 상세 페이지 딥링크를 포함한다

#### API 스펙
```
-- 재입고 알림 신청은 Phase 2 inventory-service에서 제공 (US-604)
-- notification-service는 이벤트 수신 후 발송만 담당

-- 내부 발송 처리 (이벤트 기반, 외부 API 없음)
-- Kafka Consumer: inventory.restocked → 알림 발송
```

---

### US-1403: 마케팅 알림

**As a** 운영 관리자
**I want to** 쿠폰 발급, 타임세일 시작 등 마케팅 이벤트를 회원에게 알리고 싶다
**So that** 프로모션 참여율과 매출을 높일 수 있다

#### Acceptance Criteria
- [ ] 마케팅 알림 트리거: `timesale.started`(타임세일 시작), `coupon.issued`(쿠폰 발급), `exhibition.started`(기획전 시작)
- [ ] 마케팅 알림 수신 동의한 회원에게만 발송한다
- [ ] 야간(22:00~08:00) 마케팅 알림은 발송하지 않는다 (예약 발송: 08:00)
- [ ] 대량 발송 시 발송 속도 제어 (초당 100건)
- [ ] 마케팅 알림 CTR 추적 (알림 → 페이지 방문)
- [ ] 특정 회원 그룹(세그먼트)에게만 발송하는 타겟팅 기능

#### 데이터 모델
```sql
CREATE TABLE marketing_campaign (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    title               VARCHAR(200)    NOT NULL COMMENT '캠페인 제목',
    type                VARCHAR(30)     NOT NULL COMMENT '유형: TIMESALE, COUPON, EXHIBITION, CUSTOM',
    channel             VARCHAR(20)     NOT NULL COMMENT '채널: EMAIL, SMS, PUSH, ALL',
    template_code       VARCHAR(50)     NOT NULL COMMENT '템플릿 코드',
    target_segment      VARCHAR(100)    NULL COMMENT '타겟 세그먼트 (NULL=전체)',
    scheduled_at        DATETIME(6)     NULL COMMENT '예약 발송 시간',
    total_target        INT             NOT NULL DEFAULT 0 COMMENT '발송 대상 수',
    sent_count          INT             NOT NULL DEFAULT 0 COMMENT '발송 완료 수',
    open_count          INT             NOT NULL DEFAULT 0 COMMENT '열람 수',
    click_count         INT             NOT NULL DEFAULT 0 COMMENT '클릭 수',
    status              VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '상태: DRAFT, SCHEDULED, SENDING, COMPLETED',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_status (status),
    INDEX idx_scheduled (scheduled_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='마케팅 캠페인';
```

#### API 스펙
```
POST /api/v1/notifications/campaigns
Authorization: Bearer {token} (admin)
Content-Type: application/json

Request:
{
    "title": "타임세일 시작 알림",
    "type": "TIMESALE",
    "channel": "PUSH",
    "templateCode": "TIMESALE_START",
    "targetSegment": "ALL",
    "scheduledAt": "2026-04-01T12:00:00"
}

Response: 201 Created
{
    "id": 1,
    "title": "타임세일 시작 알림",
    "status": "SCHEDULED",
    "scheduledAt": "2026-04-01T12:00:00",
    "totalTarget": 50000
}

GET /api/v1/notifications/campaigns?status={status}&page={page}&size={size}
Authorization: Bearer {token} (admin)

Response: 200 OK
{
    "totalCount": 10,
    "items": [
        {
            "id": 1,
            "title": "타임세일 시작 알림",
            "type": "TIMESALE",
            "channel": "PUSH",
            "totalTarget": 50000,
            "sentCount": 50000,
            "openCount": 15000,
            "clickCount": 5000,
            "openRate": 30.0,
            "clickRate": 10.0,
            "status": "COMPLETED"
        }
    ]
}
```

---

### US-1404: 알림 템플릿 관리

**As a** 운영 관리자
**I want to** 알림 메시지 템플릿을 관리하고 싶다
**So that** 일관된 메시지를 발송하고 내용을 쉽게 변경할 수 있다

#### Acceptance Criteria
- [ ] 채널별(이메일/SMS/푸시) 템플릿을 관리한다
- [ ] 템플릿에 변수 바인딩을 지원한다 (예: `{{memberName}}`, `{{orderNumber}}`, `{{productName}}`)
- [ ] 이메일 템플릿: HTML 형식, 제목 + 본문
- [ ] SMS 템플릿: 텍스트 형식, 최대 90자 (한글 기준)
- [ ] 푸시 템플릿: 제목(최대 30자) + 본문(최대 100자) + 이미지 URL(선택)
- [ ] 템플릿 미리보기 기능
- [ ] 템플릿 수정 이력 관리

#### 데이터 모델
```sql
CREATE TABLE notification_template (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    code                VARCHAR(50)     NOT NULL COMMENT '템플릿 코드',
    name                VARCHAR(100)    NOT NULL COMMENT '템플릿명',
    channel             VARCHAR(20)     NOT NULL COMMENT '채널: EMAIL, SMS, PUSH',
    title_template      VARCHAR(200)    NOT NULL COMMENT '제목 템플릿',
    body_template       TEXT            NOT NULL COMMENT '본문 템플릿',
    image_url           VARCHAR(500)    NULL COMMENT '이미지 URL (푸시용)',
    variables           VARCHAR(500)    NULL COMMENT '사용 변수 목록 (JSON)',
    is_active           TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '활성 여부',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_code_channel (code, channel),
    INDEX idx_channel (channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='알림 템플릿';
```

#### API 스펙
```
GET /api/v1/notifications/templates?channel={channel}
Authorization: Bearer {token} (admin)

Response: 200 OK
{
    "totalCount": 20,
    "items": [
        {
            "id": 1,
            "code": "ORDER_COMPLETED",
            "name": "주문 완료 알림",
            "channel": "PUSH",
            "titleTemplate": "주문이 완료되었습니다",
            "bodyTemplate": "{{memberName}}님, 주문번호 {{orderNumber}}의 결제가 완료되었습니다.",
            "variables": ["memberName", "orderNumber"],
            "isActive": true
        }
    ]
}

PUT /api/v1/notifications/templates/{templateId}
Authorization: Bearer {token} (admin)
Content-Type: application/json

Request:
{
    "titleTemplate": "주문이 완료되었습니다!",
    "bodyTemplate": "{{memberName}}님, {{productName}} 외 {{itemCount}}건의 주문이 완료되었습니다. 주문번호: {{orderNumber}}",
    "variables": ["memberName", "productName", "itemCount", "orderNumber"]
}

Response: 200 OK
{ ... }

POST /api/v1/notifications/templates/{templateId}/preview
Authorization: Bearer {token} (admin)
Content-Type: application/json

Request:
{
    "variables": {
        "memberName": "홍길동",
        "productName": "오버핏 티셔츠",
        "itemCount": 2,
        "orderNumber": "ORD-2026032200001"
    }
}

Response: 200 OK
{
    "title": "주문이 완료되었습니다!",
    "body": "홍길동님, 오버핏 티셔츠 외 2건의 주문이 완료되었습니다. 주문번호: ORD-2026032200001"
}
```

---

### US-1405: 알림 설정

**As a** 구매자
**I want to** 알림 채널별로 수신 여부를 설정하고 싶다
**So that** 원하는 알림만 원하는 채널로 받을 수 있다

#### Acceptance Criteria
- [ ] 채널별(이메일/SMS/푸시) 알림 수신 on/off를 설정할 수 있다
- [ ] 알림 유형별(주문/배송, 마케팅, 재입고) 수신 on/off를 설정할 수 있다
- [ ] 마케팅 알림은 기본 off, 주문/배송 알림은 기본 on
- [ ] 회원가입 시 기본 알림 설정을 자동 생성한다
- [ ] 알림 설정 변경 시 즉시 반영된다

#### 데이터 모델
```sql
CREATE TABLE notification_setting (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    member_id           BIGINT          NOT NULL COMMENT '회원 ID',
    type                VARCHAR(30)     NOT NULL COMMENT '알림 유형: ORDER, SHIPPING, MARKETING, RESTOCK',
    email_enabled       TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '이메일 수신',
    sms_enabled         TINYINT(1)      NOT NULL DEFAULT 1 COMMENT 'SMS 수신',
    push_enabled        TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '푸시 수신',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_type (member_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='알림 설정';

CREATE TABLE device_token (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    member_id           BIGINT          NOT NULL COMMENT '회원 ID',
    device_type         VARCHAR(20)     NOT NULL COMMENT '기기 유형: IOS, ANDROID, WEB',
    token               VARCHAR(500)    NOT NULL COMMENT '디바이스 토큰',
    is_active           TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '활성 여부',
    last_used_at        DATETIME(6)     NULL COMMENT '마지막 사용 일시',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_token (token),
    INDEX idx_member_id (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='디바이스 토큰';
```

#### API 스펙
```
GET /api/v1/notifications/settings
Authorization: Bearer {token}

Response: 200 OK
{
    "settings": [
        {
            "type": "ORDER",
            "typeName": "주문/결제",
            "emailEnabled": true,
            "smsEnabled": true,
            "pushEnabled": true
        },
        {
            "type": "SHIPPING",
            "typeName": "배송",
            "emailEnabled": true,
            "smsEnabled": true,
            "pushEnabled": true
        },
        {
            "type": "MARKETING",
            "typeName": "마케팅/이벤트",
            "emailEnabled": false,
            "smsEnabled": false,
            "pushEnabled": false
        },
        {
            "type": "RESTOCK",
            "typeName": "재입고",
            "emailEnabled": true,
            "smsEnabled": false,
            "pushEnabled": true
        }
    ]
}

PUT /api/v1/notifications/settings
Authorization: Bearer {token}
Content-Type: application/json

Request:
{
    "settings": [
        { "type": "MARKETING", "emailEnabled": false, "smsEnabled": false, "pushEnabled": true },
        { "type": "RESTOCK", "emailEnabled": true, "smsEnabled": false, "pushEnabled": true }
    ]
}

Response: 200 OK
{ ... }

POST /api/v1/notifications/device-tokens
Authorization: Bearer {token}
Content-Type: application/json

Request:
{
    "deviceType": "IOS",
    "token": "fcm_token_abc123..."
}

Response: 201 Created
{ ... }
```

---

## 4. 콘텐츠 도메인 (content-service)

### US-1501: 매거진 작성/발행

**As a** 에디터(관리자)
**I want to** 패션 매거진 콘텐츠를 작성하고 발행하고 싶다
**So that** 사용자에게 스타일 정보를 제공하고 상품 구매를 유도할 수 있다

#### Acceptance Criteria
- [ ] 매거진 콘텐츠 형식: 제목, 썸네일, 본문(HTML 에디터), 태그, 연관 상품
- [ ] 매거진 상태: `DRAFT`(초안) → `PUBLISHED`(발행) → `HIDDEN`(숨김)
- [ ] 발행 예약 기능: 지정된 시간에 자동 발행
- [ ] 매거진에 연관 상품을 태그하여 상품 페이지로 연결한다
- [ ] 매거진 조회수, 좋아요 수를 집계한다
- [ ] 매거진 목록: 최신순, 인기순(조회수), 카테고리별 필터
- [ ] 매거진 카테고리: `TREND`(트렌드), `STYLING`(스타일링), `BRAND_STORY`(브랜드 스토리), `HOW_TO`(활용법)

#### 데이터 모델
```sql
CREATE TABLE magazine (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    title               VARCHAR(200)    NOT NULL COMMENT '제목',
    subtitle            VARCHAR(300)    NULL COMMENT '부제목',
    thumbnail_url       VARCHAR(500)    NOT NULL COMMENT '썸네일 URL',
    content             LONGTEXT        NOT NULL COMMENT '본문 (HTML)',
    category            VARCHAR(30)     NOT NULL COMMENT '카테고리',
    author_id           BIGINT          NOT NULL COMMENT '작성자 ID',
    author_name         VARCHAR(50)     NOT NULL COMMENT '작성자 이름',
    tags                VARCHAR(500)    NULL COMMENT '태그 (JSON)',
    view_count          INT             NOT NULL DEFAULT 0 COMMENT '조회수',
    like_count          INT             NOT NULL DEFAULT 0 COMMENT '좋아요 수',
    status              VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '상태',
    published_at        DATETIME(6)     NULL COMMENT '발행일',
    scheduled_at        DATETIME(6)     NULL COMMENT '예약 발행일',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_status (status),
    INDEX idx_category (category),
    INDEX idx_published_at (published_at),
    INDEX idx_view_count (view_count DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='매거진';

CREATE TABLE magazine_product (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    magazine_id         BIGINT          NOT NULL COMMENT '매거진 ID',
    product_id          BIGINT          NOT NULL COMMENT '상품 ID',
    display_order       INT             NOT NULL DEFAULT 0 COMMENT '노출 순서',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_magazine_product (magazine_id, product_id),
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='매거진 연관 상품';
```

#### API 스펙
```
POST /api/v1/contents/magazines
Authorization: Bearer {token} (admin)
Content-Type: application/json

Request:
{
    "title": "2026 SS 필수 아이템 TOP 10",
    "subtitle": "이번 봄/여름 시즌, 반드시 갖춰야 할 아이템을 소개합니다",
    "thumbnailUrl": "https://cdn.closet.com/magazines/ss2026-top10.jpg",
    "content": "<h1>2026 SS 필수 아이템</h1><p>이번 시즌의 트렌드는...</p>",
    "category": "TREND",
    "tags": ["SS2026", "트렌드", "필수템"],
    "productIds": [100, 101, 102, 103, 104],
    "scheduledAt": "2026-04-01T09:00:00"
}

Response: 201 Created
{
    "id": 1,
    "title": "2026 SS 필수 아이템 TOP 10",
    "status": "DRAFT",
    "scheduledAt": "2026-04-01T09:00:00",
    "createdAt": "2026-03-22T10:00:00"
}

GET /api/v1/contents/magazines?category={category}&sort={sort}&page={page}&size={size}
Authorization: Bearer {token}

Parameters:
- category: TREND, STYLING, BRAND_STORY, HOW_TO (선택)
- sort: LATEST (기본), POPULAR

Response: 200 OK
{
    "totalCount": 30,
    "items": [
        {
            "id": 1,
            "title": "2026 SS 필수 아이템 TOP 10",
            "subtitle": "이번 봄/여름 시즌, 반드시 갖춰야 할 아이템을 소개합니다",
            "thumbnailUrl": "https://cdn.closet.com/magazines/ss2026-top10.jpg",
            "category": "TREND",
            "authorName": "Closet 에디터",
            "viewCount": 15200,
            "likeCount": 320,
            "publishedAt": "2026-04-01T09:00:00"
        }
    ]
}

GET /api/v1/contents/magazines/{magazineId}
Authorization: Bearer {token}

Response: 200 OK
{
    "id": 1,
    "title": "2026 SS 필수 아이템 TOP 10",
    "subtitle": "이번 봄/여름 시즌...",
    "thumbnailUrl": "...",
    "content": "<h1>2026 SS 필수 아이템</h1>...",
    "category": "TREND",
    "authorName": "Closet 에디터",
    "tags": ["SS2026", "트렌드", "필수템"],
    "viewCount": 15200,
    "likeCount": 320,
    "isLiked": false,
    "publishedAt": "2026-04-01T09:00:00",
    "relatedProducts": [
        {
            "productId": 100,
            "productName": "린넨 오버핏 셔츠",
            "brandName": "무신사 스탠다드",
            "price": 49900,
            "salePrice": 39920,
            "imageUrl": "https://cdn.closet.com/products/100/main.jpg"
        }
    ]
}

POST /api/v1/contents/magazines/{magazineId}/like
Authorization: Bearer {token}
Response: 201 Created

DELETE /api/v1/contents/magazines/{magazineId}/like
Authorization: Bearer {token}
Response: 204 No Content
```

---

### US-1502: 코디 추천

**As a** 에디터(관리자)
**I want to** 상품 조합으로 코디를 등록하고 추천하고 싶다
**So that** 사용자가 스타일링 참고와 함께 세트 구매를 할 수 있다

#### Acceptance Criteria
- [ ] 코디 등록: 제목, 설명, 대표 이미지, 상품 조합(2~10개), 스타일 태그
- [ ] 스타일 태그: `CASUAL`(캐주얼), `STREET`(스트릿), `MINIMAL`(미니멀), `CLASSIC`(클래식), `SPORTY`(스포티)
- [ ] 코디에 포함된 상품의 총 가격을 자동 계산하여 표시한다
- [ ] 코디 좋아요, 저장(스크랩) 기능
- [ ] 코디 목록: 스타일별, 시즌별, 성별 필터
- [ ] 인기 코디(좋아요 기준) Top 10을 메인 페이지에 노출한다
- [ ] 코디 상세에서 개별 상품 바로 구매/장바구니 담기 가능

#### 데이터 모델
```sql
CREATE TABLE coordi (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    title               VARCHAR(200)    NOT NULL COMMENT '코디 제목',
    description         TEXT            NULL COMMENT '코디 설명',
    image_url           VARCHAR(500)    NOT NULL COMMENT '코디 대표 이미지',
    style_tag           VARCHAR(30)     NOT NULL COMMENT '스타일 태그',
    season              VARCHAR(10)     NULL COMMENT '시즌: SS, FW, ALL',
    gender              VARCHAR(10)     NOT NULL DEFAULT 'UNISEX' COMMENT '성별: MALE, FEMALE, UNISEX',
    total_price         INT             NOT NULL DEFAULT 0 COMMENT '총 가격',
    author_id           BIGINT          NOT NULL COMMENT '작성자 ID',
    like_count          INT             NOT NULL DEFAULT 0 COMMENT '좋아요 수',
    scrap_count         INT             NOT NULL DEFAULT 0 COMMENT '스크랩 수',
    view_count          INT             NOT NULL DEFAULT 0 COMMENT '조회수',
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '상태: ACTIVE, HIDDEN',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_style_tag (style_tag),
    INDEX idx_season (season),
    INDEX idx_gender (gender),
    INDEX idx_like_count (like_count DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='코디 추천';

CREATE TABLE coordi_product (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    coordi_id           BIGINT          NOT NULL COMMENT '코디 ID',
    product_id          BIGINT          NOT NULL COMMENT '상품 ID',
    display_order       INT             NOT NULL DEFAULT 0 COMMENT '표시 순서',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_coordi_product (coordi_id, product_id),
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='코디 상품';

CREATE TABLE coordi_scrap (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    coordi_id           BIGINT          NOT NULL COMMENT '코디 ID',
    member_id           BIGINT          NOT NULL COMMENT '회원 ID',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_coordi_member (coordi_id, member_id),
    INDEX idx_member_id (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='코디 스크랩';
```

#### API 스펙
```
POST /api/v1/contents/coordis
Authorization: Bearer {token} (admin)
Content-Type: application/json

Request:
{
    "title": "2026 봄 캐주얼 코디",
    "description": "가볍고 편안한 봄 데일리 코디를 소개합니다",
    "imageUrl": "https://cdn.closet.com/coordis/spring-casual.jpg",
    "styleTag": "CASUAL",
    "season": "SS",
    "gender": "MALE",
    "productIds": [100, 201, 302, 405]
}

Response: 201 Created
{
    "id": 1,
    "title": "2026 봄 캐주얼 코디",
    "styleTag": "CASUAL",
    "totalPrice": 189600,
    "productCount": 4,
    "createdAt": "2026-03-22T10:00:00"
}

GET /api/v1/contents/coordis?style={style}&season={season}&gender={gender}&sort={sort}&page={page}&size={size}
Authorization: Bearer {token}

Parameters:
- style: CASUAL, STREET, MINIMAL, CLASSIC, SPORTY (선택)
- season: SS, FW, ALL (선택)
- gender: MALE, FEMALE, UNISEX (선택)
- sort: LATEST (기본), POPULAR

Response: 200 OK
{
    "totalCount": 50,
    "items": [
        {
            "id": 1,
            "title": "2026 봄 캐주얼 코디",
            "imageUrl": "https://cdn.closet.com/coordis/spring-casual.jpg",
            "styleTag": "CASUAL",
            "season": "SS",
            "totalPrice": 189600,
            "productCount": 4,
            "likeCount": 150,
            "scrapCount": 85
        }
    ]
}

GET /api/v1/contents/coordis/{coordiId}
Authorization: Bearer {token}

Response: 200 OK
{
    "id": 1,
    "title": "2026 봄 캐주얼 코디",
    "description": "가볍고 편안한 봄 데일리 코디를 소개합니다",
    "imageUrl": "https://cdn.closet.com/coordis/spring-casual.jpg",
    "styleTag": "CASUAL",
    "season": "SS",
    "gender": "MALE",
    "totalPrice": 189600,
    "likeCount": 150,
    "scrapCount": 85,
    "isLiked": false,
    "isScrapped": true,
    "products": [
        {
            "productId": 100,
            "productName": "린넨 오버핏 셔츠",
            "brandName": "무신사 스탠다드",
            "categoryCode": "TOP",
            "price": 49900,
            "imageUrl": "https://cdn.closet.com/products/100/main.jpg"
        },
        {
            "productId": 201,
            "productName": "와이드 치노 팬츠",
            "brandName": "무신사 스탠다드",
            "categoryCode": "BOTTOM",
            "price": 59900,
            "imageUrl": "https://cdn.closet.com/products/201/main.jpg"
        }
    ]
}

POST /api/v1/contents/coordis/{coordiId}/like
Authorization: Bearer {token}
Response: 201 Created

POST /api/v1/contents/coordis/{coordiId}/scrap
Authorization: Bearer {token}
Response: 201 Created

DELETE /api/v1/contents/coordis/{coordiId}/scrap
Authorization: Bearer {token}
Response: 204 No Content
```

---

### US-1503: OOTD 스냅

**As a** 구매자
**I want to** 나의 착용샷(OOTD)을 공유하고 다른 사용자의 스타일을 구경하고 싶다
**So that** 패션 커뮤니티에 참여하고 스타일링 영감을 얻을 수 있다

#### Acceptance Criteria
- [ ] OOTD 게시물 작성: 이미지(최대 5장), 설명, 키/체중(선택), 착용 상품 태그
- [ ] 착용 상품 태그: Closet에서 판매 중인 상품을 검색하여 태그한다
- [ ] OOTD 피드: 최신순, 인기순(좋아요), 체형별 필터
- [ ] 좋아요, 댓글, 북마크 기능
- [ ] 신고 기능: 부적절한 게시물 신고
- [ ] OOTD 작성 시 포인트 적립 (200P, 일 1회 제한)
- [ ] OOTD에 태그된 상품 클릭 시 상품 상세로 이동
- [ ] 이미지는 S3에 업로드하고 리사이즈(800x800) 처리한다
- [ ] OOTD 게시물 상태: `ACTIVE`(활성), `HIDDEN`(숨김), `REPORTED`(신고됨), `DELETED`(삭제)

#### 데이터 모델
```sql
CREATE TABLE ootd (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    member_id           BIGINT          NOT NULL COMMENT '작성자 ID',
    content             VARCHAR(1000)   NULL COMMENT '설명',
    height              INT             NULL COMMENT '키 (cm)',
    weight              INT             NULL COMMENT '체중 (kg)',
    style_tag           VARCHAR(30)     NULL COMMENT '스타일 태그',
    like_count          INT             NOT NULL DEFAULT 0 COMMENT '좋아요 수',
    comment_count       INT             NOT NULL DEFAULT 0 COMMENT '댓글 수',
    bookmark_count      INT             NOT NULL DEFAULT 0 COMMENT '북마크 수',
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '상태',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_member_id (member_id),
    INDEX idx_status (status),
    INDEX idx_like_count (like_count DESC),
    INDEX idx_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OOTD 게시물';

CREATE TABLE ootd_image (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    ootd_id             BIGINT          NOT NULL COMMENT 'OOTD ID',
    image_url           VARCHAR(500)    NOT NULL COMMENT '이미지 URL',
    thumbnail_url       VARCHAR(500)    NOT NULL COMMENT '썸네일 URL',
    display_order       INT             NOT NULL DEFAULT 0 COMMENT '표시 순서',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_ootd_id (ootd_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OOTD 이미지';

CREATE TABLE ootd_product_tag (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    ootd_id             BIGINT          NOT NULL COMMENT 'OOTD ID',
    product_id          BIGINT          NOT NULL COMMENT '상품 ID',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_ootd_id (ootd_id),
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OOTD 상품 태그';

CREATE TABLE ootd_comment (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    ootd_id             BIGINT          NOT NULL COMMENT 'OOTD ID',
    member_id           BIGINT          NOT NULL COMMENT '작성자 ID',
    content             VARCHAR(500)    NOT NULL COMMENT '댓글 내용',
    parent_id           BIGINT          NULL COMMENT '부모 댓글 ID (대댓글)',
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '상태',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_ootd_id (ootd_id),
    INDEX idx_member_id (member_id),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OOTD 댓글';

CREATE TABLE ootd_bookmark (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    ootd_id             BIGINT          NOT NULL COMMENT 'OOTD ID',
    member_id           BIGINT          NOT NULL COMMENT '회원 ID',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ootd_member (ootd_id, member_id),
    INDEX idx_member_id (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OOTD 북마크';
```

#### API 스펙
```
POST /api/v1/contents/ootds
Authorization: Bearer {token}
Content-Type: multipart/form-data

Request:
- content: "오늘의 봄 캐주얼 코디!" (선택)
- height: 178 (선택)
- weight: 72 (선택)
- styleTag: "CASUAL" (선택)
- images: [file1.jpg, file2.jpg] (필수, 최소 1장, 최대 5장)
- productIds: [100, 201] (선택)

Response: 201 Created
{
    "id": 1,
    "content": "오늘의 봄 캐주얼 코디!",
    "images": [
        { "imageUrl": "https://cdn.closet.com/ootds/1/1.jpg", "thumbnailUrl": "https://cdn.closet.com/ootds/1/1_thumb.jpg" }
    ],
    "status": "ACTIVE",
    "createdAt": "2026-03-22T10:00:00"
}

GET /api/v1/contents/ootds?sort={sort}&style={style}&minHeight={min}&maxHeight={max}&page={page}&size={size}
Authorization: Bearer {token}

Parameters:
- sort: LATEST (기본), POPULAR
- style: 스타일 태그 (선택)
- minHeight, maxHeight: 키 범위 (선택)
- minWeight, maxWeight: 체중 범위 (선택)

Response: 200 OK
{
    "totalCount": 500,
    "items": [
        {
            "id": 1,
            "memberId": 999,
            "memberName": "fashionista_kim",
            "memberProfileUrl": "https://cdn.closet.com/profiles/999.jpg",
            "content": "오늘의 봄 캐주얼 코디!",
            "thumbnailUrl": "https://cdn.closet.com/ootds/1/1_thumb.jpg",
            "imageCount": 2,
            "height": 178,
            "weight": 72,
            "styleTag": "CASUAL",
            "likeCount": 45,
            "commentCount": 8,
            "isLiked": false,
            "isBookmarked": true,
            "createdAt": "2026-03-22T10:00:00"
        }
    ]
}

GET /api/v1/contents/ootds/{ootdId}
Authorization: Bearer {token}

Response: 200 OK
{
    "id": 1,
    "memberId": 999,
    "memberName": "fashionista_kim",
    "content": "오늘의 봄 캐주얼 코디!",
    "height": 178,
    "weight": 72,
    "styleTag": "CASUAL",
    "images": [
        { "imageUrl": "...", "thumbnailUrl": "..." }
    ],
    "taggedProducts": [
        {
            "productId": 100,
            "productName": "린넨 오버핏 셔츠",
            "brandName": "무신사 스탠다드",
            "price": 49900,
            "imageUrl": "..."
        }
    ],
    "likeCount": 45,
    "commentCount": 8,
    "isLiked": false,
    "isBookmarked": true,
    "comments": [
        {
            "id": 1,
            "memberName": "style_lover",
            "content": "코디 너무 좋아요!",
            "createdAt": "2026-03-22T11:00:00"
        }
    ]
}

POST /api/v1/contents/ootds/{ootdId}/like
Authorization: Bearer {token}
Response: 201 Created

POST /api/v1/contents/ootds/{ootdId}/comments
Authorization: Bearer {token}
Content-Type: application/json

Request:
{
    "content": "코디 너무 좋아요!",
    "parentId": null
}

Response: 201 Created
{ ... }

POST /api/v1/contents/ootds/{ootdId}/bookmark
Authorization: Bearer {token}
Response: 201 Created

POST /api/v1/contents/ootds/{ootdId}/report
Authorization: Bearer {token}
Content-Type: application/json

Request:
{
    "reason": "부적절한 이미지"
}

Response: 201 Created
{ ... }
```

---

## 5. AI 도메인 (ai-service) - 향후 고도화

### 개인화 추천 (초기 버전)

**As a** 구매자
**I want to** 나의 구매 이력과 관심사에 맞는 상품을 추천받고 싶다
**So that** 원하는 상품을 더 쉽게 발견할 수 있다

#### 초기 구현 범위
- [ ] 협업 필터링 기반 "이 상품을 본 사용자가 함께 본 상품" 추천
- [ ] 구매 이력 기반 "당신을 위한 추천" 상품 리스트
- [ ] 추천 데이터는 일간 배치로 생성하여 Redis에 캐싱한다
- [ ] 추천 알고리즘: 상품 조회 로그 기반 Item-Item 유사도 (코사인 유사도)
- [ ] 추천 상품 최대 20개, Cold Start(신규 회원)는 인기 상품으로 대체

#### 사이즈 추천 (향후)
- [ ] 구매 이력 + 리뷰 사이즈 데이터 기반 사이즈 추천
- [ ] "나와 비슷한 체형의 사용자들이 선택한 사이즈" 정보 제공

#### API 스펙
```
GET /api/v1/ai/recommendations/personal?limit={limit}
Authorization: Bearer {token}

Response: 200 OK
{
    "type": "PERSONAL",
    "items": [
        {
            "productId": 300,
            "productName": "슬림핏 치노 팬츠",
            "brandName": "무신사 스탠다드",
            "price": 49900,
            "imageUrl": "https://cdn.closet.com/products/300/main.jpg",
            "score": 0.92,
            "reason": "최근 조회한 상품과 유사"
        }
    ]
}

GET /api/v1/ai/recommendations/similar?productId={productId}&limit={limit}
Authorization: Bearer {token}

Response: 200 OK
{
    "type": "SIMILAR",
    "baseProductId": 100,
    "items": [
        {
            "productId": 150,
            "productName": "린넨 레귤러핏 셔츠",
            "brandName": "무신사 스탠다드",
            "price": 44900,
            "imageUrl": "https://cdn.closet.com/products/150/main.jpg",
            "score": 0.88,
            "reason": "이 상품을 본 사용자가 함께 본 상품"
        }
    ]
}

GET /api/v1/ai/size-recommendation?productId={productId}
Authorization: Bearer {token}

Response: 200 OK
{
    "productId": 100,
    "recommendedSize": "L",
    "confidence": 0.85,
    "sizeDistribution": {
        "M": 0.15,
        "L": 0.65,
        "XL": 0.20
    },
    "basedOn": "비슷한 체형(175cm/72kg) 사용자 85%가 L을 선택"
}
```

---

## 6. 서비스 간 이벤트 흐름

```
order-service                settlement-service            notification-service
     │                            │                              │
     │  order.confirmed ─────────>│ (정산 대상 적재)              │
     │  order.confirmed ─────────────────────────────────────────>│ (구매확정 알림)
     │  order.created ───────────────────────────────────────────>│ (주문 완료 알림)
     │  order.status.changed ────────────────────────────────────>│ (상태 변경 알림)
     │                            │                              │
     │                            │  settlement.created ────────>│ (정산서 생성 알림)
     │                            │  settlement.paid ───────────>│ (정산 완료 알림)

shipping-service             notification-service           inventory-service
     │                            │                              │
     │  shipping.delivered ──────>│ (배송 완료 알림)              │
     │                            │                              │
     │                            │<── inventory.restocked ──────│ (재입고 알림)

promotion-service            notification-service           content-service
     │                            │                              │
     │  timesale.started ────────>│ (타임세일 알림)              │
     │  coupon.issued ───────────>│ (쿠폰 발급 알림)            │
     │                            │                              │
     │                            │          ootd.created ───────│ (OOTD 포인트 적립)
     │                            │                              │ ──> member-service

ai-service                   display-service               product-service
     │                            │                              │
     │  추천 데이터 ──────────────>│ (메인 추천 섹션)             │
     │  조회 로그 수집 <──────────────────────────────────────────│
```

---

## 7. 기술 의사결정

| 결정 | 선택 | 이유 |
|------|------|------|
| 정산 배치 | Spring Batch | 대용량 트랜잭션 처리, 청크 기반 처리, 재시도/스킵 정책, Job 모니터링 |
| 정산 스케줄링 | Spring Scheduler + Quartz | 일간/주간 정기 배치, 클러스터 환경 중복 실행 방지 (ShedLock) |
| 알림 발송 | Kafka Consumer + 비동기 | 대량 발송 처리, 발송 실패 재시도, 서비스 결합도 낮음 |
| 이메일 발송 | AWS SES | 대량 발송 지원, 비용 효율, 바운스/컴플레인 관리 |
| SMS 발송 | NHN Cloud SMS | 국내 SMS 발송 안정성, API 연동 용이 |
| 푸시 발송 | Firebase Cloud Messaging (FCM) | iOS/Android/Web 통합 지원, 무료 |
| 콘텐츠 이미지 | AWS S3 + CloudFront | CDN 캐싱, 글로벌 배포, 이미지 리사이즈 |
| AI 추천 초기 | Item-Item 협업 필터링 | 구현 복잡도 낮음, Cold Start 대응, 일간 배치로 충분 |
| 추천 캐싱 | Redis | 추천 결과 실시간 조회, TTL 기반 자동 갱신 |

---

## 8. 마일스톤

| Sprint | 기간 | 목표 |
|--------|------|------|
| Sprint 14 | Week 1-2 | 정산 서비스: 정산 대상 집계 + 수수료 계산 (US-1301, US-1302) |
| Sprint 15 | Week 3-4 | 정산 서비스: 정산서 생성 + 지급 + 리포트 (US-1303~1305) |
| Sprint 16 | Week 5-6 | 알림 서비스: 주문/배송 알림 + 재입고 알림 + 마케팅 알림 (US-1401~1403) |
| Sprint 17 | Week 7-8 | 알림 서비스: 템플릿/설정 (US-1404, US-1405) + 콘텐츠 서비스: 매거진/코디 (US-1501, US-1502) |
| Sprint 18 | Week 9-10 | 콘텐츠 서비스: OOTD (US-1503) + AI 서비스 초기 구현 + 통합 테스트 |
