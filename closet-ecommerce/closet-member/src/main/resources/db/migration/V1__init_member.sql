-- ============================================================
-- Member Service Database Schema
-- ============================================================

CREATE TABLE member (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '회원 고유 식별자',
    email             VARCHAR(200)    NOT NULL COMMENT '이메일 (로그인 ID)',
    password_hash     VARCHAR(200)    NULL     COMMENT '비밀번호 (BCrypt)',
    name              VARCHAR(50)     NOT NULL COMMENT '이름',
    phone             VARCHAR(20)     NULL     COMMENT '전화번호',
    grade             VARCHAR(30)     NOT NULL DEFAULT 'NORMAL' COMMENT '회원 등급 (NORMAL, SILVER, GOLD, PLATINUM)',
    point_balance     INT             NOT NULL DEFAULT 0 COMMENT '포인트 잔액',
    status            VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE' COMMENT '회원 상태 (ACTIVE, INACTIVE, WITHDRAWN)',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '가입일시',
    updated_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    deleted_at        DATETIME(6)     NULL     COMMENT '탈퇴일시 (soft delete)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='회원 정보';

CREATE TABLE shipping_address (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '배송지 고유 식별자',
    member_id         BIGINT          NOT NULL COMMENT '회원 ID',
    name              VARCHAR(50)     NOT NULL COMMENT '수령인 이름',
    phone             VARCHAR(20)     NOT NULL COMMENT '수령인 전화번호',
    zip_code          VARCHAR(10)     NOT NULL COMMENT '우편번호',
    address           VARCHAR(200)    NOT NULL COMMENT '주소',
    detail_address    VARCHAR(200)    NULL     COMMENT '상세주소',
    is_default        TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '기본 배송지 여부 (1=기본, 0=일반)',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
    deleted_at        DATETIME(6)     NULL     COMMENT '삭제일시 (soft delete)',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='회원 배송지';

CREATE TABLE point_history (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '포인트 이력 고유 식별자',
    member_id         BIGINT          NOT NULL COMMENT '회원 ID',
    type              VARCHAR(30)     NOT NULL COMMENT '유형 (EARN, USE, EXPIRE, CANCEL)',
    amount            INT             NOT NULL COMMENT '변동 금액',
    balance_after     INT             NOT NULL COMMENT '변동 후 잔액',
    reason            VARCHAR(200)    NOT NULL COMMENT '사유',
    reference_id      VARCHAR(100)    NULL     COMMENT '참조 ID (주문번호 등)',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='포인트 변동 이력';

-- 인덱스
CREATE INDEX idx_shipping_address_member ON shipping_address (member_id, is_default, deleted_at);
CREATE INDEX idx_point_history_member ON point_history (member_id, created_at DESC);
CREATE INDEX idx_member_status ON member (status);
