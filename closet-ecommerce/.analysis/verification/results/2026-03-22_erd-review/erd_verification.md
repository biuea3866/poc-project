# ERD 검증 리포트

> 검증일: 2026-03-22
> 검증 대상: `erd_detailed.md` (15개 서비스, 64개 테이블)
> 교차 참조: `full_domain_analysis.md` (15개 도메인 분석)
> 검증자 역할: DBA

---

## 총괄 요약

| 구분 | 건수 |
|------|------|
| 통과 (PASS) | 52건 |
| 위반 (VIOLATION) | 18건 |
| 제안 (SUGGESTION) | 14건 |

### 판정 기준별 요약

| 검증 항목 | 통과 | 위반 | 제안 |
|----------|------|------|------|
| 1. 테이블 누락 검증 | 13/15 서비스 | 2건 | 3건 |
| 2. 컬럼 누락 검증 | 대부분 충족 | 7건 | 2건 |
| 3. DB 규칙 준수 | 대부분 준수 | 4건 | 0건 |
| 4. 인덱스 검증 | 대부분 양호 | 2건 | 3건 |
| 5. 데이터 타입 적절성 | 대부분 양호 | 1건 | 2건 |
| 6. 서비스 간 데이터 참조 | 양호 | 0건 | 1건 |
| 7. 누락 테이블 제안 | - | - | 5건 |
| 8. 성능 관점 | - | 2건 | 3건 |

---

## 1. 테이블 누락 검증

### PASS (존재하는 테이블이 도메인 분석과 일치)

모든 15개 서비스가 ERD에 존재하며, PRD에서 정의한 핵심 엔티티가 대부분 테이블로 반영되어 있다.

### VIOLATION

#### [V-TBL-01] Order 서비스: `order_status_history` 테이블 누락
- **심각도**: MEDIUM
- **근거**: PRD 2.2절에서 "주문 이력 관리"를 범위에 포함하고 있으나, 주문 상태 변경 이력을 추적하는 테이블이 없다. `orders` 테이블의 `status`만으로는 상태 전이 이력을 추적할 수 없다.
- **배송(shipment_status_history)**, **결제(payment_history)**, **재고(stock_history)** 서비스에는 이력 테이블이 존재하므로 일관성이 깨진다.
- **제안 DDL**:
```sql
CREATE TABLE order_status_history (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '주문 상태 이력 고유 식별자',
    order_id          BIGINT          NOT NULL COMMENT '주문 ID',
    order_item_id     BIGINT          NULL     COMMENT '주문 항목 ID (항목 단위 상태 변경 시)',
    from_status       VARCHAR(30)     NOT NULL COMMENT '변경 전 상태',
    to_status         VARCHAR(30)     NOT NULL COMMENT '변경 후 상태',
    reason            VARCHAR(500)    NULL     COMMENT '변경 사유',
    changed_by        VARCHAR(100)    NULL     COMMENT '변경 주체 (SYSTEM, MEMBER, SELLER, ADMIN)',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '변경일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='주문 상태 변경 이력';

CREATE INDEX idx_order_status_history_order ON order_status_history (order_id, created_at DESC);
CREATE INDEX idx_order_status_history_item ON order_status_history (order_item_id, created_at DESC);
```

#### [V-TBL-02] Shipping 서비스: `exchange_request` 테이블 누락
- **심각도**: MEDIUM
- **근거**: PRD 2.2절 주문 도메인에서 "교환 요청(EXCHANGE_REQUESTED)"이 주문 상태 전이에 포함되어 있고, PRD 2.13절 CS 도메인에서도 "반품/교환 접수"를 범위로 정의하고 있다. 그러나 ERD에는 `return_request`만 있고 `exchange_request`가 없다.
- 반품과 교환은 물류 프로세스가 다르다 (반품: 수거 -> 환불, 교환: 수거 -> 재발송). 하나의 테이블로 처리하려면 최소한 `type` 컬럼(RETURN/EXCHANGE)이 `return_request`에 필요하다.
- **제안 DDL**:
```sql
-- 방법 A: return_request에 type 컬럼 추가 (최소 변경)
ALTER TABLE return_request ADD COLUMN request_type VARCHAR(30) NOT NULL DEFAULT 'RETURN'
    COMMENT '요청 유형 (RETURN, EXCHANGE)' AFTER shipment_id;
ALTER TABLE return_request ADD COLUMN exchange_sku_id BIGINT NULL
    COMMENT '교환 대상 SKU ID (교환 시)' AFTER return_fee;

-- 방법 B: 별도 테이블 (권장)
CREATE TABLE exchange_request (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '교환 요청 고유 식별자',
    order_id          BIGINT          NOT NULL COMMENT '주문 ID',
    order_item_id     BIGINT          NOT NULL COMMENT '원본 주문 항목 ID',
    shipment_id       BIGINT          NOT NULL COMMENT '원본 배송 ID',
    seller_id         BIGINT          NOT NULL COMMENT '셀러 ID',
    from_sku_id       BIGINT          NOT NULL COMMENT '기존 SKU ID',
    to_sku_id         BIGINT          NOT NULL COMMENT '교환 대상 SKU ID',
    reason_type       VARCHAR(30)     NOT NULL COMMENT '교환 사유 (WRONG_SIZE, DEFECTIVE, WRONG_COLOR)',
    description       TEXT            NULL     COMMENT '상세 사유',
    status            VARCHAR(30)     NOT NULL DEFAULT 'REQUESTED' COMMENT '교환 상태',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='교환 요청';

CREATE INDEX idx_exchange_request_order ON exchange_request (order_id, order_item_id);
CREATE INDEX idx_exchange_request_seller ON exchange_request (seller_id, status);
```

---

## 2. 컬럼 누락 검증

#### [V-COL-01] `orders` 테이블: `created_at` 컬럼 누락
- **심각도**: LOW
- **근거**: 모든 테이블에 `created_at`이 존재해야 하는 컨벤션이나, `orders` 테이블에는 `ordered_at`만 있고 `created_at`이 없다. `ordered_at`과 `created_at`은 의미가 다를 수 있다 (예: 주문서 생성 시점 vs 주문 확정 시점).
- **제안**: `ordered_at`으로 충분하다면 현행 유지 가능. 다만 ORM 매핑의 일관성을 위해 `created_at` 추가를 권장.

#### [V-COL-02] `order_item` 테이블: `deleted_at` 누락
- **심각도**: LOW
- **근거**: `orders` 테이블은 `deleted_at`이 있으나 `order_item`에는 없다. 부분 취소 시 soft delete가 필요할 수 있다.
- **제안**: order_item은 상태(status)로 관리하므로 deleted_at 불필요할 수 있으나, 일관성 차원에서 추가 검토 필요.

#### [V-COL-03] `product` 테이블: `gender` 컬럼 누락
- **심각도**: MEDIUM
- **근거**: PRD 1.3절에서 성별/연령대별 카테고리를 언급하고, Display 서비스의 `ranking_snapshot` 테이블에 `gender` 컬럼이 존재한다. 그러나 상품 자체에 성별 정보가 없으면 성별 기반 랭킹/필터를 구현할 수 없다.
- **제안 DDL**:
```sql
ALTER TABLE product ADD COLUMN gender VARCHAR(10) NULL
    COMMENT '대상 성별 (MALE, FEMALE, UNISEX)' AFTER fit;
CREATE INDEX idx_product_gender ON product (gender, status, deleted_at);
```

#### [V-COL-04] `product` 테이블: `material` (소재) 컬럼 누락
- **심각도**: LOW
- **근거**: CLAUDE.md에서 "색상/소재 관리 (면/폴리에스터/나일론 등)"를 주요 도메인으로 명시하고 있으나, 상품 테이블에 소재 관련 컬럼이 없다.
- **제안**: `product_material` 별도 테이블 또는 product에 `material` VARCHAR(200) 컬럼 추가.

```sql
CREATE TABLE product_material (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '상품 소재 고유 식별자',
    product_id        BIGINT          NOT NULL COMMENT '상품 ID',
    material_name     VARCHAR(50)     NOT NULL COMMENT '소재명 (면, 폴리에스터, 나일론 등)',
    percentage        INT             NULL     COMMENT '혼용률 (%)',
    display_order     INT             NOT NULL DEFAULT 0 COMMENT '노출 순서',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='상품 소재 정보';

CREATE INDEX idx_product_material_product ON product_material (product_id);
```

#### [V-COL-05] `cart` 테이블: `created_at` 누락
- **심각도**: LOW
- **근거**: `created_at`은 모든 테이블의 공통 컨벤션이나, `cart` 테이블에 누락되어 있다.

#### [V-COL-06] `ootd_snap` 테이블: `tagged_products` TEXT 타입 사용
- **심각도**: MEDIUM
- **근거**: `tagged_products`가 "상품 ID 목록 (콤마 구분)" 형태로 설계되어 있으나, 이는 사실상 JSON/배열 데이터를 TEXT로 저장하는 우회 패턴이다. 정규화하여 별도 조인 테이블로 분리하는 것이 바람직하다.
- **제안 DDL**:
```sql
CREATE TABLE ootd_product (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'OOTD 상품 태깅 고유 식별자',
    ootd_id           BIGINT          NOT NULL COMMENT 'OOTD 스냅 ID',
    product_id        BIGINT          NOT NULL COMMENT '상품 ID',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ootd_product (ootd_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='OOTD 스냅 내 상품 태깅';

CREATE INDEX idx_ootd_product_ootd ON ootd_product (ootd_id);
CREATE INDEX idx_ootd_product_product ON ootd_product (product_id);
```

#### [V-COL-07] `settlement_item` 테이블: 반품 상계 시 환불 관련 컬럼 부족
- **심각도**: LOW
- **근거**: PRD 2.11절에서 "구매확정 후 환불 시 다음 정산에서 차감(상계)"을 언급하나, `settlement_item`의 상태값에 `OFFSET`만 있고 원본 정산 항목과의 연결 컬럼이 없다.
- **제안**: `offset_target_id` BIGINT NULL COMMENT '상계 대상 원본 정산 항목 ID' 추가.

---

## 3. DB 규칙 준수 검증

### 규칙 요약
- FK 제약조건 없음
- JSON 타입 없음
- ENUM 타입 없음
- BOOLEAN -> TINYINT(1)
- DATETIME(6)
- 모든 컬럼/테이블 COMMENT 필수
- Soft Delete (deleted_at)

### 검증 결과

#### [V-RULE-01] PASS - FK 제약조건
- 전체 64개 테이블에서 FK 제약조건 미사용 확인. 모든 참조는 컬럼 COMMENT로 명시되어 있다.

#### [V-RULE-02] PASS - JSON 타입
- JSON 타입 미사용. 리스트성 데이터는 TEXT + 콤마 구분 패턴으로 처리.

#### [V-RULE-03] PASS - ENUM 타입
- ENUM 타입 미사용. 모든 상태/유형은 VARCHAR(30)로 통일.

#### [V-RULE-04] PASS - BOOLEAN -> TINYINT(1)
- `active`, `is_default`, `is_pb`, `marketing_opt_in`, `published`, `account_verified` 등 모든 boolean 성격의 컬럼이 TINYINT(1) 사용.

#### [V-RULE-05] PASS - DATETIME(6)
- 모든 날짜/시간 컬럼이 DATETIME(6) (마이크로초 정밀도) 사용.
- 예외: `settlement_statement`의 `period_start`, `period_end`는 DATE 타입 (이것은 일자만 필요하므로 적절함).

#### [V-RULE-06] PASS - COMMENT 필수
- 전체 테이블에 TABLE COMMENT 존재.
- 전체 컬럼에 COLUMN COMMENT 존재.

#### [V-RULE-07] VIOLATION - Soft Delete 누락 테이블
- **심각도**: LOW~MEDIUM
- 다음 테이블에 `deleted_at`이 누락되어 있다:

| 테이블 | deleted_at 존재 | 비고 |
|--------|---------------|------|
| order_item | X | 상태로 관리 가능하나 일관성 부족 |
| cart_item | X | 장바구니 항목은 물리 삭제가 합당할 수 있음 |
| size_guide | X | 상품 삭제 시 함께 삭제 필요 |
| delivery_fee_policy | X | 이력 보존 필요 여부 검토 |
| payment | X | 금융 데이터, soft delete 필수 |
| payment_history | X | 이력 데이터, 삭제 불필요 |
| stock_history | X | 이력 데이터, 삭제 불필요 |
| restock_request | X | 상태로 관리 가능 |
| social_account | X | 연동 해제 시 soft delete 필요 |
| point_history | X | 이력 데이터, 삭제 불필요 |
| settlement_item | X | 금융 데이터, soft delete 필수 |
| settlement_statement | X | 금융 데이터, soft delete 필수 |
| notification | X | 아카이브 정책으로 관리 |
| inquiry | X | CS 데이터 보존 필요 |

- **판정**: 이력성 테이블(payment_history, stock_history, point_history 등)은 삭제 자체가 불필요하므로 PASS.
  금융 데이터(payment, settlement_item, settlement_statement)는 deleted_at 추가 필수.
  social_account은 연동 해제 시 soft delete 필요.

#### [V-RULE-08] VIOLATION - TEXT 컬럼의 콤마 구분 리스트 패턴
- **심각도**: MEDIUM
- 다음 컬럼들이 콤마 구분 리스트를 TEXT에 저장하고 있어, 사실상 1NF 위반이다:

| 테이블 | 컬럼 | 내용 |
|--------|------|------|
| coupon_policy | applicable_categories | 카테고리 ID 목록 |
| coupon_policy | applicable_brands | 브랜드 ID 목록 |
| coupon_policy | excluded_products | 제외 상품 ID 목록 |
| return_request | evidence_urls | 증빙 이미지 URL 목록 |
| notification | variables | 변수 목록 |
| notification_template | required_variables | 필수 변수 목록 |
| inquiry | attachment_urls | 첨부 이미지 URL 목록 |
| inquiry_reply | attachment_urls | 첨부 이미지 URL 목록 |
| seller_application | document_urls | 서류 URL 목록 |
| ootd_snap | tagged_products | 상품 ID 목록 |
| ootd_snap | tags | 태그 목록 |
| coordination | style_tags | 스타일 태그 목록 |
| search_log | filters | 필터 목록 |
| synonym_dictionary | synonyms | 동의어 목록 |

- **제안**: ID 참조가 포함된 컬럼(applicable_categories, applicable_brands, excluded_products, tagged_products)은 정규화하여 별도 조인 테이블로 분리해야 한다. URL 목록(evidence_urls, attachment_urls 등)은 별도 테이블 분리 또는 현행 유지 가능 (인덱싱 불필요 시).

```sql
-- 쿠폰 정책-카테고리 매핑 테이블 예시
CREATE TABLE coupon_policy_category (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '쿠폰 정책-카테고리 매핑 고유 식별자',
    policy_id         BIGINT          NOT NULL COMMENT '쿠폰 정책 ID',
    category_id       BIGINT          NOT NULL COMMENT '카테고리 ID',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_policy_category (policy_id, category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='쿠폰 적용 카테고리 매핑';

-- 쿠폰 정책-브랜드 매핑 테이블
CREATE TABLE coupon_policy_brand (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '쿠폰 정책-브랜드 매핑 고유 식별자',
    policy_id         BIGINT          NOT NULL COMMENT '쿠폰 정책 ID',
    brand_id          BIGINT          NOT NULL COMMENT '브랜드 ID',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_policy_brand (policy_id, brand_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='쿠폰 적용 브랜드 매핑';

-- 쿠폰 정책-제외상품 매핑 테이블
CREATE TABLE coupon_policy_excluded_product (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '쿠폰 제외 상품 매핑 고유 식별자',
    policy_id         BIGINT          NOT NULL COMMENT '쿠폰 정책 ID',
    product_id        BIGINT          NOT NULL COMMENT '상품 ID',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_policy_excluded_product (policy_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='쿠폰 제외 상품 매핑';
```

#### [V-RULE-09] VIOLATION - `outbox.payload` TEXT 타입으로 JSON 저장
- **심각도**: LOW
- **근거**: outbox 테이블의 payload 컬럼이 "이벤트 페이로드 (직렬화된 JSON 문자열)"로 기술되어 있다. JSON 타입은 사용하지 않았으나 TEXT에 JSON을 저장하는 것은 JSON 사용 금지 규칙의 의도(구조화된 데이터를 비정형으로 저장하는 것을 방지)와 충돌할 수 있다.
- **판정**: Outbox 패턴의 payload는 업계 표준이며, 이벤트 페이로드의 구조가 이벤트 타입마다 다르므로 TEXT 저장이 불가피하다. **예외 허용**.

#### [V-RULE-10] VIOLATION - `search_log.filters` 구조화 미흡
- **심각도**: LOW
- **근거**: "콤마 구분 key=value" 형태는 쿼리/분석이 어렵다. 검색 로그 분석이 목적이라면 별도 테이블 분리 또는 필터 항목별 컬럼화가 필요하다.
- **판정**: search_log은 높은 볼륨(월 800만)으로 빠른 적재가 우선이므로 현행 유지 가능. 분석은 별도 데이터 파이프라인(BigQuery 등)에서 수행하는 것을 권장.

---

## 4. 인덱스 검증

### 전반적으로 양호
- 핵심 조회 패턴에 대한 인덱스가 잘 설계되어 있다.
- 복합 인덱스의 컬럼 순서가 쿼리 패턴과 일치한다.

### VIOLATION

#### [V-IDX-01] `coupon` 테이블: 만료 쿠폰 정리 배치용 인덱스 부재
- **심각도**: MEDIUM
- **근거**: 쿠폰 만료 처리 배치가 필요하나 (status=ISSUED, expires_at < NOW()), 현재 `idx_coupon_status (status, expires_at)` 인덱스로 커버 가능하지만, 대용량(1천만건)에서 만료 쿠폰 일괄 업데이트 시 성능이 우려된다.
- **제안**: 현재 인덱스로 충분하나, 파티셔닝을 고려할 수 있다.

#### [V-IDX-02] `cart_item` 테이블: `sku_id` 인덱스 부재
- **심각도**: MEDIUM
- **근거**: 장바구니에 동일 SKU가 이미 있는지 확인하는 쿼리(`cart_id + sku_id` 조합)가 빈번하나, 유니크 제약 또는 복합 인덱스가 없다.
- **제안**:
```sql
CREATE UNIQUE INDEX uk_cart_item_cart_sku ON cart_item (cart_id, sku_id);
```

### SUGGESTION

#### [S-IDX-01] `notification` 테이블: 아카이브 배치용 인덱스
- 90일 보관 후 아카이브 정책이 명시되어 있으나, `created_at` 기반 삭제/이동용 인덱스가 별도로 없다.
```sql
CREATE INDEX idx_notification_archive ON notification (created_at);
```

#### [S-IDX-02] `search_log` 테이블: 30일 아카이브용 인덱스
- 동일 패턴. `searched_at` 인덱스는 존재하나 파티셔닝을 권장.

#### [S-IDX-03] `ranking_snapshot` 테이블: `calculated_at` 기반 정리 인덱스
- 월 400만건 증가. 오래된 스냅샷 정리를 위한 인덱스 또는 파티셔닝 필요.

---

## 5. 데이터 타입 적절성

### 대부분 양호

| 검증 항목 | 결과 |
|----------|------|
| 가격 필드 BIGINT | PASS - 원 단위 정수 저장, 적절 |
| DECIMAL(5,2) 수수료율 | PASS - 최대 999.99%, 실제 사용 범위(0~100%) 충분 |
| DECIMAL(6,1) 사이즈 | PASS - 최대 99999.9cm, 충분 |
| VARCHAR 길이 | 대부분 PASS |
| MEDIUMTEXT (magazine.body) | PASS - HTML 본문, TEXT(64KB)보다 충분(16MB) |

### VIOLATION

#### [V-TYPE-01] `coupon_policy.discount_rate` INT 타입
- **심각도**: LOW
- **근거**: 할인율을 정수(%)로 관리하므로 소수점 할인율(예: 12.5%)을 지원하지 못한다.
- **제안**: 현재 비즈니스 요건에서 정수 할인율만 필요하다면 유지. 향후 확장을 위해 DECIMAL(5,2) 검토.

### SUGGESTION

#### [S-TYPE-01] `product.selling_price` / `product.list_price` BIGINT 타입
- 현재 원 단위 정수로 관리하여 적절하나, 향후 외화 결제를 지원하게 되면 DECIMAL(15,2) + 통화 코드 컬럼이 필요할 수 있다. 현 시점에서는 KRW 전용이므로 BIGINT 유지.

#### [S-TYPE-02] `member.phone` VARCHAR(20) nullable
- 의류 이커머스에서 전화번호는 배송/본인인증에 필수이므로 NOT NULL 검토 필요. 소셜 가입 시 전화번호가 없을 수 있으나, 최초 주문 시 필수 입력으로 유도하는 것이 일반적.

---

## 6. 서비스 간 데이터 참조 검증

### PASS
- 모든 크로스 서비스 참조가 컬럼 COMMENT에 명시되어 있다 (예: `seller_id COMMENT '셀러 ID (seller 테이블 참조)'`).
- FK 제약조건 없이 애플리케이션 레벨에서 참조 무결성을 관리하는 방식이 일관되게 적용되어 있다.

### SUGGESTION

#### [S-REF-01] 서비스별 참조 ID 타입 일관성 확인
- `inquiry_reply.author_id`가 VARCHAR(100)으로 되어 있는데, 회원 ID(BIGINT)와 셀러 ID(BIGINT), 관리자 ID를 모두 저장해야 하므로 타입이 다를 수 있다. `author_type + author_id(VARCHAR)` 패턴은 적절하나, 문서에 이 패턴의 이유를 명시하면 좋겠다.

---

## 7. 누락 테이블 제안

#### [S-TBL-01] `product_tag` 테이블
- **근거**: PRD Search 도메인(2.7절)에서 검색 인덱스 필드에 "태그"가 포함되어 있으나, 상품에 태그를 관리하는 테이블이 없다. 태그 기반 검색/필터를 위해 필요.
```sql
CREATE TABLE product_tag (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '상품 태그 고유 식별자',
    product_id        BIGINT          NOT NULL COMMENT '상품 ID',
    tag_name          VARCHAR(50)     NOT NULL COMMENT '태그명',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_tag (product_id, tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='상품 태그';

CREATE INDEX idx_product_tag_name ON product_tag (tag_name);
```

#### [S-TBL-02] `brand_follow` 테이블
- **근거**: PRD Display 도메인(2.10절)에서 "브랜드 팔로우 기능"을 명시하고 있으나, 팔로우 관계를 저장하는 테이블이 없다.
```sql
CREATE TABLE brand_follow (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '브랜드 팔로우 고유 식별자',
    member_id         BIGINT          NOT NULL COMMENT '회원 ID',
    brand_id          BIGINT          NOT NULL COMMENT '브랜드 ID',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '팔로우 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_brand_follow (member_id, brand_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='브랜드 팔로우';

CREATE INDEX idx_brand_follow_brand ON brand_follow (brand_id);
```

#### [S-TBL-03] `coupon_usage_history` 테이블
- **근거**: 쿠폰 사용/반환 이력을 추적할 테이블이 없다. `coupon` 테이블의 상태만으로는 사용 -> 반환 -> 재사용의 이력 추적이 불가.
```sql
CREATE TABLE coupon_usage_history (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '쿠폰 사용 이력 고유 식별자',
    coupon_id         BIGINT          NOT NULL COMMENT '쿠폰 ID',
    type              VARCHAR(30)     NOT NULL COMMENT '이력 유형 (USED, RESTORED, EXPIRED)',
    order_id          BIGINT          NULL     COMMENT '관련 주문 ID',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='쿠폰 사용/반환 이력';

CREATE INDEX idx_coupon_usage_coupon ON coupon_usage_history (coupon_id, created_at DESC);
```

#### [S-TBL-04] `admin_user` / `admin_action_log` 테이블
- **근거**: 여러 도메인에서 "관리자"가 행위 주체로 등장하지만(상품 심사, 리뷰 숨김, 문의 에스컬레이션, 기획전 관리 등), 관리자 계정과 감사 로그를 관리하는 테이블이 없다.
- 이커머스 플랫폼에서 관리자 행위 추적(audit trail)은 컴플라이언스 측면에서 필수다.

#### [S-TBL-05] `seller_commission_rate` 테이블
- **근거**: PRD 2.11절에서 "신규 입점 브랜드 프로모션 수수료(-5%p)", "월 매출 1억 이상 볼륨 디스카운트(-1%p)"를 언급하고 있으나, 셀러별 커스텀 수수료율을 관리하는 테이블이 없다. 현재는 `category.commission_rate`만 있어 셀러별 차등 적용이 불가하다.
```sql
CREATE TABLE seller_commission_rate (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '셀러 수수료율 고유 식별자',
    seller_id         BIGINT          NOT NULL COMMENT '셀러 ID',
    category_id       BIGINT          NULL     COMMENT '카테고리 ID (NULL이면 전체 카테고리)',
    rate              DECIMAL(5,2)    NOT NULL COMMENT '수수료율 (%)',
    reason            VARCHAR(200)    NOT NULL COMMENT '적용 사유 (신규 입점 프로모션, 볼륨 디스카운트 등)',
    valid_from        DATE            NOT NULL COMMENT '적용 시작일',
    valid_to          DATE            NULL     COMMENT '적용 종료일 (NULL이면 무기한)',
    active            TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '활성 여부',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='셀러별 커스텀 수수료율';

CREATE INDEX idx_seller_commission_seller ON seller_commission_rate (seller_id, active, valid_from);
```

---

## 8. 성능 관점

### 대용량 테이블 분석

| 테이블 | 1년 후 예상 규모 | 월 증가량 | 파티셔닝 필요 |
|--------|---------------|----------|-------------|
| search_log | 1억건 | 800만 | **필수** |
| notification | 5천만건 | 400만 | **필수** |
| ranking_snapshot | 5천만건 | 400만 | **필수** |
| stock_history | 3천만건 | 250만 | 권장 |
| point_history | 2천만건 | 150만 | 권장 |
| shipment_status_history | 1500만건 | 125만 | 권장 |
| coupon | 1천만건 | 80만 | 선택 |

### VIOLATION

#### [V-PERF-01] `search_log` 테이블: 파티셔닝 미설계
- **심각도**: HIGH
- **근거**: 월 800만건, 30일 보관 후 아카이브 정책이 있으나 파티셔닝이 설계되지 않았다. 1억건에서 DELETE 연산은 치명적인 성능 저하를 유발한다.
- **제안**: `searched_at` 기준 월별 RANGE 파티셔닝.
```sql
CREATE TABLE search_log (
    -- ... 기존 컬럼 동일 ...
    PRIMARY KEY (id, searched_at)  -- 파티셔닝 키 포함
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='검색 로그'
PARTITION BY RANGE (TO_DAYS(searched_at)) (
    PARTITION p202603 VALUES LESS THAN (TO_DAYS('2026-04-01')),
    PARTITION p202604 VALUES LESS THAN (TO_DAYS('2026-05-01')),
    PARTITION p202605 VALUES LESS THAN (TO_DAYS('2026-06-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

#### [V-PERF-02] `ranking_snapshot` 테이블: 파티셔닝 미설계
- **심각도**: HIGH
- **근거**: 월 400만건으로 급속 증가하며 "주기적 정리 필요"로 기술되어 있으나 파티셔닝이 없다.
- **제안**: `calculated_at` 기준 월별 RANGE 파티셔닝. DROP PARTITION으로 빠른 정리 가능.

### SUGGESTION

#### [S-PERF-01] `notification` 테이블 파티셔닝
- 90일 보관 정책, 월 400만건. `created_at` 기준 월별 파티셔닝 권장.

#### [S-PERF-02] 읽기 최적화: CQRS 패턴 적용 대상
- **product + sku + product_image**: 상품 상세 조회 시 3개 테이블 JOIN이 빈번. 읽기용 역정규화 뷰 또는 Redis 캐시 레이어 권장.
- **order + order_item**: 마이페이지 주문 목록 조회 시 JOIN 빈번. 조회 전용 테이블 또는 캐시 검토.

#### [S-PERF-03] `stock_history` 테이블 아카이빙 전략
- 월 250만건. 3개월 이상 이력은 별도 아카이브 테이블로 이동하는 배치 전략 필요.

---

## 최종 판정

### 구현 진행 가능 여부: **조건부 승인 (CONDITIONALLY APPROVED)**

#### 즉시 수정 필수 (구현 전)
1. **[V-PERF-01]** search_log 파티셔닝 설계 추가
2. **[V-PERF-02]** ranking_snapshot 파티셔닝 설계 추가
3. **[V-TBL-01]** order_status_history 테이블 추가
4. **[V-COL-03]** product 테이블에 gender 컬럼 추가
5. **[V-IDX-02]** cart_item에 (cart_id, sku_id) 유니크 인덱스 추가

#### 1차 스프린트 내 수정 권장
6. **[V-TBL-02]** 교환 요청 처리 방안 확정 (return_request 확장 또는 exchange_request 분리)
7. **[V-RULE-08]** coupon_policy의 ID 리스트 컬럼 정규화 (조인 테이블 분리)
8. **[V-COL-06]** ootd_snap.tagged_products 정규화 (ootd_product 테이블 분리)
9. **[S-TBL-01]** product_tag 테이블 추가
10. **[S-TBL-02]** brand_follow 테이블 추가

#### 2차 스프린트 이후 반영 가능
11. **[V-RULE-07]** 금융 테이블(payment, settlement)에 deleted_at 추가
12. **[V-COL-04]** product_material 테이블 추가
13. **[S-TBL-05]** seller_commission_rate 테이블 추가
14. **[S-TBL-04]** admin_user / admin_action_log 테이블 추가
15. **[S-PERF-01]** notification 파티셔닝

### 종합 평가

ERD 설계는 전체적으로 **높은 수준**이다. 15개 서비스, 64개 테이블이 PRD의 도메인 분석과 대부분 정합하며, DB 규칙(FK/JSON/ENUM 금지, TINYINT(1), DATETIME(6), COMMENT 필수)도 일관되게 준수되어 있다. 인덱스 전략, 데이터 볼륨 예측, Mermaid 다이어그램까지 포함된 것은 실무 적용 가능한 수준이다.

주요 보완 사항은:
- **대용량 테이블(search_log, ranking_snapshot)의 파티셔닝 설계**가 가장 시급하다.
- **주문 상태 이력 테이블** 누락은 운영 단계에서 반드시 필요하다.
- **성별(gender) 컬럼**이 없으면 패션 이커머스의 핵심 필터링이 불가하다.
- **콤마 구분 TEXT 패턴**은 조회 불가/인덱싱 불가하므로, ID 참조가 포함된 것은 정규화해야 한다.

위 5건의 필수 수정 사항을 반영하면 구현 진행이 가능하다.
