-- =============================================================================
-- V4: employment_history 테이블 생성
-- 모든 발령 변경 이력을 append-only 로그로 보존.
-- UPDATE 금지 — INSERT만 허용 (불변성 보장, audit 5년 보관).
-- old_value/new_value: 이벤트 유형별 부분 스냅샷 직렬화 문자열 (전체 Employment 직렬화 금지).
--   예) 부서 이동: {"departmentId": 12} → {"departmentId": 35}
-- cancelled_at: 발령 취소 보상 이벤트 처리 시 기록 (ADR-002 §4 발령 취소 보상).
--   직전 1건만 취소 가능. 취소 시 새 행 append 대신 직전 이력에 cancelledAt을 기록.
--
-- 롤백: DROP TABLE IF EXISTS employment_history;
-- =============================================================================

CREATE TABLE employment_history
(
    id                        BIGINT        NOT NULL AUTO_INCREMENT COMMENT '발령 이력 고유 식별자 (PK)',
    employment_id             BIGINT        NOT NULL               COMMENT 'employment.id 참조 (FK 미사용)',
    event_type                VARCHAR(30)   NOT NULL               COMMENT '발령 이벤트 유형 — HIRE / PROMOTION / DEPT_CHANGE / SALARY_CHANGE / SUSPEND / RESUME / RESIGN',
    old_value                 TEXT              NULL               COMMENT '변경 전 부분 스냅샷 직렬화 문자열 — 이벤트 유형별 관련 필드만 저장. 애플리케이션에서 JsonStringType으로 처리',
    new_value                 TEXT          NOT NULL               COMMENT '변경 후 부분 스냅샷 직렬화 문자열 — 이벤트 유형별 관련 필드만 저장. 애플리케이션에서 JsonStringType으로 처리',
    effective_date            DATE          NOT NULL               COMMENT '발령 효력 발생일',
    created_by_employment_id  BIGINT            NULL               COMMENT '발령을 등록한 HR 담당자 employment.id (FK 미사용, 시스템 처리 시 NULL)',
    note                      VARCHAR(500)      NULL               COMMENT '발령 메모 — 취소 사유, 특이 사항 등',
    cancelled_at              TIMESTAMP(6)      NULL               COMMENT '발령 취소 시각 — 직전 1건만 취소 허용. 취소된 이력은 NULL이 아님 (ADR-002 §4 보상 이벤트)',
    created_at                TIMESTAMP(6)  NOT NULL               COMMENT '레코드 생성 시각 (UTC) — INSERT만 허용, UPDATE 금지',
    PRIMARY KEY (id),
    INDEX idx_employment_history_employment_effective (employment_id, effective_date DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '발령 이력 — append-only. 모든 Employment 변경(채용/승진/부서이동/연봉변경/휴직/복직/퇴사)을 기록. audit 5년 보관.';
