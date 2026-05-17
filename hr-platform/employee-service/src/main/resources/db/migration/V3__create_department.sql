-- =============================================================================
-- V3: department 테이블 생성
-- Materialized Path 트리 모델로 조직 구조를 표현.
-- path 컬럼 형식: '/1/12/35/' (루트 → 현재 노드 ID 순서, 양 끝 슬래시 포함)
-- 서브트리 조회: path LIKE '/1/12/%' → ALGORITHM=INPLACE 인덱스로 O(1) 처리.
-- FK 미사용 — parent_id, head_employment_id 모두 애플리케이션 레벨에서 관리.
-- 유효기간(effective_from/to)으로 폐지 부서 이력 보존.
--
-- 롤백: DROP TABLE IF EXISTS department;
-- =============================================================================

CREATE TABLE department
(
    id                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '부서 고유 식별자 (PK)',
    company_id            BIGINT        NOT NULL               COMMENT '소속 회사 식별자',
    name                  VARCHAR(100)  NOT NULL               COMMENT '부서명',
    code                  VARCHAR(50)   NOT NULL               COMMENT '부서 코드 — 회사 내 고유',
    parent_id             BIGINT            NULL               COMMENT '상위 부서 department.id 참조 (FK 미사용, 루트 부서는 NULL)',
    path                  VARCHAR(500)  NOT NULL               COMMENT 'Materialized Path — 예: /1/12/35/. 서브트리 LIKE 조회에 사용',
    head_employment_id    BIGINT            NULL               COMMENT '부서장 employment.id 참조 (FK 미사용, 부서장 없으면 NULL)',
    order_no              INT           NOT NULL DEFAULT 0     COMMENT '동일 부모 내 표시 순서 (드래그&드롭 정렬용)',
    effective_from        DATE          NOT NULL               COMMENT '부서 유효 시작일 (조직 개편 이력 추적)',
    effective_to          DATE              NULL               COMMENT '부서 유효 종료일 — 폐지된 부서는 NOT NULL, 활성 부서는 NULL',
    created_at            TIMESTAMP(6)  NOT NULL               COMMENT '레코드 최초 생성 시각 (UTC)',
    updated_at            TIMESTAMP(6)  NOT NULL               COMMENT '레코드 최종 수정 시각 (UTC)',
    PRIMARY KEY (id),
    UNIQUE KEY uq_department_company_code (company_id, code),
    INDEX idx_department_parent_id (parent_id),
    INDEX idx_department_path (path(255)),
    INDEX idx_department_head_employment_id (head_employment_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '조직 부서 — Materialized Path 트리. 유효기간으로 폐지 이력 보존.';
