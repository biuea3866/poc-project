-- auth-service V3: email 평문 → AES-GCM 암호화 + email_hash 분리 (GDPR/PIPA PII 보호)
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
