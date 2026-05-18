-- auth-service V2: refresh_tokens에 access_jti 컬럼 추가 (jti blacklist 정확 구현)
ALTER TABLE refresh_tokens
    ADD COLUMN access_jti CHAR(36) NULL COMMENT 'Access Token JTI (logout 시 blacklist 추가용)' AFTER token_hash,
    ADD KEY idx_refresh_tokens_access_jti (access_jti);
