-- auth-service V1: 7 tables (AT-DB)
-- UTC 저장: DATETIME(6), boolean: TINYINT(1), PK: BIGINT AUTO_INCREMENT

CREATE TABLE user_accounts
(
    id                     BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'PK',
    employment_id          BIGINT          NOT NULL COMMENT '연동 고용 ID (employee-service)',
    company_id             BIGINT          NOT NULL COMMENT '회사 ID',
    email                  VARCHAR(255)    NOT NULL COMMENT '이메일 (로그인 식별자)',
    password_hash          VARCHAR(255)    NOT NULL COMMENT 'bcrypt 해시',
    status                 VARCHAR(50)     NOT NULL COMMENT 'ACTIVE|LOCKED|SUSPENDED|DEACTIVATED',
    failed_login_attempts  INT             NOT NULL DEFAULT 0 COMMENT '연속 로그인 실패 횟수',
    locked_until           DATETIME(6)              COMMENT '잠금 해제 시각 (UTC)',
    last_login_at          DATETIME(6)              COMMENT '최근 로그인 성공 시각 (UTC)',
    two_factor_enabled     TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '2FA 활성화 여부',
    two_factor_secret      BLOB                     COMMENT 'AES-GCM 암호화 TOTP 시크릿',
    created_at             DATETIME(6)     NOT NULL COMMENT '생성 시각 (UTC)',
    created_by             BIGINT                   COMMENT '생성자 userAccountId',
    updated_at             DATETIME(6)     NOT NULL COMMENT '수정 시각 (UTC)',
    updated_by             BIGINT                   COMMENT '수정자 userAccountId',
    deleted_at             DATETIME(6)              COMMENT '삭제 시각 (소프트 삭제)',
    deleted_by             BIGINT                   COMMENT '삭제자 userAccountId',
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_accounts_email (email),
    KEY idx_user_accounts_employment_id (employment_id),
    KEY idx_user_accounts_company_id (company_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '사용자 계정';

CREATE TABLE roles
(
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    company_id     BIGINT                COMMENT '회사 ID (NULL=글로벌 시스템 역할)',
    code           VARCHAR(50)  NOT NULL COMMENT '역할 코드 (EMPLOYEE|TEAM_LEAD|HR_MANAGER|ADMIN)',
    name           VARCHAR(100) NOT NULL COMMENT '역할 표시명',
    description    TEXT                  COMMENT '역할 설명',
    is_system_role TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '시스템 역할 여부',
    created_at     DATETIME(6)  NOT NULL COMMENT '생성 시각 (UTC)',
    created_by     BIGINT                COMMENT '생성자 userAccountId',
    updated_at     DATETIME(6)  NOT NULL COMMENT '수정 시각 (UTC)',
    updated_by     BIGINT                COMMENT '수정자 userAccountId',
    deleted_at     DATETIME(6)           COMMENT '삭제 시각 (소프트 삭제)',
    deleted_by     BIGINT                COMMENT '삭제자 userAccountId',
    PRIMARY KEY (id),
    KEY idx_roles_company_id (company_id),
    KEY idx_roles_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '역할';

-- 글로벌 시스템 역할 시드 (company_id=NULL)
INSERT INTO roles (company_id, code, name, description, is_system_role, created_at, updated_at)
VALUES (NULL, 'EMPLOYEE', '일반 직원', '기본 직원 역할', 1, NOW(6), NOW(6)),
       (NULL, 'TEAM_LEAD', '팀장', '팀 리더 역할', 1, NOW(6), NOW(6)),
       (NULL, 'HR_MANAGER', 'HR 매니저', 'HR 관리자 역할', 1, NOW(6), NOW(6)),
       (NULL, 'ADMIN', '관리자', '전체 관리자 역할', 1, NOW(6), NOW(6));

CREATE TABLE user_account_roles
(
    id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'PK',
    user_account_id BIGINT      NOT NULL COMMENT '사용자 계정 ID',
    role_id         BIGINT      NOT NULL COMMENT '역할 ID',
    assigned_at     DATETIME(6) NOT NULL COMMENT '역할 부여 시각 (UTC)',
    assigned_by     BIGINT               COMMENT '역할 부여자 userAccountId',
    created_at      DATETIME(6) NOT NULL COMMENT '생성 시각 (UTC)',
    created_by      BIGINT               COMMENT '생성자 userAccountId',
    updated_at      DATETIME(6) NOT NULL COMMENT '수정 시각 (UTC)',
    updated_by      BIGINT               COMMENT '수정자 userAccountId',
    deleted_at      DATETIME(6)          COMMENT '삭제 시각 (소프트 삭제)',
    deleted_by      BIGINT               COMMENT '삭제자 userAccountId',
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_account_roles (user_account_id, role_id),
    KEY idx_user_account_roles_user_account_id (user_account_id),
    KEY idx_user_account_roles_role_id (role_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '사용자-역할 매핑 (M:N)';

CREATE TABLE refresh_tokens
(
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    user_account_id BIGINT       NOT NULL COMMENT '사용자 계정 ID',
    token_hash      VARCHAR(255) NOT NULL COMMENT 'SHA-256 해시',
    expires_at      DATETIME(6)  NOT NULL COMMENT '만료 시각 (UTC)',
    device_info     VARCHAR(500)          COMMENT '기기 정보',
    ip_address      VARCHAR(45)           COMMENT 'IPv4/IPv6',
    revoked_at      DATETIME(6)           COMMENT '폐기 시각 (UTC)',
    revoked_reason  VARCHAR(255)          COMMENT '폐기 사유',
    created_at      DATETIME(6)  NOT NULL COMMENT '생성 시각 (UTC)',
    created_by      BIGINT                COMMENT '생성자 userAccountId',
    updated_at      DATETIME(6)  NOT NULL COMMENT '수정 시각 (UTC)',
    updated_by      BIGINT                COMMENT '수정자 userAccountId',
    deleted_at      DATETIME(6)           COMMENT '삭제 시각 (소프트 삭제)',
    deleted_by      BIGINT                COMMENT '삭제자 userAccountId',
    PRIMARY KEY (id),
    UNIQUE KEY uq_refresh_tokens_token_hash (token_hash),
    KEY idx_refresh_tokens_user_account_id (user_account_id),
    KEY idx_refresh_tokens_expires_at (expires_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = 'Refresh Token';

CREATE TABLE login_attempts
(
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    user_account_id BIGINT                COMMENT '사용자 계정 ID (인증 실패 시 NULL 가능)',
    email           VARCHAR(255) NOT NULL COMMENT '시도한 이메일',
    attempted_at    DATETIME(6)  NOT NULL COMMENT '시도 시각 (UTC)',
    success         TINYINT(1)   NOT NULL COMMENT '성공 여부',
    failure_reason  VARCHAR(100)          COMMENT '실패 사유 enum',
    ip_address      VARCHAR(45)           COMMENT 'IPv4/IPv6',
    user_agent      VARCHAR(500)          COMMENT 'User-Agent',
    created_at      DATETIME(6)  NOT NULL COMMENT '생성 시각 (BaseEntity audit)',
    created_by      BIGINT                COMMENT '생성자 userAccountId',
    updated_at      DATETIME(6)  NOT NULL COMMENT '수정 시각 (BaseEntity audit, 불변이나 필드 필요)',
    updated_by      BIGINT                COMMENT '수정자 userAccountId',
    deleted_at      DATETIME(6)           COMMENT '삭제 시각 (논리 삭제, append-only이므로 미사용)',
    deleted_by      BIGINT                COMMENT '삭제자 userAccountId',
    PRIMARY KEY (id),
    KEY idx_login_attempts_email_attempted_at (email, attempted_at DESC),
    KEY idx_login_attempts_user_account_id (user_account_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '로그인 시도 이력 (append-only)';

CREATE TABLE two_factor_backup_codes
(
    id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'PK',
    user_account_id BIGINT      NOT NULL COMMENT '사용자 계정 ID',
    code_hash       VARCHAR(255) NOT NULL COMMENT 'bcrypt 해시',
    used_at         DATETIME(6)           COMMENT '사용 시각 (1회용)',
    created_at      DATETIME(6) NOT NULL COMMENT '생성 시각 (UTC)',
    created_by      BIGINT               COMMENT '생성자 userAccountId',
    updated_at      DATETIME(6) NOT NULL COMMENT '수정 시각 (UTC)',
    updated_by      BIGINT               COMMENT '수정자 userAccountId',
    deleted_at      DATETIME(6)          COMMENT '삭제 시각 (소프트 삭제)',
    deleted_by      BIGINT               COMMENT '삭제자 userAccountId',
    PRIMARY KEY (id),
    KEY idx_two_factor_backup_codes_user_account_id (user_account_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '2FA 백업 코드';

CREATE TABLE api_tokens
(
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    user_account_id BIGINT       NOT NULL COMMENT '사용자 계정 ID',
    name            VARCHAR(100) NOT NULL COMMENT '토큰 이름',
    token_hash      VARCHAR(255) NOT NULL COMMENT 'SHA-256 해시 (hrp_ prefix 제거 후)',
    scopes          TEXT                  COMMENT '허용 스코프 목록 (직렬화 배열)',
    expires_at      DATETIME(6)           COMMENT '만료 시각 (NULL=무기한)',
    last_used_at    DATETIME(6)           COMMENT '최근 사용 시각',
    revoked_at      DATETIME(6)           COMMENT '폐기 시각',
    created_at      DATETIME(6)  NOT NULL COMMENT '생성 시각 (UTC)',
    created_by      BIGINT                COMMENT '생성자 userAccountId',
    updated_at      DATETIME(6)  NOT NULL COMMENT '수정 시각 (UTC)',
    updated_by      BIGINT                COMMENT '수정자 userAccountId',
    deleted_at      DATETIME(6)           COMMENT '삭제 시각 (소프트 삭제)',
    deleted_by      BIGINT                COMMENT '삭제자 userAccountId',
    PRIMARY KEY (id),
    UNIQUE KEY uq_api_tokens_token_hash (token_hash),
    KEY idx_api_tokens_user_account_id (user_account_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = 'API 토큰';
