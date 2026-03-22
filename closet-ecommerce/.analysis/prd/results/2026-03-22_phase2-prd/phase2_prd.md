# Phase 2 상세 PRD: 성장 단계

> 작성일: 2026-03-22
> 프로젝트: Closet E-commerce
> Phase: 2 (성장)
> 도메인: 배송 + 재고 + 검색 + 리뷰

---

## 1. Phase 2 개요

### 목표
Phase 1에서 구축한 핵심 커머스 기반(상품, 주문, 결제, 회원) 위에 **배송 추적, 재고 관리, 검색 고도화, 리뷰 시스템**을 추가하여 사용자 경험을 완성하고 재구매율을 높인다.

### 기간
- 예상 기간: 8주 (Phase 1 완료 후)
- Sprint 5-8

### 서비스 구성
| 서비스 | 포트 | 기술 스택 |
|--------|------|----------|
| shipping-service | 8084 | Kotlin, Spring Boot 3.x, JPA |
| inventory-service | 8085 | Kotlin, Spring Boot 3.x, JPA, Redis |
| search-service | 8086 | Kotlin, Spring Boot 3.x, Elasticsearch |
| review-service | 8087 | Kotlin, Spring Boot 3.x, JPA |

---

## 2. 배송 도메인 (shipping-service)

### US-501: 송장 등록 + 주문 상태 변경

**As a** 판매자
**I want to** 주문 건에 대해 택배사와 송장번호를 등록하고 싶다
**So that** 구매자가 배송 상태를 확인할 수 있다

#### Acceptance Criteria
- [ ] 판매자는 주문 건에 택배사(carrier)와 송장번호(tracking_number)를 등록할 수 있다
- [ ] 송장 등록 시 주문 상태가 `PAID` → `SHIPPING`으로 변경된다
- [ ] 주문 상태 변경 이벤트가 Kafka로 발행된다 (`order.status.changed`)
- [ ] 이미 송장이 등록된 주문에 대해 중복 등록 시 에러를 반환한다
- [ ] 송장번호 형식 검증 (숫자 10-15자리)

#### 데이터 모델
```sql
CREATE TABLE shipping (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    order_id        BIGINT          NOT NULL COMMENT '주문 ID',
    carrier         VARCHAR(50)     NOT NULL COMMENT '택배사 코드',
    tracking_number VARCHAR(50)     NOT NULL COMMENT '송장번호',
    status          VARCHAR(20)     NOT NULL DEFAULT 'READY' COMMENT '배송상태: READY, IN_TRANSIT, DELIVERED',
    shipped_at      DATETIME(6)     NULL COMMENT '발송일시',
    delivered_at    DATETIME(6)     NULL COMMENT '배송완료일시',
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_id (order_id),
    INDEX idx_tracking (carrier, tracking_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='배송 정보';
```

#### API 스펙
```
POST /api/v1/shippings
Authorization: Bearer {token}
Content-Type: application/json

Request:
{
    "orderId": 12345,
    "carrier": "CJ_LOGISTICS",
    "trackingNumber": "1234567890123"
}

Response: 201 Created
{
    "id": 1,
    "orderId": 12345,
    "carrier": "CJ_LOGISTICS",
    "trackingNumber": "1234567890123",
    "status": "READY",
    "createdAt": "2026-03-22T10:00:00"
}
```

---

### US-502: 택배사 API 연동 배송 추적

**As a** 구매자
**I want to** 내 주문의 실시간 배송 상태를 확인하고 싶다
**So that** 언제 상품이 도착하는지 알 수 있다

#### Acceptance Criteria
- [ ] 구매자는 주문 상세에서 배송 추적 정보를 조회할 수 있다
- [ ] 택배사 API(스마트택배 등)를 통해 실시간 배송 상태를 조회한다
- [ ] 배송 상태: `READY`(준비) → `IN_TRANSIT`(배송중) → `DELIVERED`(배송완료)
- [ ] 배송 추적 이력(트래킹 로그)을 시간순으로 표시한다
- [ ] 택배사 API 장애 시 마지막 캐싱된 정보를 반환하고 에러를 로깅한다
- [ ] 배송 추적 정보는 Redis에 5분간 캐싱한다

#### 데이터 모델
```sql
CREATE TABLE shipping_tracking_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    shipping_id     BIGINT          NOT NULL COMMENT '배송 ID',
    status          VARCHAR(50)     NOT NULL COMMENT '추적 상태',
    location        VARCHAR(200)    NULL COMMENT '위치',
    description     VARCHAR(500)    NULL COMMENT '상세 설명',
    tracked_at      DATETIME(6)     NOT NULL COMMENT '추적 일시',
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_shipping_id (shipping_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='배송 추적 로그';
```

#### API 스펙
```
GET /api/v1/shippings/{shippingId}/tracking
Authorization: Bearer {token}

Response: 200 OK
{
    "shippingId": 1,
    "carrier": "CJ_LOGISTICS",
    "trackingNumber": "1234567890123",
    "currentStatus": "IN_TRANSIT",
    "estimatedDelivery": "2026-03-24",
    "trackingLogs": [
        {
            "status": "PICKED_UP",
            "location": "서울 강남 집하점",
            "description": "상품을 인수하였습니다",
            "trackedAt": "2026-03-22T14:00:00"
        },
        {
            "status": "IN_TRANSIT",
            "location": "경기 용인 허브",
            "description": "간선 상차하였습니다",
            "trackedAt": "2026-03-22T18:00:00"
        }
    ]
}
```

---

### US-503: 자동 구매확정 (7일)

**As a** 시스템 관리자
**I want to** 배송 완료 후 7일이 경과하면 자동으로 구매를 확정하고 싶다
**So that** 판매자의 정산이 지연되지 않고, 구매자의 별도 액션 없이도 거래가 완료된다

#### Acceptance Criteria
- [ ] 배송 완료(`DELIVERED`) 후 7일이 경과하면 자동으로 구매확정(`CONFIRMED`) 상태로 변경된다
- [ ] 구매자가 수동으로 구매확정을 할 수 있다 (7일 이전에도 가능)
- [ ] 구매확정 시 정산 이벤트가 Kafka로 발행된다 (`order.confirmed`)
- [ ] 구매확정 후에는 반품/교환 신청이 불가능하다
- [ ] 자동 구매확정은 Spring Scheduler (매일 00:00)로 배치 처리한다
- [ ] 반품/교환 진행 중인 건은 자동 구매확정에서 제외한다

#### API 스펙
```
POST /api/v1/orders/{orderId}/confirm
Authorization: Bearer {token}

Response: 200 OK
{
    "orderId": 12345,
    "status": "CONFIRMED",
    "confirmedAt": "2026-03-29T00:00:00"
}
```

---

### US-504: 반품 신청 + 수거 + 검수 + 환불

**As a** 구매자
**I want to** 상품에 문제가 있거나 마음에 들지 않을 때 반품을 신청하고 싶다
**So that** 상품을 반환하고 결제 금액을 환불받을 수 있다

#### Acceptance Criteria
- [ ] 배송 완료 후 7일 이내에 반품 신청이 가능하다
- [ ] 반품 사유를 선택해야 한다: `DEFECTIVE`(불량), `WRONG_ITEM`(오배송), `CHANGE_OF_MIND`(단순변심), `SIZE_MISMATCH`(사이즈 불일치)
- [ ] 반품 상태 흐름: `REQUESTED` → `PICKUP_SCHEDULED` → `PICKUP_COMPLETED` → `INSPECTING` → `APPROVED`/`REJECTED`
- [ ] `CHANGE_OF_MIND` 반품은 반품 배송비(3,000원)를 구매자가 부담한다
- [ ] `DEFECTIVE`, `WRONG_ITEM` 반품은 판매자가 배송비를 부담한다
- [ ] 반품 승인(`APPROVED`) 시 결제 취소 API를 호출하여 환불 처리한다
- [ ] 반품 거절(`REJECTED`) 시 거절 사유를 기재하고 구매자에게 알림을 발송한다
- [ ] 재고 서비스에 반품 완료 이벤트를 발행하여 재고를 복구한다

#### 데이터 모델
```sql
CREATE TABLE return_request (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    order_id            BIGINT          NOT NULL COMMENT '주문 ID',
    order_item_id       BIGINT          NOT NULL COMMENT '주문 상품 ID',
    reason              VARCHAR(30)     NOT NULL COMMENT '반품 사유',
    reason_detail       VARCHAR(500)    NULL COMMENT '상세 사유',
    status              VARCHAR(20)     NOT NULL DEFAULT 'REQUESTED' COMMENT '반품 상태',
    shipping_fee_bearer VARCHAR(10)     NOT NULL COMMENT '배송비 부담: BUYER, SELLER',
    shipping_fee        INT             NOT NULL DEFAULT 0 COMMENT '반품 배송비',
    reject_reason       VARCHAR(500)    NULL COMMENT '거절 사유',
    requested_at        DATETIME(6)     NOT NULL,
    completed_at        DATETIME(6)     NULL,
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_order_id (order_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='반품 요청';
```

#### API 스펙
```
POST /api/v1/returns
Authorization: Bearer {token}
Content-Type: application/json

Request:
{
    "orderId": 12345,
    "orderItemId": 67890,
    "reason": "SIZE_MISMATCH",
    "reasonDetail": "L사이즈 주문했으나 실제 핏이 너무 큽니다"
}

Response: 201 Created
{
    "id": 1,
    "orderId": 12345,
    "orderItemId": 67890,
    "reason": "SIZE_MISMATCH",
    "status": "REQUESTED",
    "shippingFeeBearer": "BUYER",
    "shippingFee": 3000,
    "requestedAt": "2026-03-25T10:00:00"
}
```

---

### US-505: 교환 신청 (사이즈 교환)

**As a** 구매자
**I want to** 사이즈가 맞지 않는 상품을 다른 사이즈로 교환하고 싶다
**So that** 반품/재주문 없이 원하는 사이즈의 상품을 받을 수 있다

#### Acceptance Criteria
- [ ] 배송 완료 후 7일 이내에 교환 신청이 가능하다
- [ ] 동일 상품의 다른 옵션(사이즈/색상)으로만 교환 가능하다
- [ ] 교환 희망 옵션의 재고가 있어야 교환 신청이 가능하다
- [ ] 교환 상태 흐름: `REQUESTED` → `PICKUP_SCHEDULED` → `PICKUP_COMPLETED` → `RESHIPPING` → `COMPLETED`
- [ ] 교환 시 왕복 배송비(6,000원)는 구매자 부담 (단순변심인 경우)
- [ ] 불량/오배송 사유인 경우 배송비 판매자 부담
- [ ] 교환 요청 시 새 옵션의 재고를 선점(예약)하고, 기존 옵션 재고는 수거 완료 시 복구한다

#### 데이터 모델
```sql
CREATE TABLE exchange_request (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    order_id                BIGINT          NOT NULL COMMENT '주문 ID',
    order_item_id           BIGINT          NOT NULL COMMENT '주문 상품 ID',
    reason                  VARCHAR(30)     NOT NULL COMMENT '교환 사유',
    reason_detail           VARCHAR(500)    NULL COMMENT '상세 사유',
    exchange_option_id      BIGINT          NOT NULL COMMENT '교환 희망 옵션 ID',
    status                  VARCHAR(20)     NOT NULL DEFAULT 'REQUESTED' COMMENT '교환 상태',
    shipping_fee_bearer     VARCHAR(10)     NOT NULL COMMENT '배송비 부담',
    shipping_fee            INT             NOT NULL DEFAULT 0 COMMENT '교환 배송비',
    requested_at            DATETIME(6)     NOT NULL,
    completed_at            DATETIME(6)     NULL,
    created_at              DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_order_id (order_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='교환 요청';
```

#### API 스펙
```
POST /api/v1/exchanges
Authorization: Bearer {token}
Content-Type: application/json

Request:
{
    "orderId": 12345,
    "orderItemId": 67890,
    "reason": "SIZE_MISMATCH",
    "reasonDetail": "M사이즈로 교환 희망",
    "exchangeOptionId": 99999
}

Response: 201 Created
{
    "id": 1,
    "orderId": 12345,
    "orderItemId": 67890,
    "reason": "SIZE_MISMATCH",
    "exchangeOptionId": 99999,
    "status": "REQUESTED",
    "shippingFeeBearer": "BUYER",
    "shippingFee": 6000,
    "requestedAt": "2026-03-25T11:00:00"
}
```

---

## 3. 재고 도메인 (inventory-service)

### US-601: SKU별 재고 관리 + 주문 시 차감

**As a** 시스템
**I want to** 상품의 SKU별 재고를 관리하고, 주문 시 자동으로 차감하고 싶다
**So that** 재고 부족 상품의 주문을 방지하고 정확한 재고 수량을 유지할 수 있다

#### Acceptance Criteria
- [ ] 상품 옵션(사이즈/색상) 조합별 SKU 단위로 재고를 관리한다
- [ ] 주문 생성 이벤트(`order.created`) 수신 시 해당 SKU의 재고를 차감한다
- [ ] 재고가 부족하면 주문을 거절하고 `inventory.insufficient` 이벤트를 발행한다
- [ ] 주문 취소 이벤트(`order.cancelled`) 수신 시 재고를 복구한다
- [ ] 재고 변경 이력을 기록한다 (차감/복구/입고 사유)
- [ ] 재고 수량은 음수가 될 수 없다

#### 데이터 모델
```sql
CREATE TABLE inventory (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    product_id      BIGINT          NOT NULL COMMENT '상품 ID',
    option_id       BIGINT          NOT NULL COMMENT '옵션 ID (사이즈/색상)',
    sku             VARCHAR(50)     NOT NULL COMMENT 'SKU 코드',
    quantity         INT            NOT NULL DEFAULT 0 COMMENT '현재 재고 수량',
    safety_stock    INT             NOT NULL DEFAULT 10 COMMENT '안전 재고',
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sku (sku),
    UNIQUE KEY uk_product_option (product_id, option_id),
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='재고';

CREATE TABLE inventory_history (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    inventory_id    BIGINT          NOT NULL COMMENT '재고 ID',
    change_type     VARCHAR(20)     NOT NULL COMMENT '변경 유형: DEDUCT, RESTORE, INBOUND, ADJUST',
    change_quantity INT             NOT NULL COMMENT '변경 수량 (음수: 차감, 양수: 증가)',
    before_quantity INT             NOT NULL COMMENT '변경 전 수량',
    after_quantity  INT             NOT NULL COMMENT '변경 후 수량',
    reference_type  VARCHAR(30)     NULL COMMENT '참조 유형: ORDER, RETURN, EXCHANGE, MANUAL',
    reference_id    BIGINT          NULL COMMENT '참조 ID',
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_inventory_id (inventory_id),
    INDEX idx_reference (reference_type, reference_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='재고 변경 이력';
```

#### API 스펙
```
GET /api/v1/inventories?productId={productId}
Authorization: Bearer {token}

Response: 200 OK
{
    "items": [
        {
            "id": 1,
            "productId": 100,
            "optionId": 1,
            "sku": "TSHIRT-BLK-M",
            "quantity": 50,
            "safetyStock": 10
        }
    ]
}

POST /api/v1/inventories/{inventoryId}/deduct
Authorization: Bearer {token} (internal)
Content-Type: application/json

Request:
{
    "quantity": 2,
    "referenceType": "ORDER",
    "referenceId": 12345
}

Response: 200 OK
{
    "inventoryId": 1,
    "sku": "TSHIRT-BLK-M",
    "beforeQuantity": 50,
    "afterQuantity": 48
}
```

---

### US-602: Redis 분산 락 기반 동시성 제어

**As a** 시스템
**I want to** 동시에 여러 주문이 같은 SKU의 재고를 차감할 때 데이터 정합성을 보장하고 싶다
**So that** 재고 초과 판매(overselling)를 방지할 수 있다

#### Acceptance Criteria
- [ ] 재고 차감/복구 시 Redis 분산 락(Redisson)을 사용한다
- [ ] 락 키: `inventory:lock:{sku}`
- [ ] 락 대기 시간: 5초, 락 유지 시간: 3초
- [ ] 락 획득 실패 시 재시도(최대 3회) 후 실패 응답 반환
- [ ] 낙관적 락(JPA `@Version`)을 2차 안전장치로 사용한다
- [ ] 동시성 테스트: 100개 스레드 동시 차감 시 정확한 재고 수량 보장

#### 데이터 모델
- `inventory` 테이블에 `version` 컬럼 추가:
```sql
ALTER TABLE inventory ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '낙관적 락 버전';
```

#### API 스펙
- 별도 API 없음 (US-601의 차감/복구 API 내부에서 자동 적용)

---

### US-603: 안전재고 알림

**As a** 판매자
**I want to** 재고가 안전재고 수량 이하로 떨어지면 알림을 받고 싶다
**So that** 적시에 재입고하여 품절을 방지할 수 있다

#### Acceptance Criteria
- [ ] 재고 차감 후 잔여 수량이 안전재고(safety_stock) 이하이면 알림 이벤트 발행
- [ ] Kafka 이벤트: `inventory.low_stock` (productId, optionId, sku, quantity, safetyStock)
- [ ] 동일 SKU에 대해 24시간 내 중복 알림을 방지한다 (Redis 키로 제어)
- [ ] 안전재고 수량은 SKU별로 설정 가능하다 (기본값: 10)
- [ ] 재고가 0이 되면 `inventory.out_of_stock` 이벤트를 발행한다

#### API 스펙
```
PATCH /api/v1/inventories/{inventoryId}/safety-stock
Authorization: Bearer {token}
Content-Type: application/json

Request:
{
    "safetyStock": 20
}

Response: 200 OK
{
    "inventoryId": 1,
    "sku": "TSHIRT-BLK-M",
    "safetyStock": 20
}
```

---

### US-604: 재입고 알림

**As a** 구매자
**I want to** 품절된 상품이 재입고되면 알림을 받고 싶다
**So that** 원하는 상품을 빠르게 구매할 수 있다

#### Acceptance Criteria
- [ ] 품절 상품에 대해 구매자가 재입고 알림을 신청할 수 있다
- [ ] 알림 신청 시 상품 ID, 옵션 ID, 회원 ID를 저장한다
- [ ] 재고 입고 이벤트 수신 시 해당 SKU의 알림 신청자에게 알림 발행
- [ ] Kafka 이벤트: `inventory.restock_notification` (memberId, productId, optionId)
- [ ] 알림 발송 후 해당 신청 건은 `NOTIFIED` 상태로 변경한다
- [ ] 회원당 재입고 알림 신청은 최대 50건으로 제한한다

#### 데이터 모델
```sql
CREATE TABLE restock_notification (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    member_id       BIGINT          NOT NULL COMMENT '회원 ID',
    product_id      BIGINT          NOT NULL COMMENT '상품 ID',
    option_id       BIGINT          NOT NULL COMMENT '옵션 ID',
    status          VARCHAR(20)     NOT NULL DEFAULT 'WAITING' COMMENT '상태: WAITING, NOTIFIED, CANCELLED',
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_product_option (member_id, product_id, option_id),
    INDEX idx_product_option_status (product_id, option_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='재입고 알림 신청';
```

#### API 스펙
```
POST /api/v1/restock-notifications
Authorization: Bearer {token}
Content-Type: application/json

Request:
{
    "productId": 100,
    "optionId": 1
}

Response: 201 Created
{
    "id": 1,
    "productId": 100,
    "optionId": 1,
    "status": "WAITING",
    "createdAt": "2026-03-22T10:00:00"
}

DELETE /api/v1/restock-notifications/{notificationId}
Authorization: Bearer {token}

Response: 204 No Content
```

---

## 4. 검색 도메인 (search-service)

### US-701: Elasticsearch 상품 인덱싱 (Kafka CDC)

**As a** 시스템
**I want to** 상품 데이터를 Elasticsearch에 실시간 인덱싱하고 싶다
**So that** 빠르고 정확한 상품 검색을 제공할 수 있다

#### Acceptance Criteria
- [ ] 상품 생성/수정/삭제 이벤트를 Kafka로 수신하여 Elasticsearch에 동기화한다
- [ ] Kafka 토픽: `product.created`, `product.updated`, `product.deleted`
- [ ] Elasticsearch 인덱스: `closet-products`
- [ ] 인덱싱 필드: 상품명, 브랜드, 카테고리, 가격, 색상, 사이즈, 설명, 태그
- [ ] 벌크 인덱싱 API를 제공하여 초기 데이터 마이그레이션을 지원한다
- [ ] 인덱싱 실패 시 Dead Letter Queue(DLQ)에 저장하고 재처리한다
- [ ] 인덱싱 지연은 3초 이내를 목표로 한다

#### 데이터 모델 (Elasticsearch 인덱스)
```json
{
    "mappings": {
        "properties": {
            "productId": { "type": "long" },
            "name": {
                "type": "text",
                "analyzer": "nori_analyzer",
                "fields": {
                    "keyword": { "type": "keyword" },
                    "autocomplete": {
                        "type": "text",
                        "analyzer": "autocomplete_analyzer"
                    }
                }
            },
            "brand": {
                "type": "text",
                "analyzer": "nori_analyzer",
                "fields": { "keyword": { "type": "keyword" } }
            },
            "category": { "type": "keyword" },
            "subCategory": { "type": "keyword" },
            "price": { "type": "integer" },
            "salePrice": { "type": "integer" },
            "colors": { "type": "keyword" },
            "sizes": { "type": "keyword" },
            "tags": { "type": "keyword" },
            "description": { "type": "text", "analyzer": "nori_analyzer" },
            "imageUrl": { "type": "keyword", "index": false },
            "salesCount": { "type": "integer" },
            "reviewCount": { "type": "integer" },
            "avgRating": { "type": "float" },
            "createdAt": { "type": "date" },
            "updatedAt": { "type": "date" }
        }
    },
    "settings": {
        "analysis": {
            "analyzer": {
                "nori_analyzer": {
                    "type": "custom",
                    "tokenizer": "nori_tokenizer",
                    "filter": ["nori_readingform", "lowercase"]
                },
                "autocomplete_analyzer": {
                    "type": "custom",
                    "tokenizer": "autocomplete_tokenizer",
                    "filter": ["lowercase"]
                }
            },
            "tokenizer": {
                "autocomplete_tokenizer": {
                    "type": "edge_ngram",
                    "min_gram": 1,
                    "max_gram": 20,
                    "token_chars": ["letter", "digit"]
                }
            }
        }
    }
}
```

#### API 스펙
```
POST /api/v1/search/reindex
Authorization: Bearer {token} (admin)

Response: 202 Accepted
{
    "message": "Reindexing started",
    "estimatedTime": "5 minutes"
}
```

---

### US-702: 한글 형태소 분석 (nori) + 키워드 검색

**As a** 구매자
**I want to** 한글로 상품을 검색하면 정확한 결과를 얻고 싶다
**So that** 원하는 상품을 빠르게 찾을 수 있다

#### Acceptance Criteria
- [ ] nori 한글 형태소 분석기를 사용하여 한글 검색을 지원한다
- [ ] 복합어 분리: "반팔티셔츠" → "반팔", "티셔츠" 모두 검색 가능
- [ ] 유의어 처리: "바지" = "팬츠", "상의" = "탑", "원피스" = "드레스"
- [ ] 상품명, 브랜드명, 카테고리, 태그, 설명을 대상으로 검색한다
- [ ] 검색 결과는 관련도순(기본), 최신순, 가격순, 인기순으로 정렬 가능하다
- [ ] 검색 결과에 하이라이팅을 적용한다
- [ ] 검색 결과가 없을 때 오타 교정 제안을 제공한다

#### API 스펙
```
GET /api/v1/search/products?q={keyword}&sort={sort}&page={page}&size={size}
Authorization: Bearer {token}

Parameters:
- q: 검색 키워드 (필수)
- sort: RELEVANCE (기본), LATEST, PRICE_ASC, PRICE_DESC, POPULAR
- page: 페이지 번호 (기본 0)
- size: 페이지 크기 (기본 20, 최대 100)

Response: 200 OK
{
    "totalCount": 150,
    "page": 0,
    "size": 20,
    "items": [
        {
            "productId": 100,
            "name": "<em>반팔</em> <em>티셔츠</em> 오버핏",
            "brand": "CLOSET STANDARD",
            "category": "상의",
            "price": 29000,
            "salePrice": 19900,
            "imageUrl": "https://cdn.closet.com/products/100/main.jpg",
            "avgRating": 4.5,
            "reviewCount": 120
        }
    ],
    "suggestion": null
}
```

---

### US-703: 필터 (카테고리/브랜드/가격/색상/사이즈)

**As a** 구매자
**I want to** 검색 결과를 다양한 조건으로 필터링하고 싶다
**So that** 원하는 조건에 맞는 상품만 빠르게 찾을 수 있다

#### Acceptance Criteria
- [ ] 카테고리 필터: 대/중/소 카테고리 계층 구조 지원
- [ ] 브랜드 필터: 복수 선택 가능
- [ ] 가격 필터: 최소/최대 가격 범위 지정
- [ ] 색상 필터: 복수 선택 가능
- [ ] 사이즈 필터: 복수 선택 가능
- [ ] 필터 적용 시 각 필터 옵션별 상품 개수(facet count)를 표시한다
- [ ] 복수 필터 조합 시 AND 조건으로 적용한다
- [ ] 필터 결과가 없으면 빈 배열을 반환한다

#### API 스펙
```
GET /api/v1/search/products?q={keyword}&category={category}&brand={brand}&minPrice={min}&maxPrice={max}&color={color}&size={size}
Authorization: Bearer {token}

Parameters (모두 선택):
- category: 카테고리 코드 (예: "TOP", "BOTTOM", "OUTER")
- brand: 브랜드명 (복수 선택: brand=Nike&brand=Adidas)
- minPrice: 최소 가격
- maxPrice: 최대 가격
- color: 색상 코드 (복수 선택: color=BLACK&color=WHITE)
- size: 사이즈 (복수 선택: size=M&size=L)

Response: 200 OK
{
    "totalCount": 30,
    "items": [...],
    "facets": {
        "categories": [
            { "code": "TOP", "name": "상의", "count": 20 },
            { "code": "BOTTOM", "name": "하의", "count": 10 }
        ],
        "brands": [
            { "name": "Nike", "count": 15 },
            { "name": "Adidas", "count": 10 }
        ],
        "priceRanges": [
            { "label": "~30,000원", "min": 0, "max": 30000, "count": 8 },
            { "label": "30,000~50,000원", "min": 30000, "max": 50000, "count": 12 },
            { "label": "50,000~100,000원", "min": 50000, "max": 100000, "count": 7 },
            { "label": "100,000원~", "min": 100000, "max": null, "count": 3 }
        ],
        "colors": [
            { "code": "BLACK", "name": "블랙", "count": 18 },
            { "code": "WHITE", "name": "화이트", "count": 12 }
        ],
        "sizes": [
            { "code": "S", "count": 20 },
            { "code": "M", "count": 25 },
            { "code": "L", "count": 22 }
        ]
    }
}
```

---

### US-704: 자동완성

**As a** 구매자
**I want to** 검색어를 입력하면 자동완성 추천을 보고 싶다
**So that** 빠르게 원하는 검색어를 완성하여 검색할 수 있다

#### Acceptance Criteria
- [ ] 2글자 이상 입력 시 자동완성 결과를 반환한다
- [ ] 최대 10개의 자동완성 추천을 제공한다
- [ ] 자동완성 소스: 상품명, 브랜드명, 카테고리명
- [ ] edge_ngram 토크나이저를 사용하여 prefix 매칭을 지원한다
- [ ] 응답 시간 50ms 이내를 목표로 한다
- [ ] 자동완성 결과에 매칭된 부분을 하이라이팅한다

#### API 스펙
```
GET /api/v1/search/autocomplete?q={prefix}
Authorization: Bearer {token}

Parameters:
- q: 입력 중인 검색어 (최소 2글자)

Response: 200 OK
{
    "suggestions": [
        { "text": "반팔 티셔츠", "type": "PRODUCT", "count": 150 },
        { "text": "반팔 셔츠", "type": "PRODUCT", "count": 80 },
        { "text": "반클리프", "type": "BRAND", "count": 30 }
    ]
}
```

---

### US-705: 인기 검색어

**As a** 구매자
**I want to** 현재 다른 사람들이 많이 검색하는 키워드를 보고 싶다
**So that** 트렌드를 파악하고 관심 상품을 발견할 수 있다

#### Acceptance Criteria
- [ ] 실시간 인기 검색어 Top 10을 제공한다
- [ ] 검색 시마다 Redis Sorted Set에 검색 횟수를 기록한다
- [ ] 1시간 단위로 갱신한다 (sliding window)
- [ ] 부적절한 검색어 필터링 (금칙어 목록 관리)
- [ ] 이전 시간 대비 순위 변동 표시 (NEW, UP, DOWN, SAME)
- [ ] 인기 검색어 조회 API 응답 시간 10ms 이내 (Redis 직접 조회)

#### API 스펙
```
GET /api/v1/search/popular-keywords

Response: 200 OK
{
    "updatedAt": "2026-03-22T10:00:00",
    "keywords": [
        { "rank": 1, "keyword": "반팔 티셔츠", "change": "UP", "searchCount": 5200 },
        { "rank": 2, "keyword": "린넨 셔츠", "change": "NEW", "searchCount": 4800 },
        { "rank": 3, "keyword": "와이드 팬츠", "change": "SAME", "searchCount": 4500 },
        { "rank": 4, "keyword": "크로스백", "change": "DOWN", "searchCount": 4200 },
        { "rank": 5, "keyword": "나이키 덩크", "change": "UP", "searchCount": 3900 },
        { "rank": 6, "keyword": "슬랙스", "change": "SAME", "searchCount": 3700 },
        { "rank": 7, "keyword": "맨투맨", "change": "DOWN", "searchCount": 3500 },
        { "rank": 8, "keyword": "후드집업", "change": "NEW", "searchCount": 3200 },
        { "rank": 9, "keyword": "스니커즈", "change": "SAME", "searchCount": 3000 },
        { "rank": 10, "keyword": "카고팬츠", "change": "UP", "searchCount": 2800 }
    ]
}
```

---

## 5. 리뷰 도메인 (review-service)

### US-801: 텍스트 + 포토 리뷰

**As a** 구매자
**I want to** 구매한 상품에 대해 텍스트와 사진으로 리뷰를 작성하고 싶다
**So that** 다른 구매자에게 도움을 주고, 리뷰 포인트를 적립받을 수 있다

#### Acceptance Criteria
- [ ] 구매확정(`CONFIRMED`) 상태의 주문 건에 대해서만 리뷰 작성이 가능하다
- [ ] 하나의 주문 상품에 대해 리뷰는 1회만 작성 가능하다
- [ ] 텍스트 리뷰: 별점(1-5), 내용(최소 20자, 최대 1000자)
- [ ] 포토 리뷰: 최대 5장의 이미지 첨부 가능 (JPEG/PNG, 최대 5MB/장)
- [ ] 이미지는 S3에 업로드하고 리사이즈(400x400 썸네일) 처리한다
- [ ] 리뷰 작성 시 `review.created` Kafka 이벤트를 발행한다
- [ ] 리뷰 수정은 작성 후 7일 이내에만 가능하다
- [ ] 리뷰 삭제는 본인만 가능하다

#### 데이터 모델
```sql
CREATE TABLE review (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    member_id       BIGINT          NOT NULL COMMENT '작성자 ID',
    product_id      BIGINT          NOT NULL COMMENT '상품 ID',
    order_item_id   BIGINT          NOT NULL COMMENT '주문 상품 ID',
    rating          TINYINT(1)      NOT NULL COMMENT '별점 (1-5)',
    content         VARCHAR(1000)   NOT NULL COMMENT '리뷰 내용',
    has_photo       TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '포토 리뷰 여부',
    helpful_count   INT             NOT NULL DEFAULT 0 COMMENT '도움이 됐어요 수',
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '상태: ACTIVE, HIDDEN, DELETED',
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_item (order_item_id),
    INDEX idx_product_id (product_id),
    INDEX idx_member_id (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='리뷰';

CREATE TABLE review_image (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    review_id       BIGINT          NOT NULL COMMENT '리뷰 ID',
    image_url       VARCHAR(500)    NOT NULL COMMENT '원본 이미지 URL',
    thumbnail_url   VARCHAR(500)    NOT NULL COMMENT '썸네일 이미지 URL',
    display_order   INT             NOT NULL DEFAULT 0 COMMENT '표시 순서',
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_review_id (review_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='리뷰 이미지';
```

#### API 스펙
```
POST /api/v1/reviews
Authorization: Bearer {token}
Content-Type: multipart/form-data

Request:
- rating: 5 (필수)
- content: "정말 편하고 핏이 좋아요. 사이즈도 딱 맞습니다." (필수)
- orderItemId: 67890 (필수)
- productId: 100 (필수)
- images: [file1.jpg, file2.jpg] (선택, 최대 5장)

Response: 201 Created
{
    "id": 1,
    "memberId": 999,
    "productId": 100,
    "orderItemId": 67890,
    "rating": 5,
    "content": "정말 편하고 핏이 좋아요. 사이즈도 딱 맞습니다.",
    "hasPhoto": true,
    "images": [
        {
            "imageUrl": "https://cdn.closet.com/reviews/1/1.jpg",
            "thumbnailUrl": "https://cdn.closet.com/reviews/1/1_thumb.jpg"
        }
    ],
    "createdAt": "2026-03-30T10:00:00"
}

GET /api/v1/reviews?productId={productId}&sort={sort}&page={page}&size={size}
Authorization: Bearer {token}

Parameters:
- productId: 상품 ID (필수)
- sort: LATEST (기본), RATING_HIGH, RATING_LOW, HELPFUL
- photoOnly: true/false (포토 리뷰만)
- page: 페이지 번호 (기본 0)
- size: 페이지 크기 (기본 10)

Response: 200 OK
{
    "totalCount": 120,
    "avgRating": 4.3,
    "ratingDistribution": {
        "5": 60, "4": 30, "3": 15, "2": 10, "1": 5
    },
    "items": [...]
}
```

---

### US-802: 사이즈 후기 (키/몸무게/핏 평가)

**As a** 구매자
**I want to** 다른 구매자의 체형 정보와 사이즈 핏 평가를 보고 싶다
**So that** 내 체형에 맞는 사이즈를 선택할 수 있다

#### Acceptance Criteria
- [ ] 리뷰 작성 시 선택적으로 사이즈 정보를 입력할 수 있다
- [ ] 입력 항목: 키(cm), 몸무게(kg), 평소 사이즈, 구매 사이즈
- [ ] 핏 평가: `SMALL`(작아요), `PERFECT`(딱 맞아요), `LARGE`(커요)
- [ ] 상품 상세에서 사이즈 핏 분포를 시각화한다 (작아요/딱맞아요/커요 비율)
- [ ] "나와 비슷한 체형" 필터: 키/몸무게 +-5 범위의 리뷰 필터링
- [ ] 사이즈 정보 입력 시 추가 포인트 50P 적립

#### 데이터 모델
```sql
CREATE TABLE review_size_info (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    review_id       BIGINT          NOT NULL COMMENT '리뷰 ID',
    height          INT             NULL COMMENT '키 (cm)',
    weight          INT             NULL COMMENT '몸무게 (kg)',
    usual_size      VARCHAR(10)     NULL COMMENT '평소 사이즈',
    purchased_size  VARCHAR(10)     NOT NULL COMMENT '구매한 사이즈',
    size_fit        VARCHAR(10)     NOT NULL COMMENT '핏 평가: SMALL, PERFECT, LARGE',
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_review_id (review_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='리뷰 사이즈 정보';
```

#### API 스펙
```
GET /api/v1/reviews/size-summary?productId={productId}
Authorization: Bearer {token}

Response: 200 OK
{
    "productId": 100,
    "totalSizeReviews": 80,
    "fitDistribution": {
        "SMALL": 15,
        "PERFECT": 55,
        "LARGE": 10
    },
    "recommendation": "이 상품은 정사이즈 추천입니다.",
    "sizeReviews": [
        {
            "height": 175,
            "weight": 70,
            "usualSize": "L",
            "purchasedSize": "L",
            "sizeFit": "PERFECT",
            "content": "딱 맞게 잘 입고 있어요"
        }
    ]
}

GET /api/v1/reviews?productId={productId}&minHeight={min}&maxHeight={max}&minWeight={min}&maxWeight={max}
-- 나와 비슷한 체형 필터
```

---

### US-803: 리뷰 포인트 적립 (100P/300P)

**As a** 구매자
**I want to** 리뷰를 작성하면 포인트를 적립받고 싶다
**So that** 적극적으로 리뷰를 작성할 동기가 생긴다

#### Acceptance Criteria
- [ ] 텍스트 리뷰 작성 시 100P 적립
- [ ] 포토 리뷰 작성 시 300P 적립 (텍스트 100P 포함)
- [ ] 사이즈 정보 입력 시 추가 50P 적립
- [ ] 최대 적립 조합: 포토 리뷰 + 사이즈 정보 = 350P
- [ ] 리뷰 삭제 시 적립된 포인트를 회수한다
- [ ] 포인트 적립은 `review.created` 이벤트를 통해 비동기로 처리한다 (member-service에서 처리)
- [ ] 적립 이벤트: `point.earn` (memberId, amount, reason, referenceType, referenceId)
- [ ] 하루 최대 리뷰 포인트 적립 한도: 5,000P

#### API 스펙
- 별도 API 없음 (리뷰 작성 API 내부에서 자동 발행)
- 포인트 적립/조회는 member-service에서 처리

---

### US-804: 리뷰 집계 (평균 별점, 사이즈 분포)

**As a** 시스템
**I want to** 상품별 리뷰 통계를 실시간으로 집계하고 싶다
**So that** 상품 목록/상세에서 리뷰 요약 정보를 빠르게 제공할 수 있다

#### Acceptance Criteria
- [ ] 상품별 리뷰 집계 데이터를 관리한다: 리뷰 수, 평균 별점, 별점 분포
- [ ] 리뷰 생성/수정/삭제 시 집계 데이터를 갱신한다
- [ ] 집계 데이터는 Redis에 캐싱하고, 리뷰 변경 시 캐시를 갱신한다
- [ ] 사이즈 핏 분포도 집계한다: SMALL/PERFECT/LARGE 비율
- [ ] Elasticsearch 상품 인덱스에 리뷰 집계 데이터를 동기화한다 (검색 정렬용)
- [ ] 벌크 집계 배치를 제공하여 데이터 불일치 시 보정할 수 있다

#### 데이터 모델
```sql
CREATE TABLE review_summary (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    product_id      BIGINT          NOT NULL COMMENT '상품 ID',
    review_count    INT             NOT NULL DEFAULT 0 COMMENT '리뷰 수',
    avg_rating      DECIMAL(2,1)    NOT NULL DEFAULT 0.0 COMMENT '평균 별점',
    rating_1_count  INT             NOT NULL DEFAULT 0 COMMENT '1점 개수',
    rating_2_count  INT             NOT NULL DEFAULT 0 COMMENT '2점 개수',
    rating_3_count  INT             NOT NULL DEFAULT 0 COMMENT '3점 개수',
    rating_4_count  INT             NOT NULL DEFAULT 0 COMMENT '4점 개수',
    rating_5_count  INT             NOT NULL DEFAULT 0 COMMENT '5점 개수',
    photo_review_count INT          NOT NULL DEFAULT 0 COMMENT '포토 리뷰 수',
    size_small_count    INT         NOT NULL DEFAULT 0 COMMENT '작아요 수',
    size_perfect_count  INT         NOT NULL DEFAULT 0 COMMENT '딱맞아요 수',
    size_large_count    INT         NOT NULL DEFAULT 0 COMMENT '커요 수',
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='리뷰 집계';
```

#### API 스펙
```
GET /api/v1/reviews/summary?productId={productId}
Authorization: Bearer {token}

Response: 200 OK
{
    "productId": 100,
    "reviewCount": 120,
    "avgRating": 4.3,
    "ratingDistribution": {
        "1": 5, "2": 10, "3": 15, "4": 30, "5": 60
    },
    "photoReviewCount": 45,
    "sizeFitDistribution": {
        "SMALL": 15,
        "PERFECT": 55,
        "LARGE": 10
    },
    "sizeFitRecommendation": "PERFECT"
}

POST /api/v1/reviews/summary/recalculate
Authorization: Bearer {token} (admin)

Response: 202 Accepted
{
    "message": "Review summary recalculation started"
}
```

---

## 6. 서비스 간 이벤트 흐름

```
order-service                shipping-service              inventory-service
     │                            │                              │
     │  order.created ───────────>│                              │
     │  order.created ──────────────────────────────────────────>│ (재고 차감)
     │                            │                              │
     │                   ┌────────┤                              │
     │                   │ 송장 등록                              │
     │<── order.status   │        │                              │
     │    .changed ──────┘        │                              │
     │                            │                              │
     │                   ┌────────┤                              │
     │                   │배송완료 │                              │
     │<── order.status   │        │                              │
     │    .changed ──────┘        │                              │
     │                            │                              │
     │  order.confirmed ─────────>│                              │
     │                            │                              │
     │  order.cancelled ────────────────────────────────────────>│ (재고 복구)
     │                            │                              │
     │                            │  return.approved ───────────>│ (재고 복구)

product-service              search-service                review-service
     │                            │                              │
     │  product.created ─────────>│ (인덱싱)                      │
     │  product.updated ─────────>│ (업데이트)                    │
     │                            │                              │
     │                            │<── review.summary.updated ───│
     │                            │    (별점/리뷰수 동기화)        │
     │                            │                              │
     │                            │                  review.created ──> member-service
     │                            │                  (포인트 적립)       (point.earn)
```

---

## 7. 기술 의사결정

| 결정 | 선택 | 이유 |
|------|------|------|
| 검색 엔진 | Elasticsearch 8.x | nori 한글 형태소 분석, facet 검색, 자동완성 지원 |
| 데이터 동기화 | Kafka CDC | 실시간 인덱싱, 서비스 간 느슨한 결합 |
| 분산 락 | Redisson | Redis 기반, Spring Boot 통합 용이, Pub/Sub 기반 효율적 락 |
| 이미지 저장 | AWS S3 | 확장성, CDN 연동, 비용 효율 |
| 이미지 리사이즈 | AWS Lambda (또는 서비스 내) | 비동기 처리, 서버 부하 분리 |
| 배송 추적 | 스마트택배 API | 국내 택배사 통합 지원, 합리적 비용 |
| 캐싱 | Redis 7.x | 검색 캐시, 인기 검색어, 리뷰 집계 캐시 |

---

## 8. 마일스톤

| Sprint | 기간 | 목표 |
|--------|------|------|
| Sprint 5 | Week 1-2 | 재고 서비스 (US-601, US-602) + 배송 송장 등록 (US-501) |
| Sprint 6 | Week 3-4 | 배송 추적/반품/교환 (US-502~505) + 재고 알림 (US-603, US-604) |
| Sprint 7 | Week 5-6 | 검색 서비스 (US-701~705) |
| Sprint 8 | Week 7-8 | 리뷰 서비스 (US-801~804) + 통합 테스트 |
