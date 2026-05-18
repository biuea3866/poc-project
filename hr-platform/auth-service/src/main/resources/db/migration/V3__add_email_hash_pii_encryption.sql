-- auth-service V3: email 평문 → AES-GCM 암호화 + email_hash 분리 (GDPR/PIPA PII 보호)
-- ============================================================
-- WARNING: 본 마이그레이션은 DEV/TEST 환경 전용 (운영 데이터 0건 가정).
-- 운영 적용 시 평문 email → AES 암호화 + HMAC hash 채우기 backfill 스크립트가 선행되어야 함.
-- backfill 미수행 상태로 운영 배포 시 모든 user_account 로딩 실패(BadPadding) → 전 사용자 로그인 차단.
-- 운영 배포 가드: hrplatform.deploy.allow-destructive-migration=true 환경변수 또는 별도 운영 절차 필요.
-- ============================================================
-- user_accounts: email 평문 인덱스 제거 + VARBINARY 타입 전환 + email_hash 컬럼 추가
ALTER TABLE user_accounts
    DROP INDEX uq_user_accounts_email;

ALTER TABLE user_accounts
    MODIFY COLUMN email VARBINARY(500) NULL COMMENT 'AES-256-GCM 암호화 이메일 (기존 운영 데이터는 별도 backfill 절차 필요)',
    ADD COLUMN email_hash VARCHAR(64) NULL COMMENT 'HMAC-SHA-256(email) hex — deterministic lookup' AFTER email,
    ADD UNIQUE KEY uq_user_accounts_email_hash (email_hash);

-- login_attempts: email 평문 컬럼 → email_hash 전환
ALTER TABLE login_attempts
    DROP KEY idx_login_attempts_email_attempted_at,
    ADD COLUMN email_hash VARCHAR(64) NULL COMMENT 'HMAC-SHA-256(email) hex' AFTER email,
    ADD KEY idx_login_attempts_email_hash_attempted_at (email_hash, attempted_at DESC);

ALTER TABLE login_attempts
    DROP COLUMN email;
