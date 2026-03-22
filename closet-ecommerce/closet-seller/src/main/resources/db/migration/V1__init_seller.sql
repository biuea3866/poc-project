-- ============================================================
-- Seller Service Database Schema
-- ============================================================

CREATE TABLE seller (
    id                   BIGINT          NOT NULL AUTO_INCREMENT COMMENT '셀러 고유 식별자',
    email                VARCHAR(200)    NOT NULL COMMENT '셀러 이메일',
    name                 VARCHAR(50)     NOT NULL COMMENT '셀러 담당자 이름',
    business_name        VARCHAR(100)    NOT NULL COMMENT '사업체명',
    business_number      VARCHAR(20)     NOT NULL COMMENT '사업자등록번호',
    representative_name  VARCHAR(50)     NOT NULL COMMENT '대표자 이름',
    phone                VARCHAR(20)     NOT NULL COMMENT '연락처',
    status               VARCHAR(30)     NOT NULL DEFAULT 'PENDING' COMMENT '셀러 상태 (PENDING, ACTIVE, SUSPENDED, WITHDRAWN)',
    commission_rate      DECIMAL(5,2)    NULL     COMMENT '수수료율 (%)',
    created_at           DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at           DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    deleted_at           DATETIME(6)     NULL     COMMENT '삭제일시 (soft delete)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_seller_email (email),
    UNIQUE KEY uk_seller_business_number (business_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='셀러 정보';

CREATE TABLE seller_application (
    id                    BIGINT          NOT NULL AUTO_INCREMENT COMMENT '입점 신청 고유 식별자',
    seller_id             BIGINT          NOT NULL COMMENT '셀러 ID',
    brand_name            VARCHAR(100)    NOT NULL COMMENT '브랜드명',
    category_ids          TEXT            NULL     COMMENT '카테고리 ID 목록 (콤마 구분)',
    business_license_url  VARCHAR(500)    NOT NULL COMMENT '사업자등록증 URL',
    bank_name             VARCHAR(50)     NOT NULL COMMENT '은행명',
    account_number        VARCHAR(50)     NOT NULL COMMENT '계좌번호',
    account_holder        VARCHAR(50)     NOT NULL COMMENT '예금주',
    status                VARCHAR(30)     NOT NULL DEFAULT 'SUBMITTED' COMMENT '신청 상태 (SUBMITTED, REVIEWING, APPROVED, REJECTED)',
    reject_reason         VARCHAR(500)    NULL     COMMENT '반려 사유',
    submitted_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '신청일시',
    reviewed_at           DATETIME(6)     NULL     COMMENT '심사일시',
    created_at            DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='셀러 입점 신청';

CREATE TABLE seller_settlement_account (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '정산 계좌 고유 식별자',
    seller_id         BIGINT          NOT NULL COMMENT '셀러 ID',
    bank_name         VARCHAR(50)     NOT NULL COMMENT '은행명',
    account_number    VARCHAR(50)     NOT NULL COMMENT '계좌번호',
    account_holder    VARCHAR(50)     NOT NULL COMMENT '예금주',
    is_verified       TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '계좌 인증 여부 (1=인증, 0=미인증)',
    verified_at       DATETIME(6)     NULL     COMMENT '인증일시',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='셀러 정산 계좌';

-- 인덱스
CREATE INDEX idx_seller_status ON seller (status);
CREATE INDEX idx_seller_application_seller ON seller_application (seller_id);
CREATE INDEX idx_seller_application_status ON seller_application (status);
CREATE INDEX idx_seller_settlement_seller ON seller_settlement_account (seller_id);
