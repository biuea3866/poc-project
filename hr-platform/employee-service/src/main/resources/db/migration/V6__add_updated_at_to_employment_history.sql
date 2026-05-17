-- =============================================================================
-- V6: employment_history 테이블에 updated_at 컬럼 추가
--
-- BaseEntity 상속 일관성을 위해 추가.
-- append-only 특성상 실제 사용하지 않으나, JPA @LastModifiedDate 매핑을 위해 필요.
-- NULL 허용 → 기존 데이터 영향 없음. InnoDB Online DDL (INPLACE, LOCK NONE).
--
-- 롤백:
--   ALTER TABLE employment_history DROP COLUMN updated_at;
-- =============================================================================

ALTER TABLE employment_history
    ADD COLUMN updated_at TIMESTAMP(6) NULL COMMENT '마지막 수정 시각 (append-only — 사용 안 함, BaseEntity JPA 매핑 일관성용)',
    ALGORITHM = INPLACE,
    LOCK      = NONE;
