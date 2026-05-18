-- =============================================================================
-- V1: auth-service 7개 테이블 초기 생성
--
-- 테이블 목록:
--   1. user_account       — 사용자 계정 (인증 주체, 4상태 FSM)
--   2. role               — 역할 정의
--   3. user_account_role  — 사용자↔역할 N:M 매핑
--   4. refresh_token      — JWT Refresh Token (SHA-256 해시 저장)
--   5. login_attempt      — 로그인 시도 이력 (append-only)
--   6. two_factor_backup_code — 2FA 백업 코드 (bcrypt 해시)
--   7. api_token          — API 토큰 (long-lived, scope 기반)
--
-- 설계 결정:
--   - FK 미사용: 서비스 경계 간 참조는 애플리케이션 레이어에서 관리 (ADR-003 §1)
--   - ENUM 미사용: VARCHAR로 대체 (추후 상태 추가 시 DDL 없이 가능)
--   - scopes 컬럼은 TEXT 타입 (콤마 구분 문자열, 애플리케이션 파싱 — DB 타입 정책 준수)
--   - BOOLEAN 미사용: TINYINT(1) 사용
--   - 모든 시간 컬럼: TIMESTAMP(6) UTC (ZonedDateTime 저장)
--   - 2FA 시크릿: VARBINARY(255) — AES-256-GCM 암호화 바이트열
--   - 비밀번호: VARCHAR(60) — bcrypt cost-12 고정 길이
--
-- 파괴적 변경: 0건 (신규 테이블 생성)
--
-- 롤백:
--   DROP TABLE IF EXISTS api_token;
--   DROP TABLE IF EXISTS two_factor_backup_code;
--   DROP TABLE IF EXISTS login_attempt;
--   DROP TABLE IF EXISTS refresh_token;
--   DROP TABLE IF EXISTS user_account_role;
--   DROP TABLE IF EXISTS role;
--   DROP TABLE IF EXISTS user_account;
-- =============================================================================

-- =============================================================================
-- 1. user_account — 사용자 계정
-- =============================================================================
CREATE TABLE user_account
(
    id                    BIGINT          NOT NULL AUTO_INCREMENT     COMMENT '사용자 계정 고유 식별자 (PK)',
    employment_id         BIGINT          NOT NULL                    COMMENT 'employee-service Employment 식별자 (1:1 매핑, FK 미사용)',
    company_id            BIGINT          NOT NULL                    COMMENT '소속 회사 식별자',
    email                 VARCHAR(255)    NOT NULL                    COMMENT '로그인 이메일 (유니크)',
    password_hash         VARCHAR(60)     NOT NULL                    COMMENT 'bcrypt cost-12 해시 (고정 60자)',
    status                VARCHAR(20)     NOT NULL                    COMMENT '계정 상태 — ACTIVE / LOCKED / SUSPENDED / DEACTIVATED',
    failed_login_attempts INT             NOT NULL DEFAULT 0          COMMENT '연속 로그인 실패 횟수 (5회 초과 시 LOCKED)',
    locked_until          TIMESTAMP(6)        NULL                    COMMENT '잠금 해제 예정 시각 (UTC, NULL이면 잠금 없음)',
    last_login_at         TIMESTAMP(6)        NULL                    COMMENT '마지막 로그인 성공 시각 (UTC)',
    two_factor_enabled    TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '2FA 활성화 여부 (0: 비활성, 1: 활성)',
    two_factor_secret     VARBINARY(255)      NULL                    COMMENT '2FA TOTP 시크릿 — AES-256-GCM 암호화 바이트열 (NULL이면 2FA 미설정)',
    created_at            TIMESTAMP(6)    NOT NULL                    COMMENT '레코드 최초 생성 시각 (UTC)',
    created_by            BIGINT              NULL                    COMMENT '생성한 user_account_id (시스템 액션은 NULL)',
    updated_at            TIMESTAMP(6)    NOT NULL                    COMMENT '레코드 최종 수정 시각 (UTC)',
    updated_by            BIGINT              NULL                    COMMENT '마지막 수정한 user_account_id',
    deleted_at            TIMESTAMP(6)        NULL                    COMMENT '소프트 딜리트 시각 (UTC, NULL이면 미삭제)',
    deleted_by            BIGINT              NULL                    COMMENT '삭제한 user_account_id',
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_account_employment_id (employment_id)          COMMENT 'Employment 1:1 보장',
    UNIQUE KEY uq_user_account_email         (email)                  COMMENT '이메일 중복 방지',
    INDEX      idx_user_account_company_id   (company_id)             COMMENT '회사별 계정 조회',
    INDEX      idx_user_account_status       (status)                 COMMENT '상태별 필터링',
    INDEX      idx_user_account_deleted_at   (deleted_at)             COMMENT 'soft-delete 필터링'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '사용자 계정 — 인증 주체 (ACTIVE/LOCKED/SUSPENDED/DEACTIVATED 4상태 FSM)';

-- =============================================================================
-- 2. role — 역할 정의
-- =============================================================================
CREATE TABLE role
(
    id             BIGINT          NOT NULL AUTO_INCREMENT COMMENT '역할 고유 식별자 (PK)',
    company_id     BIGINT          NOT NULL               COMMENT '소속 회사 식별자 (0이면 시스템 공통 역할)',
    code           VARCHAR(50)     NOT NULL               COMMENT '역할 코드 — 회사 내 유니크 (예: ADMIN, HR_MANAGER)',
    name           VARCHAR(100)    NOT NULL               COMMENT '역할 표시명',
    description    VARCHAR(500)        NULL               COMMENT '역할 설명',
    is_system_role TINYINT(1)      NOT NULL DEFAULT 0     COMMENT '시스템 기본 역할 여부 (0: 커스텀, 1: 시스템 — 삭제 불가)',
    created_at     TIMESTAMP(6)    NOT NULL               COMMENT '레코드 최초 생성 시각 (UTC)',
    created_by     BIGINT              NULL               COMMENT '생성한 user_account_id (시스템 액션은 NULL)',
    updated_at     TIMESTAMP(6)    NOT NULL               COMMENT '레코드 최종 수정 시각 (UTC)',
    updated_by     BIGINT              NULL               COMMENT '마지막 수정한 user_account_id',
    deleted_at     TIMESTAMP(6)        NULL               COMMENT '소프트 딜리트 시각 (UTC, NULL이면 미삭제)',
    deleted_by     BIGINT              NULL               COMMENT '삭제한 user_account_id',
    PRIMARY KEY (id),
    UNIQUE KEY uq_role_company_code   (company_id, code)  COMMENT '회사 내 역할 코드 중복 방지',
    INDEX      idx_role_deleted_at    (deleted_at)         COMMENT 'soft-delete 필터링'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '역할 정의 — 회사별 RBAC 역할 (시스템 역할 + 커스텀 역할)';

-- =============================================================================
-- 3. user_account_role — 사용자↔역할 N:M 매핑
-- =============================================================================
CREATE TABLE user_account_role
(
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '매핑 고유 식별자 (PK)',
    user_account_id BIGINT       NOT NULL               COMMENT '사용자 계정 식별자 (user_account.id 참조, FK 미사용)',
    role_id         BIGINT       NOT NULL               COMMENT '역할 식별자 (role.id 참조, FK 미사용)',
    assigned_at     TIMESTAMP(6) NOT NULL               COMMENT '역할 부여 시각 (UTC)',
    assigned_by     BIGINT           NULL               COMMENT '역할을 부여한 user_account_id (시스템 자동 부여는 NULL)',
    created_at      TIMESTAMP(6) NOT NULL               COMMENT '레코드 최초 생성 시각 (UTC)',
    created_by      BIGINT           NULL               COMMENT '생성한 user_account_id',
    updated_at      TIMESTAMP(6) NOT NULL               COMMENT '레코드 최종 수정 시각 (UTC)',
    updated_by      BIGINT           NULL               COMMENT '마지막 수정한 user_account_id',
    deleted_at      TIMESTAMP(6)     NULL               COMMENT '소프트 딜리트 시각 (UTC, NULL이면 미삭제)',
    deleted_by      BIGINT           NULL               COMMENT '삭제한 user_account_id',
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_account_role_mapping (user_account_id, role_id) COMMENT '동일 역할 중복 부여 방지',
    INDEX      idx_user_account_role_account (user_account_id)         COMMENT '사용자별 역할 목록 조회',
    INDEX      idx_user_account_role_role    (role_id)                 COMMENT '역할별 보유 사용자 조회'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '사용자↔역할 N:M 매핑 — 한 사용자는 여러 역할 보유 가능';

-- =============================================================================
-- 4. refresh_token — JWT Refresh Token
-- =============================================================================
CREATE TABLE refresh_token
(
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'Refresh Token 고유 식별자 (PK)',
    user_account_id BIGINT          NOT NULL               COMMENT '발급 대상 user_account_id (FK 미사용)',
    token_hash      CHAR(64)        NOT NULL               COMMENT 'SHA-256 해시 (원문 토큰은 미저장, 64자 hex)',
    expires_at      TIMESTAMP(6)    NOT NULL               COMMENT '토큰 만료 시각 (UTC)',
    device_info     VARCHAR(500)        NULL               COMMENT '발급 디바이스 정보 (User-Agent 요약)',
    ip_address      VARCHAR(45)         NULL               COMMENT '발급 IP 주소 (IPv4: 최대 15자, IPv6: 최대 45자)',
    revoked_at      TIMESTAMP(6)        NULL               COMMENT '토큰 폐기 시각 (UTC, NULL이면 유효)',
    created_at      TIMESTAMP(6)    NOT NULL               COMMENT '레코드 최초 생성 시각 (UTC)',
    created_by      BIGINT              NULL               COMMENT '생성한 user_account_id',
    updated_at      TIMESTAMP(6)    NOT NULL               COMMENT '레코드 최종 수정 시각 (UTC)',
    updated_by      BIGINT              NULL               COMMENT '마지막 수정한 user_account_id',
    deleted_at      TIMESTAMP(6)        NULL               COMMENT '소프트 딜리트 시각 (UTC, NULL이면 미삭제)',
    deleted_by      BIGINT              NULL               COMMENT '삭제한 user_account_id',
    PRIMARY KEY (id),
    UNIQUE KEY uq_refresh_token_hash          (token_hash)            COMMENT '토큰 해시 중복 방지 (조회 키)',
    INDEX      idx_refresh_token_account      (user_account_id)       COMMENT '사용자별 토큰 목록 조회',
    INDEX      idx_refresh_token_expires_at   (expires_at)            COMMENT '만료 토큰 정리 배치용',
    INDEX      idx_refresh_token_deleted_at   (deleted_at)            COMMENT 'soft-delete 필터링'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'JWT Refresh Token — SHA-256 해시만 저장, 원문 미보관';

-- =============================================================================
-- 5. login_attempt — 로그인 시도 이력 (append-only)
-- =============================================================================
CREATE TABLE login_attempt
(
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '로그인 시도 고유 식별자 (PK)',
    user_account_id BIGINT              NULL               COMMENT '시도 대상 user_account_id (계정 미존재 시도는 NULL)',
    email           VARCHAR(255)    NOT NULL               COMMENT '로그인 시도에 사용된 이메일',
    attempted_at    TIMESTAMP(6)    NOT NULL               COMMENT '시도 시각 (UTC)',
    success         TINYINT(1)      NOT NULL               COMMENT '로그인 성공 여부 (0: 실패, 1: 성공)',
    failure_reason  VARCHAR(100)        NULL               COMMENT '실패 원인 코드 (예: INVALID_PASSWORD, ACCOUNT_LOCKED, NULL이면 성공)',
    ip_address      VARCHAR(45)         NULL               COMMENT '시도 IP 주소 (IPv4: 최대 15자, IPv6: 최대 45자)',
    user_agent      VARCHAR(500)        NULL               COMMENT '시도 클라이언트 User-Agent',
    created_at      TIMESTAMP(6)    NOT NULL               COMMENT '레코드 생성 시각 (UTC, append-only — updated_at 미사용)',
    created_by      BIGINT              NULL               COMMENT '생성한 user_account_id (시스템 기록은 NULL)',
    PRIMARY KEY (id),
    INDEX      idx_login_attempt_account_at  (user_account_id, attempted_at) COMMENT '계정별 시도 이력 조회 (최신순)',
    INDEX      idx_login_attempt_email_at    (email, attempted_at)           COMMENT '이메일별 시도 이력 조회 (최신순, 브루트포스 감지)'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '로그인 시도 이력 — append-only (수정/삭제 없음, 보안 감사 로그)';

-- =============================================================================
-- 6. two_factor_backup_code — 2FA 백업 코드
-- =============================================================================
CREATE TABLE two_factor_backup_code
(
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '백업 코드 고유 식별자 (PK)',
    user_account_id BIGINT       NOT NULL               COMMENT '소유 user_account_id (FK 미사용)',
    code_hash       CHAR(60)     NOT NULL               COMMENT 'bcrypt 해시 (60자 고정, 원문 미보관)',
    used_at         TIMESTAMP(6)     NULL               COMMENT '백업 코드 사용 시각 (UTC, NULL이면 미사용)',
    created_at      TIMESTAMP(6) NOT NULL               COMMENT '레코드 최초 생성 시각 (UTC)',
    created_by      BIGINT           NULL               COMMENT '생성한 user_account_id',
    updated_at      TIMESTAMP(6) NOT NULL               COMMENT '레코드 최종 수정 시각 (UTC)',
    updated_by      BIGINT           NULL               COMMENT '마지막 수정한 user_account_id',
    deleted_at      TIMESTAMP(6)     NULL               COMMENT '소프트 딜리트 시각 (UTC, NULL이면 미삭제)',
    deleted_by      BIGINT           NULL               COMMENT '삭제한 user_account_id',
    PRIMARY KEY (id),
    INDEX      idx_two_factor_backup_code_account (user_account_id) COMMENT '사용자별 백업 코드 목록 조회'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '2FA 백업 코드 — bcrypt 해시 저장, 1회 사용 후 used_at 기록';

-- =============================================================================
-- 7. api_token — API 토큰 (long-lived, scope 기반)
-- =============================================================================
CREATE TABLE api_token
(
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'API Token 고유 식별자 (PK)',
    user_account_id BIGINT          NOT NULL               COMMENT '발급 대상 user_account_id (FK 미사용)',
    name            VARCHAR(100)    NOT NULL               COMMENT '토큰 식별 명칭 (사용자 지정)',
    token_hash      CHAR(64)        NOT NULL               COMMENT 'SHA-256 해시 (64자 hex, 원문 미보관)',
    scopes          TEXT                NULL               COMMENT '허용 스코프 목록 — 콤마 구분 문자열 (예: read:profile,write:openings). 애플리케이션에서 파싱',
    expires_at      TIMESTAMP(6)        NULL               COMMENT '토큰 만료 시각 (UTC, NULL이면 무기한)',
    last_used_at    TIMESTAMP(6)        NULL               COMMENT '마지막 사용 시각 (UTC)',
    revoked_at      TIMESTAMP(6)        NULL               COMMENT '토큰 폐기 시각 (UTC, NULL이면 유효)',
    created_at      TIMESTAMP(6)    NOT NULL               COMMENT '레코드 최초 생성 시각 (UTC)',
    created_by      BIGINT              NULL               COMMENT '생성한 user_account_id',
    updated_at      TIMESTAMP(6)    NOT NULL               COMMENT '레코드 최종 수정 시각 (UTC)',
    updated_by      BIGINT              NULL               COMMENT '마지막 수정한 user_account_id',
    deleted_at      TIMESTAMP(6)        NULL               COMMENT '소프트 딜리트 시각 (UTC, NULL이면 미삭제)',
    deleted_by      BIGINT              NULL               COMMENT '삭제한 user_account_id',
    PRIMARY KEY (id),
    UNIQUE KEY uq_api_token_hash        (token_hash)        COMMENT '토큰 해시 중복 방지 (조회 키)',
    INDEX      idx_api_token_account    (user_account_id)   COMMENT '사용자별 API 토큰 목록 조회',
    INDEX      idx_api_token_deleted_at (deleted_at)        COMMENT 'soft-delete 필터링'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'API 토큰 — long-lived, scope 기반 접근 제어. SHA-256 해시만 저장';
