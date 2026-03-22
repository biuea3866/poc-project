# ADR-002: JPA + QueryDSL 선택

## 상태: 승인

## 컨텍스트
이커머스 도메인은 복잡한 연관관계와 다양한 조회 조건을 가진다. ORM 기술 선택이 필요하다.
- 상품 검색: 카테고리, 가격 범위, 상태 등 다양한 필터링
- 주문 조회: 기간, 상태, 회원별 등 복합 조건
- 도메인 객체의 영속성 관리 전략 필요

## 결정
JPA(Hibernate)를 기본 ORM으로 사용하고, 복잡한 동적 쿼리에는 QueryDSL을 적용한다.
- 엔티티 매핑: JPA 표준 어노테이션
- CRUD: Spring Data JPA Repository
- 동적 쿼리: QueryDSL BooleanBuilder / BooleanExpression
- 통계/집계: QueryDSL Projections (DTO 직접 매핑)

## 이유
- JPA는 도메인 모델과 DB 스키마 간 매핑을 자연스럽게 표현
- 1차 캐시, 변경 감지(Dirty Checking) 등 생산성 이점
- QueryDSL은 컴파일 타임 타입 체크로 쿼리 오류를 사전에 방지
- 코틀린과의 궁합이 좋음 (null safety 활용 가능)

## 결과
- 모든 엔티티는 JPA `@Entity`로 매핑
- Repository는 Spring Data JPA + QueryDSL Custom Repository 패턴
- `{Entity}RepositoryCustom` 인터페이스 + `{Entity}RepositoryImpl` 구현
- Projection DTO를 사용한 조회 최적화
- N+1 문제는 `@EntityGraph` 또는 `fetchJoin`으로 해결

## 대안 (검토했으나 선택하지 않은 것)

### MyBatis
- 장점: SQL 직접 작성, 세밀한 성능 튜닝
- 단점: 도메인 모델과 SQL 분리, XML 관리 부담, 타입 안전성 부족
- 기각 사유: 도메인 중심 설계에서 SQL 중심 접근은 생산성 저하

### jOOQ
- 장점: 타입 세이프 SQL, DB 스키마 기반 코드 생성
- 단점: JPA 대비 객체 매핑 편의성 부족, 러닝 커브
- 기각 사유: JPA + QueryDSL 조합이 코틀린 생태계에서 더 풍부한 레퍼런스 보유

### Spring JDBC Template
- 장점: 가장 단순, SQL 직접 제어
- 단점: 보일러플레이트 코드 다량 발생, ORM 편의 기능 부재
- 기각 사유: 4개 도메인의 복잡한 연관관계를 수동 매핑하기에는 비효율적
