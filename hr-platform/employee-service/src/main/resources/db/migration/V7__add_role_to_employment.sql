-- =============================================================================
-- V7: employment 테이블에 role 컬럼 추가 (BE-05)
-- EmployeeQueryDomainService 권한 범위 필터링에 사용.
-- EMPLOYEE / TEAM_LEAD / HR_MANAGER / ADMIN 4종.
-- 기본값: EMPLOYEE
--
-- 롤백: ALTER TABLE employment DROP COLUMN role;
-- =============================================================================

ALTER TABLE employment
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'EMPLOYEE'
        COMMENT '권한 역할 — EMPLOYEE / TEAM_LEAD / HR_MANAGER / ADMIN'
        AFTER status;
