-- =============================================================================
-- V5: 4개 테이블에 audit + soft-delete 컬럼 추가
--
-- 대상 테이블: person, employment, department, employment_history
-- 추가 컬럼:
--   created_by BIGINT NULL  — 생성한 employmentId (시스템 액션은 NULL)
--   updated_by BIGINT NULL  — 마지막 수정한 employmentId
--   deleted_at TIMESTAMP(6) NULL — 소프트 딜리트 시각 (UTC, NULL이면 미삭제)
--   deleted_by BIGINT NULL  — 삭제한 employmentId
--
-- 하위 호환성:
--   - 모든 컬럼이 NULL 허용이므로 기존 데이터 영향 없음 (비파괴적 변경)
--   - ADD COLUMN NULL: 기존 행에 NULL 자동 설정, 락 없음 (InnoDB Online DDL)
--   - ADD INDEX: ALGORITHM=INPLACE, LOCK=NONE 명시하여 운영 환경 무중단 보장
--
-- 파괴적 변경: 0건
--
-- ADR 결정 (employment_history):
--   - append-only 이력 테이블이므로 deleted_at/deleted_by는 실제 사용하지 않음
--   - 애플리케이션 레이어 BaseAuditEntity 일관성을 위해 동일 컬럼 부착
--   - 사용하지 않음을 COMMENT에 명시
--
-- 롤백:
--   ALTER TABLE person       DROP COLUMN created_by, DROP COLUMN updated_by, DROP COLUMN deleted_at, DROP COLUMN deleted_by, DROP INDEX idx_person_deleted_at;
--   ALTER TABLE employment   DROP COLUMN created_by, DROP COLUMN updated_by, DROP COLUMN deleted_at, DROP COLUMN deleted_by, DROP INDEX idx_employment_deleted_at;
--   ALTER TABLE department   DROP COLUMN created_by, DROP COLUMN updated_by, DROP COLUMN deleted_at, DROP COLUMN deleted_by, DROP INDEX idx_department_deleted_at;
--   ALTER TABLE employment_history DROP COLUMN created_by, DROP COLUMN updated_by, DROP COLUMN deleted_at, DROP COLUMN deleted_by, DROP INDEX idx_employment_history_deleted_at;
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────
-- person
-- ─────────────────────────────────────────────────────────────────
ALTER TABLE person
    ADD COLUMN created_by BIGINT          NULL COMMENT '생성한 employmentId (시스템 액션은 NULL)',
    ADD COLUMN updated_by BIGINT          NULL COMMENT '마지막 수정한 employmentId',
    ADD COLUMN deleted_at TIMESTAMP(6)    NULL COMMENT '소프트 딜리트 시각 (UTC, NULL이면 미삭제)',
    ADD COLUMN deleted_by BIGINT          NULL COMMENT '삭제한 employmentId',
    ADD INDEX  idx_person_deleted_at (deleted_at) COMMENT 'soft-delete 필터링용',
    ALGORITHM = INPLACE,
    LOCK      = NONE;

-- ─────────────────────────────────────────────────────────────────
-- employment
-- ─────────────────────────────────────────────────────────────────
ALTER TABLE employment
    ADD COLUMN created_by BIGINT          NULL COMMENT '생성한 employmentId',
    ADD COLUMN updated_by BIGINT          NULL COMMENT '마지막 수정한 employmentId',
    ADD COLUMN deleted_at TIMESTAMP(6)    NULL COMMENT '소프트 딜리트 시각 (UTC, NULL이면 미삭제)',
    ADD COLUMN deleted_by BIGINT          NULL COMMENT '삭제한 employmentId',
    ADD INDEX  idx_employment_deleted_at (deleted_at) COMMENT 'soft-delete 필터링용',
    ALGORITHM = INPLACE,
    LOCK      = NONE;

-- ─────────────────────────────────────────────────────────────────
-- department
-- ─────────────────────────────────────────────────────────────────
ALTER TABLE department
    ADD COLUMN created_by BIGINT          NULL COMMENT '생성한 employmentId',
    ADD COLUMN updated_by BIGINT          NULL COMMENT '마지막 수정한 employmentId',
    ADD COLUMN deleted_at TIMESTAMP(6)    NULL COMMENT '소프트 딜리트 시각 (UTC, NULL이면 미삭제)',
    ADD COLUMN deleted_by BIGINT          NULL COMMENT '삭제한 employmentId',
    ADD INDEX  idx_department_deleted_at (deleted_at) COMMENT 'soft-delete 필터링용',
    ALGORITHM = INPLACE,
    LOCK      = NONE;

-- ─────────────────────────────────────────────────────────────────
-- employment_history (append-only — deleted_at/deleted_by 사용 안 함, 일관성 목적 부착)
-- ─────────────────────────────────────────────────────────────────
ALTER TABLE employment_history
    ADD COLUMN created_by BIGINT          NULL COMMENT '생성한 employmentId (시스템 처리 시 NULL)',
    ADD COLUMN updated_by BIGINT          NULL COMMENT '마지막 수정한 employmentId (append-only — 사용 안 함, BaseAuditEntity 일관성용)',
    ADD COLUMN deleted_at TIMESTAMP(6)    NULL COMMENT '소프트 딜리트 시각 (append-only — 사용 안 함, BaseAuditEntity 일관성용)',
    ADD COLUMN deleted_by BIGINT          NULL COMMENT '삭제한 employmentId (append-only — 사용 안 함, BaseAuditEntity 일관성용)',
    ADD INDEX  idx_employment_history_deleted_at (deleted_at) COMMENT 'soft-delete 필터링용 (append-only 특성상 실제 쿼리 없음)',
    ALGORITHM = INPLACE,
    LOCK      = NONE;
