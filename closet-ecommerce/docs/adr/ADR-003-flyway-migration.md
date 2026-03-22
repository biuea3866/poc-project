# ADR-003: Flyway 마이그레이션 전략

## 상태: 승인

## 컨텍스트
DB 스키마 변경을 코드와 함께 버전 관리해야 한다.
- 로컬/개발/운영 환경별 스키마 동기화 필요
- 롤백 전략 필요
- 초기 데이터(시드) 관리 방안 필요

## 결정
Flyway를 DB 마이그레이션 도구로 사용하며, 환경별 전략을 분리한다.
- 마이그레이션 파일 위치: `src/main/resources/db/migration/`
- 네이밍: `V{yyyyMMdd}_{순번}__{설명}.sql` (예: `V20260322_001__create_member_table.sql`)
- 환경별 시드: `src/main/resources/db/seed/` (Spring Profile로 분기)
- 롤백: 별도의 롤백 SQL을 `db/rollback/`에 준비 (Flyway Undo 미사용)

## 이유
- 스키마 변경 이력을 Git으로 추적 가능
- 환경 간 스키마 불일치 문제 해소
- Spring Boot와의 자연스러운 통합 (자동 실행)
- 타임스탬프 기반 네이밍으로 브랜치 병합 시 충돌 최소화

## 결과
- 모든 DDL 변경은 반드시 Flyway 마이그레이션 파일로 작성
- JPA `ddl-auto`는 `validate`로 설정 (자동 생성 금지)
- 로컬 개발 환경에서도 Flyway 적용 (H2/Testcontainers MySQL)
- 롤백 시 새로운 마이그레이션 파일로 변경 사항 되돌리기
- 데이터 마이그레이션(DML)도 Flyway로 관리

## 대안 (검토했으나 선택하지 않은 것)

### Liquibase
- 장점: XML/YAML/JSON 다양한 포맷, 자동 롤백 지원
- 단점: 설정 복잡, Flyway 대비 무거움
- 기각 사유: SQL 기반 마이그레이션의 단순성이 학습 프로젝트에 적합

### JPA ddl-auto (create/update)
- 장점: 설정 불필요, 자동 스키마 관리
- 단점: 운영 환경 사용 불가, 데이터 손실 위험, 세밀한 제어 불가
- 기각 사유: 운영 환경에서의 안전성 확보 불가

### 수동 SQL 관리
- 장점: 가장 단순
- 단점: 이력 추적 불가, 환경 간 불일치 위험
- 기각 사유: 스키마 변경 추적 및 자동화 불가
