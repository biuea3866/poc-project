-- =============================================================================
-- V2: employment 테이블 생성
-- Person·Company 페어당 하나의 고용 인스턴스.
-- 상태 머신: PRE_HIRED → ACTIVE → ON_LEAVE → ACTIVE | RESIGNED
-- FK 미사용 — 서비스 경계 원칙(ADR-002 §1, PRD §10.4).
-- 보상 정보: 정수 minorUnits(base_salary)로 부동소수점 오류 방지.
-- additional_compensation: 급여 외 보상(인센티브·스톡옵션 등) 직렬화 배열 (JsonStringType으로 처리).
--
-- 롤백: DROP TABLE IF EXISTS employment;
-- =============================================================================

CREATE TABLE employment
(
    id                       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '고용 인스턴스 고유 식별자 (PK)',
    person_id                BIGINT       NOT NULL               COMMENT 'person.id 참조 (FK 미사용, 서비스 경계)',
    company_id               BIGINT       NOT NULL               COMMENT '소속 회사 식별자',
    employee_number          VARCHAR(50)  NOT NULL               COMMENT '사원 번호 — 회사 내 고유',
    employment_type          VARCHAR(20)  NOT NULL               COMMENT '고용 형태 — REGULAR / CONTRACT / PART_TIME / INTERN',
    status                   VARCHAR(20)  NOT NULL               COMMENT '고용 상태 — PRE_HIRED / ACTIVE / ON_LEAVE / RESIGNED',
    start_date               DATE         NOT NULL               COMMENT '고용 시작일',
    end_date                 DATE             NULL               COMMENT '고용 종료일 — 계약직·기간제 한정, 정규직은 NULL',
    country                  CHAR(2)      NOT NULL               COMMENT '근무 국가 코드 — ISO 3166-1 alpha-2',
    currency                 CHAR(3)      NOT NULL               COMMENT '급여 기준 통화 코드 — ISO 4217 (예: KRW, USD)',
    timezone                 VARCHAR(64)  NOT NULL               COMMENT '근무 타임존 — IANA TZ (예: Asia/Seoul)',
    position_id              BIGINT           NULL               COMMENT '직책 식별자 — position 도메인 참조 (FK 미사용)',
    department_id            BIGINT           NULL               COMMENT 'department.id 참조 — 소속 부서 (FK 미사용)',
    manager_employment_id    BIGINT           NULL               COMMENT '직속 상관 employment.id 참조 (FK 미사용)',
    work_schedule_policy_id  BIGINT           NULL               COMMENT '근무 일정 정책 식별자 (FK 미사용)',
    leave_policy_id          BIGINT           NULL               COMMENT '휴가 정책 식별자 (FK 미사용)',
    base_salary              BIGINT           NULL               COMMENT '기본급 — 정수 minorUnits (예: KRW 3,000,000원 → 300000000)',
    compensation_currency    CHAR(3)          NULL               COMMENT '보상 통화 코드 — ISO 4217',
    additional_compensation  TEXT             NULL               COMMENT '급여 외 추가 보상 배열 (인센티브·스톡옵션 등) 직렬화 문자열. 애플리케이션에서 JsonStringType으로 처리',
    created_at               TIMESTAMP(6) NOT NULL               COMMENT '레코드 최초 생성 시각 (UTC)',
    updated_at               TIMESTAMP(6) NOT NULL               COMMENT '레코드 최종 수정 시각 (UTC)',
    PRIMARY KEY (id),
    UNIQUE KEY uq_employment_company_employee_number (company_id, employee_number),
    INDEX idx_employment_person_id (person_id),
    INDEX idx_employment_department_id (department_id),
    INDEX idx_employment_manager_employment_id (manager_employment_id),
    INDEX idx_employment_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '고용 인스턴스 — Person·Company 페어. 상태 머신(PRE_HIRED/ACTIVE/ON_LEAVE/RESIGNED) 및 조직·보상 정보 보유.';
