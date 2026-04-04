-- ============================================================
-- CP-03: 회원 역할(role) 컬럼 추가
-- 기존 회원은 BUYER로 기본 설정 (하위 호환)
-- ============================================================

ALTER TABLE member
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'BUYER' COMMENT '회원 역할 (BUYER/SELLER/ADMIN)'
    AFTER status;
