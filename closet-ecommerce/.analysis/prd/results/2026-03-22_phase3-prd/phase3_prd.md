# Phase 3 상세 PRD: 확장 단계

> 작성일: 2026-03-22
> 프로젝트: Closet E-commerce
> Phase: 3 (확장)
> 도메인: 프로모션 + 전시 + 셀러 + CS

---

## 1. Phase 3 개요

### 목표
Phase 2에서 구축한 배송, 재고, 검색, 리뷰 기반 위에 **프로모션(쿠폰/할인/적립금), 전시(메인/랭킹/기획전), 셀러 입점, CS(문의/반품/교환 접수)** 기능을 추가하여 마켓플레이스 플랫폼으로의 확장을 완성한다. 구매 전환율 향상과 셀러 생태계 구축이 핵심 목표이다.

### 기간
- 예상 기간: 10주 (Phase 2 완료 후)
- Sprint 9-13

### 서비스 구성
| 서비스 | 포트 | 기술 스택 |
|--------|------|----------|
| promotion-service | 8090 | Kotlin, Spring Boot 3.x, JPA, Redis |
| display-service | 8091 | Kotlin, Spring Boot 3.x, JPA, Redis |
| seller-service | 8092 | Kotlin, Spring Boot 3.x, JPA |
| cs-service | 8093 | Kotlin, Spring Boot 3.x, JPA |

### Phase 3 KPI

| 지표 | 목표 | 측정 방법 |
|------|------|----------|
| 쿠폰 사용률 | 발급 대비 30% 이상 | 발급 건수 / 사용 건수 |
| 타임세일 전환율 | 페이지 방문 대비 15% | 타임세일 페이지 UV / 주문 수 |
| 메인 페이지 CTR | 배너 클릭률 5% 이상 | 배너 노출 / 클릭 |
| 셀러 입점 심사 TAT | 평균 3영업일 이내 | 신청일 ~ 승인/반려일 |
| CS 문의 평균 답변 시간 | 24시간 이내 | 문의 등록 ~ 답변 완료 |
| 반품/교환 처리율 | 접수 대비 95% 처리 | 접수 건 / 완료 건 |

---

## 2. 프로모션 도메인 (promotion-service)

### US-901: 쿠폰 정책 생성

**As a** 운영 관리자
**I want to** 다양한 조건의 쿠폰 정책을 생성하고 싶다
**So that** 마케팅 목적에 맞는 할인 프로모션을 운영할 수 있다

#### Acceptance Criteria
- [ ] 쿠폰 유형: `FIXED_AMOUNT`(정액 할인), `PERCENTAGE`(정률 할인)를 지원한다
- [ ] 적용 범위: `ALL`(전체), `CATEGORY`(카테고리), `BRAND`(브랜드), `PRODUCT`(특정 상품)
- [ ] 최소 주문 금액을 설정할 수 있다 (예: 30,000원 이상 구매 시 사용 가능)
- [ ] 정률 할인 시 최대 할인 금액을 설정할 수 있다 (예: 최대 10,000원)
- [ ] 유효 기간(시작일~종료일)을 설정할 수 있다
- [ ] 발급 상한 수량을 설정할 수 있다 (0 = 무제한)
- [ ] 쿠폰 정책 상태: `DRAFT`(초안) → `ACTIVE`(활성) → `EXPIRED`(만료) → `DISABLED`(비활성)
- [ ] 쿠폰 정책 생성 시 고유 코드가 자동 생성된다 (예: `SUMMER2026-ABCD`)
- [ ] 회원당 발급 횟수 제한을 설정할 수 있다 (기본값: 1회)

#### 데이터 모델
```sql
CREATE TABLE coupon_policy (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    code                VARCHAR(50)     NOT NULL COMMENT '쿠폰 정책 코드',
    name                VARCHAR(100)    NOT NULL COMMENT '쿠폰명',
    description         VARCHAR(500)    NULL COMMENT '쿠폰 설명',
    discount_type       VARCHAR(20)     NOT NULL COMMENT '할인 유형: FIXED_AMOUNT, PERCENTAGE',
    discount_value      INT             NOT NULL COMMENT '할인 값 (금액 또는 퍼센트)',
    max_discount_amount INT             NULL COMMENT '최대 할인 금액 (정률 시)',
    min_order_amount    INT             NOT NULL DEFAULT 0 COMMENT '최소 주문 금액',
    scope_type          VARCHAR(20)     NOT NULL DEFAULT 'ALL' COMMENT '적용 범위: ALL, CATEGORY, BRAND, PRODUCT',
    scope_value         VARCHAR(500)    NULL COMMENT '적용 대상 값 (JSON: categoryIds, brandIds, productIds)',
    total_quantity       INT            NOT NULL DEFAULT 0 COMMENT '발급 상한 (0=무제한)',
    issued_quantity      INT            NOT NULL DEFAULT 0 COMMENT '발급된 수량',
    max_issue_per_member INT            NOT NULL DEFAULT 1 COMMENT '회원당 발급 제한',
    status              VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '상태',
    started_at          DATETIME(6)     NOT NULL COMMENT '유효 시작일',
    ended_at            DATETIME(6)     NOT NULL COMMENT '유효 종료일',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    INDEX idx_status (status),
    INDEX idx_period (started_at, ended_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='쿠폰 정책';
```

#### API 스펙
```
POST /api/v1/promotions/coupon-policies
Authorization: Bearer {token} (admin)
Content-Type: application/json

Request:
{
    "name": "여름 시즌 10% 할인",
    "description": "여름 시즌 전 상품 10% 할인 쿠폰",
    "discountType": "PERCENTAGE",
    "discountValue": 10,
    "maxDiscountAmount": 10000,
    "minOrderAmount": 30000,
    "scopeType": "CATEGORY",
    "scopeValue": ["TOP", "BOTTOM"],
    "totalQuantity": 1000,
    "maxIssuePerMember": 1,
    "startedAt": "2026-06-01T00:00:00",
    "endedAt": "2026-08-31T23:59:59"
}

Response: 201 Created
{
    "id": 1,
    "code": "SUMMER2026-A1B2",
    "name": "여름 시즌 10% 할인",
    "discountType": "PERCENTAGE",
    "discountValue": 10,
    "maxDiscountAmount": 10000,
    "minOrderAmount": 30000,
    "scopeType": "CATEGORY",
    "status": "DRAFT",
    "createdAt": "2026-03-22T10:00:00"
}

GET /api/v1/promotions/coupon-policies?status={status}&page={page}&size={size}
Authorization: Bearer {token} (admin)

Response: 200 OK
{
    "totalCount": 15,
    "items": [...]
}
```

---

### US-902: 쿠폰 발급

**As a** 구매자
**I want to** 쿠폰을 다운로드하거나 코드를 입력하여 쿠폰을 발급받고 싶다
**So that** 주문 시 할인을 적용할 수 있다

#### Acceptance Criteria
- [ ] 발급 방식: `AUTO`(자동 다운로드), `CODE_INPUT`(코드 입력)
- [ ] 자동 발급: 쿠폰 목록에서 클릭 한 번으로 발급받을 수 있다
- [ ] 코드 입력: 프로모션 코드를 직접 입력하여 발급받을 수 있다
- [ ] 선착순 발급 시 동시성 제어를 위해 Redis `DECR` + Lua Script를 사용한다
- [ ] 발급 상한 초과 시 "쿠폰이 모두 소진되었습니다" 에러를 반환한다
- [ ] 회원당 발급 제한 초과 시 "이미 발급받은 쿠폰입니다" 에러를 반환한다
- [ ] 쿠폰 발급 시 `coupon.issued` Kafka 이벤트를 발행한다
- [ ] 발급된 쿠폰 상태: `AVAILABLE`(사용 가능) → `USED`(사용 완료) → `EXPIRED`(만료)
- [ ] 발급된 쿠폰의 유효기간은 정책의 유효기간을 따른다

#### 데이터 모델
```sql
CREATE TABLE coupon (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    coupon_policy_id    BIGINT          NOT NULL COMMENT '쿠폰 정책 ID',
    member_id           BIGINT          NOT NULL COMMENT '회원 ID',
    status              VARCHAR(20)     NOT NULL DEFAULT 'AVAILABLE' COMMENT '상태: AVAILABLE, USED, EXPIRED',
    used_at             DATETIME(6)     NULL COMMENT '사용 일시',
    used_order_id       BIGINT          NULL COMMENT '사용된 주문 ID',
    expired_at          DATETIME(6)     NOT NULL COMMENT '만료 일시',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_policy_member (coupon_policy_id, member_id),
    INDEX idx_member_status (member_id, status),
    INDEX idx_expired_at (expired_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='발급된 쿠폰';
```

#### API 스펙
```
POST /api/v1/promotions/coupons/issue
Authorization: Bearer {token}
Content-Type: application/json

Request:
{
    "couponPolicyId": 1
}

Response: 201 Created
{
    "id": 1,
    "couponPolicyId": 1,
    "couponName": "여름 시즌 10% 할인",
    "discountType": "PERCENTAGE",
    "discountValue": 10,
    "maxDiscountAmount": 10000,
    "status": "AVAILABLE",
    "expiredAt": "2026-08-31T23:59:59",
    "createdAt": "2026-03-22T10:00:00"
}

POST /api/v1/promotions/coupons/issue-by-code
Authorization: Bearer {token}
Content-Type: application/json

Request:
{
    "code": "SUMMER2026-A1B2"
}

Response: 201 Created
{ ... }

GET /api/v1/promotions/coupons/my?status={status}
Authorization: Bearer {token}

Response: 200 OK
{
    "totalCount": 5,
    "items": [
        {
            "id": 1,
            "couponName": "여름 시즌 10% 할인",
            "discountType": "PERCENTAGE",
            "discountValue": 10,
            "maxDiscountAmount": 10000,
            "minOrderAmount": 30000,
            "scopeType": "CATEGORY",
            "status": "AVAILABLE",
            "expiredAt": "2026-08-31T23:59:59"
        }
    ]
}
```

---

### US-903: 쿠폰 적용 (주문 시 할인 계산)

**As a** 구매자
**I want to** 주문 시 보유한 쿠폰을 적용하여 할인받고 싶다
**So that** 더 저렴한 가격에 상품을 구매할 수 있다

#### Acceptance Criteria
- [ ] 주문 시 사용 가능한 쿠폰 목록을 조회할 수 있다 (적용 범위, 최소 주문금액 검증)
- [ ] 쿠폰은 주문당 최대 1장만 적용 가능하다
- [ ] 정액 할인: 할인 금액을 직접 차감한다
- [ ] 정률 할인: 적용 대상 상품 합계 금액에 비율을 적용하고, 최대 할인 금액을 초과하지 않는다
- [ ] 쿠폰 적용 시 할인 금액을 미리 계산하여 반환한다 (적용 전 확인)
- [ ] 주문 확정 시 쿠폰 상태를 `USED`로 변경하고 `used_order_id`를 기록한다
- [ ] 주문 취소 시 쿠폰 상태를 `AVAILABLE`로 복구한다 (쿠폰 유효기간 내인 경우)
- [ ] 적립금과 쿠폰은 중복 사용 가능하다

#### API 스펙
```
GET /api/v1/promotions/coupons/applicable?orderAmount={amount}&productIds={ids}&categoryCode={code}
Authorization: Bearer {token}

Response: 200 OK
{
    "applicableCoupons": [
        {
            "couponId": 1,
            "couponName": "여름 시즌 10% 할인",
            "discountType": "PERCENTAGE",
            "discountValue": 10,
            "expectedDiscountAmount": 5000,
            "minOrderAmount": 30000
        },
        {
            "couponId": 2,
            "couponName": "신규 회원 5,000원 할인",
            "discountType": "FIXED_AMOUNT",
            "discountValue": 5000,
            "expectedDiscountAmount": 5000,
            "minOrderAmount": 20000
        }
    ]
}

POST /api/v1/promotions/coupons/{couponId}/apply
Authorization: Bearer {token}
Content-Type: application/json

Request:
{
    "orderId": 12345,
    "orderAmount": 50000,
    "productIds": [100, 101],
    "categoryCode": "TOP"
}

Response: 200 OK
{
    "couponId": 1,
    "discountAmount": 5000,
    "finalOrderAmount": 45000
}
```

---

### US-904: 타임세일

**As a** 운영 관리자
**I want to** 특정 시간대에 한정 수량 할인 이벤트를 운영하고 싶다
**So that** 트래픽을 집중시키고 판매를 촉진할 수 있다

#### Acceptance Criteria
- [ ] 타임세일 생성: 상품, 할인율, 시작/종료 시간, 한정 수량을 설정한다
- [ ] 타임세일 상태: `SCHEDULED`(예정) → `ACTIVE`(진행중) → `ENDED`(종료) → `SOLD_OUT`(매진)
- [ ] 시작 시간이 되면 자동으로 `ACTIVE` 상태로 전환된다 (Spring Scheduler)
- [ ] 종료 시간이 되면 자동으로 `ENDED` 상태로 전환된다
- [ ] 한정 수량이 모두 판매되면 `SOLD_OUT` 상태로 전환된다
- [ ] 타임세일 수량은 Redis DECR로 관리하여 동시성을 보장한다
- [ ] 타임세일 시작/종료 시 `timesale.started`, `timesale.ended` 이벤트를 발행한다
- [ ] 진행 중인 타임세일 목록을 조회할 수 있다 (잔여 수량, 남은 시간 포함)

#### 데이터 모델
```sql
CREATE TABLE time_sale (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    product_id          BIGINT          NOT NULL COMMENT '상품 ID',
    product_option_id   BIGINT          NULL COMMENT '옵션 ID (NULL=전체)',
    title               VARCHAR(200)    NOT NULL COMMENT '타임세일 제목',
    discount_rate       INT             NOT NULL COMMENT '할인율 (%)',
    original_price      INT             NOT NULL COMMENT '원래 가격',
    sale_price          INT             NOT NULL COMMENT '세일 가격',
    total_quantity      INT             NOT NULL COMMENT '한정 수량',
    sold_quantity       INT             NOT NULL DEFAULT 0 COMMENT '판매 수량',
    status              VARCHAR(20)     NOT NULL DEFAULT 'SCHEDULED' COMMENT '상태',
    started_at          DATETIME(6)     NOT NULL COMMENT '시작 시간',
    ended_at            DATETIME(6)     NOT NULL COMMENT '종료 시간',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_status (status),
    INDEX idx_period (started_at, ended_at),
    INDEX idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='타임세일';
```

#### API 스펙
```
POST /api/v1/promotions/time-sales
Authorization: Bearer {token} (admin)
Content-Type: application/json

Request:
{
    "productId": 100,
    "title": "나이키 에어맥스 반값 세일",
    "discountRate": 50,
    "originalPrice": 159000,
    "salePrice": 79500,
    "totalQuantity": 100,
    "startedAt": "2026-04-01T12:00:00",
    "endedAt": "2026-04-01T14:00:00"
}

Response: 201 Created
{
    "id": 1,
    "productId": 100,
    "title": "나이키 에어맥스 반값 세일",
    "discountRate": 50,
    "salePrice": 79500,
    "totalQuantity": 100,
    "soldQuantity": 0,
    "status": "SCHEDULED",
    "startedAt": "2026-04-01T12:00:00",
    "endedAt": "2026-04-01T14:00:00"
}

GET /api/v1/promotions/time-sales?status=ACTIVE
Authorization: Bearer {token}

Response: 200 OK
{
    "totalCount": 3,
    "items": [
        {
            "id": 1,
            "productId": 100,
            "productName": "나이키 에어맥스 90",
            "productImageUrl": "https://cdn.closet.com/products/100/main.jpg",
            "title": "나이키 에어맥스 반값 세일",
            "discountRate": 50,
            "originalPrice": 159000,
            "salePrice": 79500,
            "totalQuantity": 100,
            "remainingQuantity": 42,
            "status": "ACTIVE",
            "remainingSeconds": 3600,
            "endedAt": "2026-04-01T14:00:00"
        }
    ]
}
```

---

### US-905: 적립금 정책

**As a** 운영 관리자
**I want to** 회원 등급별 적립률과 리뷰 적립 정책을 관리하고 싶다
**So that** 충성 고객에게 더 높은 적립 혜택을 제공하여 재구매를 유도할 수 있다

#### Acceptance Criteria
- [ ] 등급별 기본 적립률 설정: NORMAL(1%), SILVER(2%), GOLD(3%), PLATINUM(5%)
- [ ] 적립금 적립 시점: 구매확정(`CONFIRMED`) 시 자동 적립
- [ ] 리뷰 적립: 텍스트 리뷰 100P, 포토 리뷰 300P, 사이즈 정보 50P
- [ ] 적립금 사용: 1P = 1원, 최소 사용 단위 1,000P
- [ ] 적립금 유효기간: 적립일로부터 1년 (만료 30일 전 알림)
- [ ] 적립금 사용/적립 이력 조회 API 제공
- [ ] 주문 취소 시 사용한 적립금 복구, 적립된 적립금 회수

#### 데이터 모델
```sql
CREATE TABLE point_policy (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    member_grade        VARCHAR(20)     NOT NULL COMMENT '회원 등급',
    earn_rate           DECIMAL(5,2)    NOT NULL COMMENT '적립률 (%)',
    review_text_point   INT             NOT NULL DEFAULT 100 COMMENT '텍스트 리뷰 적립금',
    review_photo_point  INT             NOT NULL DEFAULT 300 COMMENT '포토 리뷰 적립금',
    review_size_point   INT             NOT NULL DEFAULT 50 COMMENT '사이즈 정보 적립금',
    min_use_amount      INT             NOT NULL DEFAULT 1000 COMMENT '최소 사용 금액',
    expiry_days         INT             NOT NULL DEFAULT 365 COMMENT '유효기간 (일)',
    is_active           TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '활성 여부',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_grade (member_grade)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='적립금 정책';

CREATE TABLE point_transaction (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    member_id           BIGINT          NOT NULL COMMENT '회원 ID',
    type                VARCHAR(20)     NOT NULL COMMENT '유형: EARN, USE, EXPIRE, CANCEL_EARN, CANCEL_USE',
    amount              INT             NOT NULL COMMENT '금액 (양수=적립, 음수=사용)',
    balance             INT             NOT NULL COMMENT '잔액',
    reason              VARCHAR(100)    NOT NULL COMMENT '사유',
    reference_type      VARCHAR(30)     NULL COMMENT '참조 유형: ORDER, REVIEW, EVENT',
    reference_id        BIGINT          NULL COMMENT '참조 ID',
    expired_at          DATETIME(6)     NULL COMMENT '만료일',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_member_id (member_id),
    INDEX idx_reference (reference_type, reference_id),
    INDEX idx_expired_at (expired_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='적립금 거래 내역';
```

#### API 스펙
```
GET /api/v1/promotions/points/my
Authorization: Bearer {token}

Response: 200 OK
{
    "memberId": 999,
    "totalBalance": 15000,
    "expiringAmount": 3000,
    "expiringDate": "2026-04-30"
}

GET /api/v1/promotions/points/my/transactions?page={page}&size={size}
Authorization: Bearer {token}

Response: 200 OK
{
    "totalCount": 30,
    "items": [
        {
            "id": 1,
            "type": "EARN",
            "amount": 1500,
            "balance": 15000,
            "reason": "주문 적립 (3%)",
            "referenceType": "ORDER",
            "referenceId": 12345,
            "expiredAt": "2027-03-22T23:59:59",
            "createdAt": "2026-03-22T10:00:00"
        }
    ]
}

PUT /api/v1/promotions/point-policies/{grade}
Authorization: Bearer {token} (admin)
Content-Type: application/json

Request:
{
    "earnRate": 3.0,
    "reviewTextPoint": 100,
    "reviewPhotoPoint": 300,
    "reviewSizePoint": 50,
    "minUseAmount": 1000,
    "expiryDays": 365
}

Response: 200 OK
{ ... }
```

---

## 3. 전시 도메인 (display-service)

### US-1001: 메인 페이지 구성

**As a** 구매자
**I want to** 메인 페이지에서 배너, 기획전, 신상품, 추천 상품을 한눈에 보고 싶다
**So that** 관심 있는 상품이나 이벤트를 빠르게 발견할 수 있다

#### Acceptance Criteria
- [ ] 메인 페이지 섹션: 상단 배너 → 기획전 → 실시간 랭킹 → 신상품 → 추천 상품 → 브랜드관
- [ ] 각 섹션은 독립적으로 API를 호출하여 조합한다
- [ ] 메인 페이지 구성 정보는 Redis에 캐싱한다 (TTL: 5분)
- [ ] 섹션 노출 순서와 on/off를 관리자가 설정할 수 있다
- [ ] 메인 페이지 전체 응답 시간: 500ms 이내

#### 데이터 모델
```sql
CREATE TABLE main_section (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    section_type        VARCHAR(30)     NOT NULL COMMENT '섹션 유형: BANNER, EXHIBITION, RANKING, NEW_ARRIVAL, RECOMMENDATION, BRAND',
    title               VARCHAR(100)    NOT NULL COMMENT '섹션 제목',
    display_order       INT             NOT NULL DEFAULT 0 COMMENT '노출 순서',
    is_active           TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '노출 여부',
    config              TEXT            NULL COMMENT '섹션별 설정 (JSON)',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_active_order (is_active, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='메인 페이지 섹션';
```

#### API 스펙
```
GET /api/v1/display/main
Authorization: Bearer {token}

Response: 200 OK
{
    "sections": [
        {
            "sectionType": "BANNER",
            "title": "메인 배너",
            "items": [
                {
                    "bannerId": 1,
                    "imageUrl": "https://cdn.closet.com/banners/summer-sale.jpg",
                    "linkUrl": "/exhibitions/1",
                    "title": "여름 시즌 최대 50% 세일"
                }
            ]
        },
        {
            "sectionType": "RANKING",
            "title": "실시간 랭킹",
            "items": [
                {
                    "rank": 1,
                    "productId": 100,
                    "productName": "나이키 에어맥스 90",
                    "brandName": "Nike",
                    "price": 159000,
                    "salePrice": 129000,
                    "imageUrl": "https://cdn.closet.com/products/100/main.jpg",
                    "rankChange": "UP"
                }
            ]
        },
        {
            "sectionType": "NEW_ARRIVAL",
            "title": "신상품",
            "items": [...]
        }
    ]
}
```

---

### US-1002: 실시간 랭킹

**As a** 구매자
**I want to** 실시간으로 인기 있는 상품 랭킹을 확인하고 싶다
**So that** 트렌드 상품을 빠르게 파악하고 구매 결정에 참고할 수 있다

#### Acceptance Criteria
- [ ] 랭킹 스코어 공식: 판매량 x 0.5 + 리뷰수 x 0.3 + 조회수 x 0.2
- [ ] Redis Sorted Set(ZSET)으로 실시간 랭킹을 관리한다
- [ ] 랭킹 키: `ranking:overall`, `ranking:category:{categoryCode}`, `ranking:brand:{brandId}`
- [ ] 주문 완료 시 판매량 스코어 갱신 (order.created 이벤트 수신)
- [ ] 리뷰 작성 시 리뷰 스코어 갱신 (review.created 이벤트 수신)
- [ ] 상품 조회 시 조회수 스코어 갱신 (Redis ZINCRBY)
- [ ] 랭킹 Top 100을 제공한다
- [ ] 이전 시간 대비 순위 변동 표시 (NEW, UP, DOWN, SAME)
- [ ] 랭킹 데이터는 1시간 단위로 스냅샷을 저장한다 (순위 변동 비교용)
- [ ] 카테고리별, 브랜드별 랭킹 필터를 지원한다

#### 데이터 모델
```sql
CREATE TABLE ranking_snapshot (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    ranking_type        VARCHAR(30)     NOT NULL COMMENT '랭킹 유형: OVERALL, CATEGORY, BRAND',
    ranking_key         VARCHAR(50)     NOT NULL COMMENT '랭킹 키 (categoryCode 또는 brandId)',
    product_id          BIGINT          NOT NULL COMMENT '상품 ID',
    rank_position       INT             NOT NULL COMMENT '순위',
    score               DECIMAL(10,2)   NOT NULL COMMENT '스코어',
    snapshot_at         DATETIME(6)     NOT NULL COMMENT '스냅샷 시점',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_snapshot (ranking_type, ranking_key, snapshot_at),
    INDEX idx_product (product_id, snapshot_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='랭킹 스냅샷';
```

#### API 스펙
```
GET /api/v1/display/rankings?type={type}&key={key}&page={page}&size={size}
Authorization: Bearer {token}

Parameters:
- type: OVERALL (기본), CATEGORY, BRAND
- key: 카테고리코드 또는 브랜드ID (type이 CATEGORY/BRAND인 경우 필수)
- page: 페이지 번호 (기본 0)
- size: 페이지 크기 (기본 20, 최대 100)

Response: 200 OK
{
    "rankingType": "OVERALL",
    "updatedAt": "2026-03-22T10:00:00",
    "totalCount": 100,
    "items": [
        {
            "rank": 1,
            "productId": 100,
            "productName": "나이키 에어맥스 90",
            "brandName": "Nike",
            "categoryCode": "SHOES",
            "price": 159000,
            "salePrice": 129000,
            "imageUrl": "https://cdn.closet.com/products/100/main.jpg",
            "score": 9850.50,
            "salesCount": 1520,
            "reviewCount": 320,
            "viewCount": 45000,
            "rankChange": "UP",
            "previousRank": 3
        },
        {
            "rank": 2,
            "productId": 205,
            "productName": "아디다스 스탠스미스",
            "brandName": "Adidas",
            "categoryCode": "SHOES",
            "price": 119000,
            "salePrice": null,
            "imageUrl": "https://cdn.closet.com/products/205/main.jpg",
            "score": 9200.30,
            "rankChange": "SAME",
            "previousRank": 2
        }
    ]
}
```

---

### US-1003: 기획전 관리

**As a** 운영 관리자
**I want to** 특정 테마의 기획전을 만들고 상품을 배정하고 싶다
**So that** 시즌별/테마별 큐레이션을 통해 구매를 유도할 수 있다

#### Acceptance Criteria
- [ ] 기획전 생성: 제목, 설명, 대표 이미지, 노출 기간, 할인율을 설정한다
- [ ] 기획전에 상품을 배정할 수 있다 (다대다 관계)
- [ ] 기획전별 할인율을 설정할 수 있다 (기획전 전용 할인)
- [ ] 기획전 상태: `DRAFT`(초안) → `SCHEDULED`(예정) → `ACTIVE`(진행중) → `ENDED`(종료)
- [ ] 노출 기간이 되면 자동으로 상태가 전환된다
- [ ] 기획전 목록 조회 시 진행 중인 기획전만 반환한다 (관리자는 전체 조회)
- [ ] 기획전 상세 페이지에서 배정된 상품 목록을 조회할 수 있다
- [ ] 기획전 시작/종료 시 `exhibition.started`, `exhibition.ended` 이벤트를 발행한다

#### 데이터 모델
```sql
CREATE TABLE exhibition (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    title               VARCHAR(200)    NOT NULL COMMENT '기획전 제목',
    description         TEXT            NULL COMMENT '기획전 설명',
    banner_image_url    VARCHAR(500)    NOT NULL COMMENT '배너 이미지 URL',
    detail_image_url    VARCHAR(500)    NULL COMMENT '상세 이미지 URL',
    discount_rate       INT             NULL COMMENT '기획전 할인율 (%)',
    status              VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '상태',
    started_at          DATETIME(6)     NOT NULL COMMENT '노출 시작일',
    ended_at            DATETIME(6)     NOT NULL COMMENT '노출 종료일',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_status (status),
    INDEX idx_period (started_at, ended_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='기획전';

CREATE TABLE exhibition_product (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    exhibition_id       BIGINT          NOT NULL COMMENT '기획전 ID',
    product_id          BIGINT          NOT NULL COMMENT '상품 ID',
    display_order       INT             NOT NULL DEFAULT 0 COMMENT '노출 순서',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_exhibition_product (exhibition_id, product_id),
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='기획전 상품 배정';
```

#### API 스펙
```
POST /api/v1/display/exhibitions
Authorization: Bearer {token} (admin)
Content-Type: application/json

Request:
{
    "title": "2026 SS 컬렉션 최대 30% OFF",
    "description": "봄/여름 신상품을 특별 할인가에 만나보세요",
    "bannerImageUrl": "https://cdn.closet.com/exhibitions/ss2026.jpg",
    "discountRate": 30,
    "startedAt": "2026-04-01T00:00:00",
    "endedAt": "2026-04-30T23:59:59",
    "productIds": [100, 101, 102, 103, 104]
}

Response: 201 Created
{
    "id": 1,
    "title": "2026 SS 컬렉션 최대 30% OFF",
    "status": "DRAFT",
    "productCount": 5,
    "createdAt": "2026-03-22T10:00:00"
}

GET /api/v1/display/exhibitions?status=ACTIVE&page={page}&size={size}
Authorization: Bearer {token}

Response: 200 OK
{
    "totalCount": 3,
    "items": [
        {
            "id": 1,
            "title": "2026 SS 컬렉션 최대 30% OFF",
            "bannerImageUrl": "https://cdn.closet.com/exhibitions/ss2026.jpg",
            "discountRate": 30,
            "productCount": 50,
            "startedAt": "2026-04-01T00:00:00",
            "endedAt": "2026-04-30T23:59:59"
        }
    ]
}

GET /api/v1/display/exhibitions/{exhibitionId}
Authorization: Bearer {token}

Response: 200 OK
{
    "id": 1,
    "title": "2026 SS 컬렉션 최대 30% OFF",
    "description": "봄/여름 신상품을 특별 할인가에 만나보세요",
    "bannerImageUrl": "https://cdn.closet.com/exhibitions/ss2026.jpg",
    "discountRate": 30,
    "products": [
        {
            "productId": 100,
            "productName": "린넨 오버핏 셔츠",
            "brandName": "무신사 스탠다드",
            "originalPrice": 49900,
            "salePrice": 34930,
            "imageUrl": "https://cdn.closet.com/products/100/main.jpg"
        }
    ]
}
```

---

### US-1004: 배너 관리

**As a** 운영 관리자
**I want to** 위치별로 배너를 등록하고 노출 기간을 관리하고 싶다
**So that** 프로모션, 기획전, 이벤트를 효과적으로 홍보할 수 있다

#### Acceptance Criteria
- [ ] 배너 위치: `MAIN_TOP`(메인 상단), `MAIN_MIDDLE`(메인 중간), `CATEGORY_TOP`(카테고리 상단), `PRODUCT_DETAIL`(상품 상세)
- [ ] 배너별 노출 기간(시작일~종료일)을 설정한다
- [ ] 배너 클릭 시 이동할 링크 URL을 설정한다
- [ ] 같은 위치에 복수 배너 등록 시 노출 순서(display_order)로 정렬한다
- [ ] 배너 노출/클릭 이벤트를 기록한다 (CTR 측정용)
- [ ] 배너 상태: `ACTIVE`(활성), `INACTIVE`(비활성), `EXPIRED`(만료)
- [ ] 배너 이미지는 S3에 업로드한다 (권장 사이즈: 1920x600)

#### 데이터 모델
```sql
CREATE TABLE banner (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    title               VARCHAR(200)    NOT NULL COMMENT '배너 제목',
    image_url           VARCHAR(500)    NOT NULL COMMENT '배너 이미지 URL',
    mobile_image_url    VARCHAR(500)    NULL COMMENT '모바일 배너 이미지 URL',
    link_url            VARCHAR(500)    NOT NULL COMMENT '클릭 시 이동 URL',
    position            VARCHAR(30)     NOT NULL COMMENT '배너 위치',
    display_order       INT             NOT NULL DEFAULT 0 COMMENT '노출 순서',
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '상태',
    impression_count    BIGINT          NOT NULL DEFAULT 0 COMMENT '노출 수',
    click_count         BIGINT          NOT NULL DEFAULT 0 COMMENT '클릭 수',
    started_at          DATETIME(6)     NOT NULL COMMENT '노출 시작일',
    ended_at            DATETIME(6)     NOT NULL COMMENT '노출 종료일',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_position_status (position, status),
    INDEX idx_period (started_at, ended_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='배너';
```

#### API 스펙
```
POST /api/v1/display/banners
Authorization: Bearer {token} (admin)
Content-Type: application/json

Request:
{
    "title": "여름 시즌 세일",
    "imageUrl": "https://cdn.closet.com/banners/summer-sale.jpg",
    "mobileImageUrl": "https://cdn.closet.com/banners/summer-sale-m.jpg",
    "linkUrl": "/exhibitions/1",
    "position": "MAIN_TOP",
    "displayOrder": 1,
    "startedAt": "2026-06-01T00:00:00",
    "endedAt": "2026-08-31T23:59:59"
}

Response: 201 Created
{ ... }

GET /api/v1/display/banners?position={position}
Authorization: Bearer {token}

Response: 200 OK
{
    "items": [
        {
            "id": 1,
            "title": "여름 시즌 세일",
            "imageUrl": "https://cdn.closet.com/banners/summer-sale.jpg",
            "mobileImageUrl": "https://cdn.closet.com/banners/summer-sale-m.jpg",
            "linkUrl": "/exhibitions/1",
            "position": "MAIN_TOP"
        }
    ]
}

POST /api/v1/display/banners/{bannerId}/click
-- 배너 클릭 이벤트 기록 (비동기)
Response: 204 No Content
```

---

### US-1005: 브랜드관

**As a** 구매자
**I want to** 좋아하는 브랜드의 전용 페이지에서 해당 브랜드 상품을 모아보고 싶다
**So that** 브랜드별로 상품을 탐색하고 브랜드 정보를 확인할 수 있다

#### Acceptance Criteria
- [ ] 브랜드관 페이지: 브랜드 로고, 소개, 전체 상품, 인기 상품, 신상품 탭
- [ ] 브랜드 정보: 로고, 배경 이미지, 소개 텍스트, 공식 사이트 링크
- [ ] 브랜드 팔로우(즐겨찾기) 기능: 팔로우한 브랜드의 신상품 알림 수신
- [ ] 브랜드 목록: 알파벳/가나다 순, 카테고리별 필터
- [ ] 브랜드관 상품 목록은 search-service와 연동하여 제공한다
- [ ] 브랜드 팔로워 수를 집계하여 표시한다

#### 데이터 모델
```sql
CREATE TABLE brand_page (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    brand_id            BIGINT          NOT NULL COMMENT '브랜드 ID (product-service)',
    logo_url            VARCHAR(500)    NOT NULL COMMENT '브랜드 로고 URL',
    background_url      VARCHAR(500)    NULL COMMENT '배경 이미지 URL',
    description         TEXT            NULL COMMENT '브랜드 소개',
    official_url        VARCHAR(500)    NULL COMMENT '공식 사이트 URL',
    follower_count      INT             NOT NULL DEFAULT 0 COMMENT '팔로워 수',
    is_active           TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '활성 여부',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_brand_id (brand_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='브랜드관 페이지';

CREATE TABLE brand_follow (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    brand_id            BIGINT          NOT NULL COMMENT '브랜드 ID',
    member_id           BIGINT          NOT NULL COMMENT '회원 ID',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_brand_member (brand_id, member_id),
    INDEX idx_member_id (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='브랜드 팔로우';
```

#### API 스펙
```
GET /api/v1/display/brands?sort={sort}&category={category}
Authorization: Bearer {token}

Parameters:
- sort: NAME_ASC (기본), FOLLOWER_DESC, PRODUCT_COUNT_DESC
- category: 카테고리 코드 (선택)

Response: 200 OK
{
    "totalCount": 150,
    "items": [
        {
            "brandId": 1,
            "name": "Nike",
            "logoUrl": "https://cdn.closet.com/brands/nike-logo.png",
            "followerCount": 25000,
            "productCount": 320,
            "isFollowing": true
        }
    ]
}

GET /api/v1/display/brands/{brandId}
Authorization: Bearer {token}

Response: 200 OK
{
    "brandId": 1,
    "name": "Nike",
    "logoUrl": "https://cdn.closet.com/brands/nike-logo.png",
    "backgroundUrl": "https://cdn.closet.com/brands/nike-bg.jpg",
    "description": "Just Do It. 나이키는 세계 최대의 스포츠웨어 브랜드입니다.",
    "officialUrl": "https://www.nike.com",
    "followerCount": 25000,
    "productCount": 320,
    "isFollowing": true,
    "popularProducts": [...],
    "newProducts": [...]
}

POST /api/v1/display/brands/{brandId}/follow
Authorization: Bearer {token}
Response: 201 Created

DELETE /api/v1/display/brands/{brandId}/follow
Authorization: Bearer {token}
Response: 204 No Content
```

---

## 4. 셀러 도메인 (seller-service)

### US-1101: 입점 신청

**As a** 셀러(브랜드)
**I want to** Closet 마켓플레이스에 입점 신청을 하고 싶다
**So that** 내 브랜드의 상품을 판매할 수 있다

#### Acceptance Criteria
- [ ] 입점 신청 시 필수 정보: 브랜드명, 사업자등록번호, 대표자명, 연락처, 이메일
- [ ] 사업자등록증 이미지 첨부 필수 (S3 업로드)
- [ ] 판매 희망 카테고리 선택 (복수 선택 가능)
- [ ] 브랜드 소개 및 주요 상품 설명 입력
- [ ] 신청 상태: `PENDING`(심사 대기) → `REVIEWING`(심사중) → `APPROVED`(승인) / `REJECTED`(반려)
- [ ] 입점 신청 완료 시 `seller.application.submitted` 이벤트를 발행한다
- [ ] 동일 사업자등록번호로 중복 신청 방지

#### 데이터 모델
```sql
CREATE TABLE seller_application (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    brand_name              VARCHAR(100)    NOT NULL COMMENT '브랜드명',
    business_number         VARCHAR(20)     NOT NULL COMMENT '사업자등록번호',
    representative_name     VARCHAR(50)     NOT NULL COMMENT '대표자명',
    phone                   VARCHAR(20)     NOT NULL COMMENT '연락처',
    email                   VARCHAR(100)    NOT NULL COMMENT '이메일',
    business_license_url    VARCHAR(500)    NOT NULL COMMENT '사업자등록증 이미지 URL',
    categories              VARCHAR(500)    NOT NULL COMMENT '판매 희망 카테고리 (JSON)',
    description             TEXT            NULL COMMENT '브랜드 소개',
    status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '신청 상태',
    reject_reason           VARCHAR(500)    NULL COMMENT '반려 사유',
    reviewed_at             DATETIME(6)     NULL COMMENT '심사 완료일',
    reviewed_by             BIGINT          NULL COMMENT '심사자 ID',
    created_at              DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_business_number (business_number),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='셀러 입점 신청';
```

#### API 스펙
```
POST /api/v1/sellers/applications
Content-Type: multipart/form-data

Request:
- brandName: "스트리트 브랜드" (필수)
- businessNumber: "123-45-67890" (필수)
- representativeName: "홍길동" (필수)
- phone: "010-1234-5678" (필수)
- email: "seller@brand.com" (필수)
- businessLicense: [file.jpg] (필수)
- categories: ["TOP", "BOTTOM", "OUTER"] (필수)
- description: "2020년 설립된 스트리트 패션 브랜드..." (선택)

Response: 201 Created
{
    "id": 1,
    "brandName": "스트리트 브랜드",
    "businessNumber": "123-45-67890",
    "status": "PENDING",
    "createdAt": "2026-03-22T10:00:00"
}

GET /api/v1/sellers/applications/my
Authorization: Bearer {token}

Response: 200 OK
{
    "id": 1,
    "brandName": "스트리트 브랜드",
    "status": "REVIEWING",
    "createdAt": "2026-03-22T10:00:00",
    "updatedAt": "2026-03-23T09:00:00"
}
```

---

### US-1102: 입점 심사

**As a** 운영 관리자
**I want to** 셀러 입점 신청을 심사하고 승인/반려하고 싶다
**So that** 품질 기준에 맞는 셀러만 입점시킬 수 있다

#### Acceptance Criteria
- [ ] 심사 대기 중인 입점 신청 목록을 조회할 수 있다
- [ ] 입점 신청 상세 정보(사업자등록증 포함)를 확인할 수 있다
- [ ] 승인 시 수수료율을 설정한다 (카테고리별 10~30%)
- [ ] 승인 시 셀러 계정이 자동 생성된다 (seller 테이블)
- [ ] 반려 시 반려 사유를 필수로 입력한다
- [ ] 승인/반려 시 셀러에게 이메일 알림을 발송한다
- [ ] `seller.application.approved`, `seller.application.rejected` 이벤트를 발행한다

#### 데이터 모델
```sql
CREATE TABLE seller (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    application_id          BIGINT          NOT NULL COMMENT '입점 신청 ID',
    member_id               BIGINT          NULL COMMENT '회원 ID',
    brand_name              VARCHAR(100)    NOT NULL COMMENT '브랜드명',
    business_number         VARCHAR(20)     NOT NULL COMMENT '사업자등록번호',
    commission_rate         DECIMAL(5,2)    NOT NULL COMMENT '수수료율 (%)',
    status                  VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '상태: ACTIVE, SUSPENDED, WITHDRAWN',
    settlement_account_bank VARCHAR(30)     NULL COMMENT '정산 계좌 은행',
    settlement_account_no   VARCHAR(30)     NULL COMMENT '정산 계좌 번호',
    settlement_account_holder VARCHAR(50)   NULL COMMENT '정산 계좌 예금주',
    created_at              DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_application (application_id),
    UNIQUE KEY uk_business_number (business_number),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='셀러';
```

#### API 스펙
```
GET /api/v1/sellers/applications?status={status}&page={page}&size={size}
Authorization: Bearer {token} (admin)

Response: 200 OK
{
    "totalCount": 10,
    "items": [
        {
            "id": 1,
            "brandName": "스트리트 브랜드",
            "businessNumber": "123-45-67890",
            "categories": ["TOP", "BOTTOM"],
            "status": "PENDING",
            "createdAt": "2026-03-22T10:00:00"
        }
    ]
}

POST /api/v1/sellers/applications/{applicationId}/approve
Authorization: Bearer {token} (admin)
Content-Type: application/json

Request:
{
    "commissionRate": 15.0
}

Response: 200 OK
{
    "applicationId": 1,
    "sellerId": 1,
    "brandName": "스트리트 브랜드",
    "commissionRate": 15.0,
    "status": "APPROVED"
}

POST /api/v1/sellers/applications/{applicationId}/reject
Authorization: Bearer {token} (admin)
Content-Type: application/json

Request:
{
    "rejectReason": "사업자등록증이 유효하지 않습니다. 유효한 사업자등록증을 다시 첨부해주세요."
}

Response: 200 OK
{
    "applicationId": 1,
    "status": "REJECTED",
    "rejectReason": "사업자등록증이 유효하지 않습니다..."
}
```

---

### US-1103: 셀러 상품 관리

**As a** 셀러
**I want to** 셀러 어드민에서 내 상품을 등록/수정/삭제하고 싶다
**So that** 판매할 상품 카탈로그를 관리할 수 있다

#### Acceptance Criteria
- [ ] 승인된 셀러만 상품 관리 기능에 접근 가능하다
- [ ] 상품 등록 시 product-service에 API 호출로 상품을 생성한다 (셀러 ID 포함)
- [ ] 셀러는 자신이 등록한 상품만 수정/삭제할 수 있다
- [ ] 상품 상태 관리: `DRAFT`(초안) → `PENDING_REVIEW`(검수 대기) → `ON_SALE`(판매중) → `SOLD_OUT`(품절) → `HIDDEN`(숨김)
- [ ] 상품 등록 시 운영팀 검수를 거친다 (자동 승인 또는 수동 승인)
- [ ] 셀러 어드민 대시보드에서 등록 상품 목록, 상태별 필터 조회 가능

#### API 스펙
```
POST /api/v1/sellers/me/products
Authorization: Bearer {token} (seller)
Content-Type: application/json

Request:
{
    "name": "오버핏 티셔츠",
    "categoryCode": "TOP",
    "brandId": 1,
    "price": 39900,
    "description": "편안한 오버핏 실루엣의 데일리 티셔츠",
    "options": [
        { "size": "S", "color": "BLACK", "sku": "OVT-BLK-S", "stock": 50, "price": 39900 },
        { "size": "M", "color": "BLACK", "sku": "OVT-BLK-M", "stock": 100, "price": 39900 },
        { "size": "L", "color": "BLACK", "sku": "OVT-BLK-L", "stock": 80, "price": 39900 }
    ],
    "images": ["https://cdn.closet.com/products/ovt-1.jpg"]
}

Response: 201 Created
{
    "productId": 500,
    "sellerId": 1,
    "name": "오버핏 티셔츠",
    "status": "PENDING_REVIEW",
    "createdAt": "2026-03-22T10:00:00"
}

GET /api/v1/sellers/me/products?status={status}&page={page}&size={size}
Authorization: Bearer {token} (seller)

Response: 200 OK
{
    "totalCount": 25,
    "items": [...]
}
```

---

### US-1104: 셀러 주문 관리

**As a** 셀러
**I want to** 내 상품에 대한 주문을 확인하고 발송 처리를 하고 싶다
**So that** 구매자에게 빠르게 상품을 배송할 수 있다

#### Acceptance Criteria
- [ ] 셀러는 자신의 상품이 포함된 주문만 조회할 수 있다
- [ ] 주문 상태별 필터: `PAID`(결제 완료), `SHIPPING`(배송중), `DELIVERED`(배송 완료), `CONFIRMED`(구매 확정)
- [ ] 결제 완료 주문에 대해 송장을 등록하여 발송 처리할 수 있다
- [ ] 송장 등록 시 shipping-service에 API 호출로 처리한다
- [ ] 일괄 발송 처리: 여러 주문을 한 번에 송장 등록할 수 있다 (엑셀 업로드)
- [ ] 주문 건에 대한 메모 기능 (셀러 내부용)

#### API 스펙
```
GET /api/v1/sellers/me/orders?status={status}&startDate={start}&endDate={end}&page={page}&size={size}
Authorization: Bearer {token} (seller)

Response: 200 OK
{
    "totalCount": 150,
    "statusSummary": {
        "PAID": 10,
        "SHIPPING": 25,
        "DELIVERED": 100,
        "CONFIRMED": 15
    },
    "items": [
        {
            "orderId": 12345,
            "orderNumber": "ORD-2026032200001",
            "buyerName": "김구매",
            "productName": "오버핏 티셔츠 (BLACK/M)",
            "quantity": 2,
            "totalAmount": 79800,
            "status": "PAID",
            "orderedAt": "2026-03-22T10:00:00"
        }
    ]
}

POST /api/v1/sellers/me/orders/{orderId}/ship
Authorization: Bearer {token} (seller)
Content-Type: application/json

Request:
{
    "carrier": "CJ_LOGISTICS",
    "trackingNumber": "1234567890123"
}

Response: 200 OK
{
    "orderId": 12345,
    "status": "SHIPPING",
    "carrier": "CJ_LOGISTICS",
    "trackingNumber": "1234567890123",
    "shippedAt": "2026-03-22T14:00:00"
}
```

---

### US-1105: 셀러 통계

**As a** 셀러
**I want to** 내 판매 현황을 한눈에 파악하고 싶다
**So that** 매출 트렌드와 인기 상품을 분석하여 판매 전략을 세울 수 있다

#### Acceptance Criteria
- [ ] 대시보드 요약: 오늘 주문 수, 총 매출, 미처리 주문, 평균 리뷰 점수
- [ ] 기간별 매출 추이: 일별/주별/월별 매출 그래프 데이터
- [ ] 상품별 판매량 Top 10
- [ ] 리뷰 평균 별점, 최근 리뷰 목록
- [ ] 통계 데이터는 Redis에 캐싱한다 (TTL: 1시간)
- [ ] 상세 통계 조회 시 기간 필터 (최대 90일)

#### API 스펙
```
GET /api/v1/sellers/me/statistics/summary
Authorization: Bearer {token} (seller)

Response: 200 OK
{
    "todayOrders": 15,
    "todayRevenue": 1250000,
    "pendingOrders": 5,
    "avgReviewRating": 4.5,
    "totalProducts": 25,
    "totalRevenue": 45000000,
    "period": "2026-03-22"
}

GET /api/v1/sellers/me/statistics/sales?startDate={start}&endDate={end}&unit={unit}
Authorization: Bearer {token} (seller)

Parameters:
- startDate: 시작일 (필수)
- endDate: 종료일 (필수)
- unit: DAY (기본), WEEK, MONTH

Response: 200 OK
{
    "items": [
        { "date": "2026-03-20", "orderCount": 12, "revenue": 980000 },
        { "date": "2026-03-21", "orderCount": 18, "revenue": 1450000 },
        { "date": "2026-03-22", "orderCount": 15, "revenue": 1250000 }
    ]
}

GET /api/v1/sellers/me/statistics/top-products?limit={limit}
Authorization: Bearer {token} (seller)

Response: 200 OK
{
    "items": [
        {
            "productId": 500,
            "productName": "오버핏 티셔츠",
            "salesCount": 320,
            "revenue": 12768000,
            "avgRating": 4.7
        }
    ]
}
```

---

## 5. CS 도메인 (cs-service)

### US-1201: 1:1 문의

**As a** 구매자
**I want to** 상품이나 주문에 대해 1:1 문의를 등록하고 싶다
**So that** 궁금한 점이나 문제를 해결할 수 있다

#### Acceptance Criteria
- [ ] 문의 카테고리: `PRODUCT`(상품), `ORDER`(주문), `SHIPPING`(배송), `PAYMENT`(결제), `RETURN`(반품/교환), `ETC`(기타)
- [ ] 문의 등록 시 제목, 내용, 첨부 이미지(최대 3장)를 입력한다
- [ ] 상품 관련 문의는 해당 셀러에게 답변 요청이 전달된다
- [ ] 일반 문의는 운영팀이 답변한다
- [ ] 문의 상태: `WAITING`(답변 대기) → `ANSWERED`(답변 완료) → `CLOSED`(종료)
- [ ] 답변 등록 시 문의자에게 알림을 발송한다
- [ ] 추가 문의(답변에 대한 재문의)를 등록할 수 있다
- [ ] 내 문의 내역을 조회할 수 있다

#### 데이터 모델
```sql
CREATE TABLE inquiry (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    member_id           BIGINT          NOT NULL COMMENT '문의자 ID',
    category            VARCHAR(20)     NOT NULL COMMENT '문의 카테고리',
    title               VARCHAR(200)    NOT NULL COMMENT '문의 제목',
    content             TEXT            NOT NULL COMMENT '문의 내용',
    product_id          BIGINT          NULL COMMENT '관련 상품 ID',
    order_id            BIGINT          NULL COMMENT '관련 주문 ID',
    seller_id           BIGINT          NULL COMMENT '답변 담당 셀러 ID',
    status              VARCHAR(20)     NOT NULL DEFAULT 'WAITING' COMMENT '상태',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_member_id (member_id),
    INDEX idx_status (status),
    INDEX idx_seller_id (seller_id),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='1:1 문의';

CREATE TABLE inquiry_image (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    inquiry_id          BIGINT          NOT NULL COMMENT '문의 ID',
    image_url           VARCHAR(500)    NOT NULL COMMENT '이미지 URL',
    display_order       INT             NOT NULL DEFAULT 0 COMMENT '표시 순서',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_inquiry_id (inquiry_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='문의 첨부 이미지';

CREATE TABLE inquiry_answer (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    inquiry_id          BIGINT          NOT NULL COMMENT '문의 ID',
    answerer_id         BIGINT          NOT NULL COMMENT '답변자 ID',
    answerer_type       VARCHAR(20)     NOT NULL COMMENT '답변자 유형: ADMIN, SELLER',
    content             TEXT            NOT NULL COMMENT '답변 내용',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_inquiry_id (inquiry_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='문의 답변';
```

#### API 스펙
```
POST /api/v1/cs/inquiries
Authorization: Bearer {token}
Content-Type: multipart/form-data

Request:
- category: "PRODUCT" (필수)
- title: "사이즈 문의" (필수)
- content: "이 상품은 일반 사이즈인가요? 오버핏인가요?" (필수)
- productId: 100 (선택)
- orderId: null (선택)
- images: [file1.jpg] (선택, 최대 3장)

Response: 201 Created
{
    "id": 1,
    "category": "PRODUCT",
    "title": "사이즈 문의",
    "status": "WAITING",
    "createdAt": "2026-03-22T10:00:00"
}

GET /api/v1/cs/inquiries/my?status={status}&page={page}&size={size}
Authorization: Bearer {token}

Response: 200 OK
{
    "totalCount": 5,
    "items": [
        {
            "id": 1,
            "category": "PRODUCT",
            "title": "사이즈 문의",
            "status": "ANSWERED",
            "createdAt": "2026-03-22T10:00:00",
            "answeredAt": "2026-03-22T14:00:00"
        }
    ]
}

POST /api/v1/cs/inquiries/{inquiryId}/answers
Authorization: Bearer {token} (admin/seller)
Content-Type: application/json

Request:
{
    "content": "안녕하세요. 해당 상품은 레귤러핏입니다. 평소 사이즈로 주문해주시면 됩니다."
}

Response: 201 Created
{
    "id": 1,
    "inquiryId": 1,
    "content": "안녕하세요. 해당 상품은 레귤러핏입니다...",
    "answererType": "SELLER",
    "createdAt": "2026-03-22T14:00:00"
}
```

---

### US-1202: FAQ 관리

**As a** 구매자
**I want to** 자주 묻는 질문과 답변을 카테고리별로 확인하고 싶다
**So that** 1:1 문의 없이도 궁금한 점을 빠르게 해결할 수 있다

#### Acceptance Criteria
- [ ] FAQ 카테고리: `PRODUCT`(상품), `ORDER`(주문), `SHIPPING`(배송), `PAYMENT`(결제), `RETURN`(반품/교환), `MEMBER`(회원), `ETC`(기타)
- [ ] 관리자가 FAQ를 등록/수정/삭제할 수 있다
- [ ] FAQ 노출 순서를 관리할 수 있다
- [ ] FAQ 검색 기능 (키워드 기반)
- [ ] FAQ 조회 수를 기록하여 인기 FAQ를 제공한다

#### 데이터 모델
```sql
CREATE TABLE faq (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    category            VARCHAR(20)     NOT NULL COMMENT 'FAQ 카테고리',
    question            VARCHAR(500)    NOT NULL COMMENT '질문',
    answer              TEXT            NOT NULL COMMENT '답변',
    display_order       INT             NOT NULL DEFAULT 0 COMMENT '노출 순서',
    view_count          INT             NOT NULL DEFAULT 0 COMMENT '조회 수',
    is_active           TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '노출 여부',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_category (category, is_active),
    INDEX idx_view_count (view_count DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='FAQ';
```

#### API 스펙
```
GET /api/v1/cs/faqs?category={category}&keyword={keyword}
Authorization: Bearer {token}

Response: 200 OK
{
    "totalCount": 20,
    "items": [
        {
            "id": 1,
            "category": "SHIPPING",
            "question": "배송은 얼마나 걸리나요?",
            "answer": "일반 배송은 결제 완료 후 2~3일 이내에 도착합니다...",
            "viewCount": 1520
        }
    ]
}

POST /api/v1/cs/faqs
Authorization: Bearer {token} (admin)
Content-Type: application/json

Request:
{
    "category": "SHIPPING",
    "question": "배송은 얼마나 걸리나요?",
    "answer": "일반 배송은 결제 완료 후 2~3일 이내에 도착합니다. 도서산간 지역은 1~2일 추가 소요될 수 있습니다.",
    "displayOrder": 1
}

Response: 201 Created
{ ... }
```

---

### US-1203: 반품/교환 접수 관리

**As a** 구매자
**I want to** CS 센터를 통해 반품/교환을 접수하고 진행 상황을 확인하고 싶다
**So that** 체계적으로 반품/교환 절차를 진행할 수 있다

#### Acceptance Criteria
- [ ] 반품/교환 접수 유형: `RETURN`(반품), `EXCHANGE`(교환)
- [ ] 접수 사유: `DEFECTIVE`(불량), `WRONG_ITEM`(오배송), `CHANGE_OF_MIND`(단순변심), `SIZE_MISMATCH`(사이즈 불일치)
- [ ] 접수 시 사유 상세 설명과 증거 사진(최대 5장) 첨부 가능
- [ ] 접수 상태: `RECEIVED`(접수) → `REVIEWING`(검토) → `APPROVED`(승인) → `PICKUP_SCHEDULED`(수거 예정) → `COMPLETED`(완료) / `REJECTED`(거절)
- [ ] 승인 시 shipping-service에 반품 수거 요청 API를 호출한다
- [ ] 교환의 경우 교환 희망 옵션(사이즈/색상)을 입력할 수 있다
- [ ] 반품/교환 접수 내역과 현재 상태를 조회할 수 있다
- [ ] `cs.claim.created`, `cs.claim.approved`, `cs.claim.completed` 이벤트를 발행한다

#### 데이터 모델
```sql
CREATE TABLE claim (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    member_id           BIGINT          NOT NULL COMMENT '회원 ID',
    order_id            BIGINT          NOT NULL COMMENT '주문 ID',
    order_item_id       BIGINT          NOT NULL COMMENT '주문 상품 ID',
    type                VARCHAR(20)     NOT NULL COMMENT '유형: RETURN, EXCHANGE',
    reason              VARCHAR(30)     NOT NULL COMMENT '사유',
    description         TEXT            NULL COMMENT '상세 설명',
    exchange_option     VARCHAR(200)    NULL COMMENT '교환 희망 옵션 (JSON)',
    status              VARCHAR(20)     NOT NULL DEFAULT 'RECEIVED' COMMENT '상태',
    reject_reason       VARCHAR(500)    NULL COMMENT '거절 사유',
    processed_by        BIGINT          NULL COMMENT '처리자 ID',
    processed_at        DATETIME(6)     NULL COMMENT '처리 완료일',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_member_id (member_id),
    INDEX idx_order_id (order_id),
    INDEX idx_status (status),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='반품/교환 접수';

CREATE TABLE claim_image (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    claim_id            BIGINT          NOT NULL COMMENT '접수 ID',
    image_url           VARCHAR(500)    NOT NULL COMMENT '이미지 URL',
    display_order       INT             NOT NULL DEFAULT 0 COMMENT '표시 순서',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_claim_id (claim_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='접수 첨부 이미지';
```

#### API 스펙
```
POST /api/v1/cs/claims
Authorization: Bearer {token}
Content-Type: multipart/form-data

Request:
- orderId: 12345 (필수)
- orderItemId: 67890 (필수)
- type: "RETURN" (필수)
- reason: "SIZE_MISMATCH" (필수)
- description: "M 사이즈를 주문했는데 생각보다 작습니다" (선택)
- exchangeOption: null (교환 시 필수)
- images: [file1.jpg, file2.jpg] (선택, 최대 5장)

Response: 201 Created
{
    "id": 1,
    "type": "RETURN",
    "reason": "SIZE_MISMATCH",
    "status": "RECEIVED",
    "createdAt": "2026-03-22T10:00:00"
}

GET /api/v1/cs/claims/my?page={page}&size={size}
Authorization: Bearer {token}

Response: 200 OK
{
    "totalCount": 2,
    "items": [
        {
            "id": 1,
            "orderId": 12345,
            "productName": "오버핏 티셔츠 (BLACK/M)",
            "type": "RETURN",
            "reason": "SIZE_MISMATCH",
            "status": "APPROVED",
            "createdAt": "2026-03-22T10:00:00"
        }
    ]
}

POST /api/v1/cs/claims/{claimId}/approve
Authorization: Bearer {token} (admin)

Response: 200 OK
{
    "claimId": 1,
    "status": "APPROVED",
    "approvedAt": "2026-03-23T09:00:00"
}

POST /api/v1/cs/claims/{claimId}/reject
Authorization: Bearer {token} (admin)
Content-Type: application/json

Request:
{
    "rejectReason": "착용 흔적이 확인되어 반품이 불가합니다."
}

Response: 200 OK
{
    "claimId": 1,
    "status": "REJECTED",
    "rejectReason": "착용 흔적이 확인되어 반품이 불가합니다."
}
```

---

### US-1204: 문의 통계

**As a** 운영 관리자
**I want to** 문의 유형별 건수와 평균 답변 시간을 확인하고 싶다
**So that** CS 운영 효율을 분석하고 개선할 수 있다

#### Acceptance Criteria
- [ ] 기간별 문의 건수 집계 (일별/주별/월별)
- [ ] 카테고리별 문의 건수 분포
- [ ] 평균 답변 시간 (문의 등록 ~ 첫 답변)
- [ ] 미답변 문의 수 (24시간 이상 경과)
- [ ] 반품/교환 접수 건수 및 처리율
- [ ] 셀러별 답변 현황 (답변률, 평균 답변 시간)
- [ ] 통계 데이터는 일간 배치로 집계한다

#### API 스펙
```
GET /api/v1/cs/statistics?startDate={start}&endDate={end}
Authorization: Bearer {token} (admin)

Response: 200 OK
{
    "period": { "startDate": "2026-03-01", "endDate": "2026-03-22" },
    "inquirySummary": {
        "totalCount": 350,
        "answeredCount": 320,
        "waitingCount": 30,
        "avgResponseTimeMinutes": 240,
        "overdue24hCount": 5,
        "categoryDistribution": {
            "PRODUCT": 120,
            "ORDER": 80,
            "SHIPPING": 60,
            "PAYMENT": 40,
            "RETURN": 35,
            "ETC": 15
        }
    },
    "claimSummary": {
        "totalCount": 45,
        "returnCount": 30,
        "exchangeCount": 15,
        "completedRate": 93.3,
        "reasonDistribution": {
            "CHANGE_OF_MIND": 20,
            "SIZE_MISMATCH": 12,
            "DEFECTIVE": 8,
            "WRONG_ITEM": 5
        }
    }
}
```

---

## 6. 서비스 간 이벤트 흐름

```
order-service                promotion-service             display-service
     │                            │                              │
     │  order.created ───────────>│ (쿠폰 사용 처리)              │
     │  order.created ──────────────────────────────────────────>│ (랭킹 갱신)
     │  order.cancelled ─────────>│ (쿠폰 복구)                  │
     │  order.confirmed ─────────>│ (적립금 적립)                 │
     │                            │                              │

review-service               display-service               seller-service
     │                            │                              │
     │  review.created ──────────>│ (랭킹 리뷰 스코어 갱신)       │
     │                            │                              │

seller-service               product-service               cs-service
     │                            │                              │
     │  seller.approved ─────────>│ (브랜드 등록)                 │
     │                            │                              │
     │                            │                   cs.claim.approved ──> shipping-service
     │                            │                   (반품 수거 요청)

promotion-service            notification-service (Phase 4)
     │                            │
     │  timesale.started ────────>│ (타임세일 알림)
     │  coupon.issued ───────────>│ (쿠폰 발급 알림)

display-service              search-service
     │                            │
     │  상품 조회 ────────────────>│ (브랜드관 상품 목록)
```

---

## 7. 기술 의사결정

| 결정 | 선택 | 이유 |
|------|------|------|
| 쿠폰 동시성 제어 | Redis Lua Script (DECR) | 원자적 연산으로 선착순 발급 동시성 보장, Redisson보다 가벼움 |
| 랭킹 저장소 | Redis Sorted Set (ZSET) | O(log N) 스코어 갱신, O(log N + M) 범위 조회, 실시간 랭킹에 최적 |
| 랭킹 스코어 갱신 | 이벤트 기반 비동기 | 주문/리뷰/조회 이벤트 수신으로 실시간 갱신, 서비스 결합도 낮음 |
| 배너/기획전 캐싱 | Redis (TTL 5분) | 메인 페이지 응답 속도 보장, 잦은 변경 없는 데이터에 적합 |
| 셀러 상품 등록 | API 호출 (동기) | 상품 데이터 정합성 보장, 검수 프로세스 동기 처리 필요 |
| CS 파일 첨부 | AWS S3 | 이미지 용량 제한 없음, CDN 연동, 비용 효율 |
| 적립금 만료 처리 | Spring Scheduler (일간 배치) | 대량 만료 처리에 배치가 적합, 실시간 처리 불필요 |

---

## 8. 마일스톤

| Sprint | 기간 | 목표 |
|--------|------|------|
| Sprint 9 | Week 1-2 | 프로모션 서비스: 쿠폰 정책/발급 (US-901, US-902) |
| Sprint 10 | Week 3-4 | 프로모션 서비스: 쿠폰 적용/타임세일/적립금 (US-903~905) + 전시 서비스: 메인/랭킹 (US-1001, US-1002) |
| Sprint 11 | Week 5-6 | 전시 서비스: 기획전/배너/브랜드관 (US-1003~1005) |
| Sprint 12 | Week 7-8 | 셀러 서비스: 입점/심사/상품/주문 관리 (US-1101~1104) |
| Sprint 13 | Week 9-10 | 셀러 통계 (US-1105) + CS 서비스 전체 (US-1201~1204) + 통합 테스트 |
