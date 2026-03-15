# feat/NAW-114-be-rag-vector-search 작업 로그

---
### 2026-03-15 18:00
- **Agent:** Claude
- **Task:** RAG 벡터 검색 BE API 구현 (NAW-114)
- **Changes:**
  - `wiki-domain/build.gradle.kts` — `spring-boot-starter-data-jdbc` + `postgresql` 의존성 추가
  - `wiki-domain/.../infrastructure/config/VectorDataSourceConfig.kt` — 새 파일. PostgreSQL 벡터 DB 전용 DataSource + JdbcClient 빈 설정
  - `wiki-domain/.../domain/search/VectorSearchService.kt` — 새 파일. pgvector 코사인 유사도 검색 (`searchByVector`) + 텍스트 폴백 검색 (`searchByText`)
  - `wiki-domain/.../resources/application.yml` — `datasource-vector` 설정 추가 (PostgreSQL 연결 정보)
  - `wiki-api/.../presentation/search/SearchApiController.kt` — `POST /api/v1/search/vector` 엔드포인트 추가
  - `wiki-api/.../presentation/search/request/VectorSearchRequest.kt` — 새 파일. embedding(float[]) 또는 query(string) + limit
  - `wiki-api/.../presentation/search/response/VectorSearchResultResponse.kt` — 새 파일. documentId, chunkContent, similarity 응답
  - `requirement.md` — API 명세에 `POST /api/v1/search/vector` 추가
- **Decisions:**
  - `wiki-domain` 모듈에 PostgreSQL 이중 DataSource 구성: 기존 MySQL(JPA) + 신규 PostgreSQL(JDBC). `@Qualifier("vectorJdbcClient")`로 구분
  - `VectorSearchService`는 JdbcClient 기반 raw SQL 사용 (pgvector 확장 함수 `<=>` 코사인 거리 연산자)
  - embedding 미전달 시 텍스트 ILIKE 폴백 검색 지원 (프론트엔드에서 임베딩 없이도 호출 가능)
  - `Environment.getRequiredProperty()` 사용으로 벡터 DB 설정 누락 시 빠른 실패
- **Issues:** 없음
- **Next:** 프론트엔드에서 임베딩 생성 후 호출하는 플로우 연동, 또는 서버사이드 임베딩 생성 엔드포인트 추가 검토
---
