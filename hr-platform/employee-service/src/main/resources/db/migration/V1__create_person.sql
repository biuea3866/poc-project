-- =============================================================================
-- V1: person 테이블 생성
-- 불변 신원 정보(PII)를 보관하는 자연인 엔티티.
-- 한 사람이 여러 회사에서 고용될 수 있으므로 Employment와 분리.
-- PII 컬럼(personal_email, phone_number)은 애플리케이션 레이어에서 AES-256-GCM으로
-- 암복호화하며 DB에는 암호화된 바이트열로 저장한다.
--
-- 롤백: DROP TABLE IF EXISTS person;
-- =============================================================================

CREATE TABLE person
(
    id                 BIGINT          NOT NULL AUTO_INCREMENT COMMENT '자연인 고유 식별자 (PK)',
    name               VARCHAR(100)    NOT NULL                COMMENT '법적 성명',
    personal_email     VARBINARY(255)  NOT NULL                COMMENT '개인 이메일 — AES-256-GCM 암호화 바이트열 (PII)',
    phone_number       VARBINARY(255)      NULL                COMMENT '휴대전화 번호 — AES-256-GCM 암호화 바이트열 (PII)',
    birth_date         DATE                NULL                COMMENT '생년월일 (PII)',
    nationality        CHAR(2)             NULL                COMMENT '국적 코드 — ISO 3166-1 alpha-2 (예: KR, US)',
    gender             VARCHAR(15)         NULL                COMMENT '성별 — MALE / FEMALE / OTHER / UNDISCLOSED',
    emergency_contacts TEXT                NULL                COMMENT '비상연락처 배열 — [{name, relation, phone}] 직렬화 문자열. 애플리케이션에서 JsonStringType으로 처리',
    created_at         TIMESTAMP(6)    NOT NULL                COMMENT '레코드 최초 생성 시각 (UTC, ZonedDateTime 저장)',
    updated_at         TIMESTAMP(6)    NOT NULL                COMMENT '레코드 최종 수정 시각 (UTC, ZonedDateTime 저장)',
    PRIMARY KEY (id),
    UNIQUE KEY uq_person_personal_email (personal_email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '자연인(불변 신원) — PII 보관 테이블. Employment와 1:N 관계.';
