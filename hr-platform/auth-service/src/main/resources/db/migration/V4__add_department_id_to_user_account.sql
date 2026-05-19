-- auth-service V4: department_id 캐시 컬럼 추가 (JWT did claim용)
-- employee-service의 departmentId read-model 캐시.
-- EmployeeHired / EmployeeTransferred 이벤트 수신 시 EmployeeEventWorker가 갱신.
-- NULL 허용: 부서 미배치 직원 케이스 + 기존 데이터 하위 호환.
ALTER TABLE user_accounts
    ADD COLUMN department_id BIGINT NULL
        COMMENT 'employee.departmentId 캐시 (JWT did claim용). EmployeeTransferred 이벤트 수신 시 갱신'
        AFTER employment_id,
    ADD KEY idx_user_account_department (department_id);
