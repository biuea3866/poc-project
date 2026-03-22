# ADR-007: Soft Delete 전략

## 상태: 승인

## 컨텍스트
이커머스 데이터는 법적/비즈니스 이유로 물리 삭제가 적합하지 않은 경우가 많다.
- 주문/결제 이력: 법적 보관 의무
- 회원 탈퇴: 개인정보 처리 방침에 따른 보관
- 상품 삭제: 기존 주문과의 참조 무결성 유지
- 실수로 인한 데이터 복구 가능성 확보

## 결정
`deletedAt` 컬럼을 사용한 Soft Delete를 기본 전략으로 채택하고, JPA `@SQLRestriction`으로 자동 필터링한다.

- 모든 엔티티의 공통 필드: `deletedAt: LocalDateTime?` (nullable, null이면 활성)
- `BaseEntity`에 `deletedAt` 포함, `@SQLRestriction("deleted_at IS NULL")` 적용
- 삭제 시 `DELETE` 대신 `UPDATE SET deleted_at = NOW(6)` 수행
- 삭제된 데이터 조회 필요 시 Native Query 또는 별도 Repository 사용

## 이유
- 데이터 복구 가능 (실수 방지)
- 참조 무결성 유지 (FK 없이도 ID 참조 데이터가 유지됨)
- 삭제 이력 추적 가능
- `@SQLRestriction`으로 개발자가 삭제 여부를 매번 체크할 필요 없음

## 결과
- `BaseEntity`: `id`, `createdAt`, `updatedAt`, `deletedAt` 4개 공통 필드
- 모든 JPA 쿼리에 `deleted_at IS NULL` 조건 자동 적용
- `deletedAt IS NOT NULL` 데이터는 별도 배치로 일정 기간 후 물리 삭제 (Phase 2)
- 인덱스: `deleted_at` 포함 복합 인덱스 고려 (성능 영향 모니터링)
- `@SQLRestriction`은 Hibernate 6.3+ 기능 (구 `@Where` 대체)

## 대안 (검토했으나 선택하지 않은 것)

### 물리 삭제 (Hard Delete)
- 장점: 단순, 저장 공간 효율
- 단점: 복구 불가, 참조 무결성 깨짐, 법적 보관 의무 불이행
- 기각 사유: 이커머스 도메인 특성상 데이터 보존이 필수

### Boolean 플래그 (isDeleted)
- 장점: 단순한 true/false 체크
- 단점: 삭제 시점을 알 수 없음, 감사 추적 불가
- 기각 사유: `deletedAt`이 삭제 시점까지 기록하므로 상위 호환

### 이력 테이블 분리 (Archive Table)
- 장점: 원본 테이블 성능 유지, 깔끔한 분리
- 단점: 구현 복잡, 아카이브 테이블 관리 부담
- 기각 사유: 현 규모에서 과도한 복잡성, Phase 2 이후 검토
