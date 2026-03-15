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

---
### 2026-03-16 10:00
- **Agent:** Claude Sonnet 4.6
- **Task:** RAG 시맨틱 검색 API 완성 — GET /semantic, GET /integrated?mode=HYBRID, RRF 랭킹, 태그 포함 응답 (NAW-130)
- **Changes:**
  - `wiki-domain/.../domain/search/SemanticSearchService.kt` — 새 파일. `semanticSearch()` (pgvector 코사인 유사도 + threshold), `integratedSearch()` (KEYWORD/SEMANTIC/HYBRID 모드), `hybridSearch()` (RRF k=60), `embed()` (OpenAI RestClient), `hybridSemanticQuery()` (오버라이드 가능 pgvector 쿼리)
  - `wiki-domain/.../domain/search/SearchService.kt` — 변경 없음 (기존 LIKE 검색 유지)
  - `wiki-domain/.../infrastructure/tag/TagDocumentMappingRepository.kt` — `findByDocumentIdIn()` JPQL 쿼리 추가
  - `wiki-domain/src/main/resources/application.yml` — `spring.ai.openai.api-key/model` 추가, 중복 spring 키 수정
  - `wiki-domain/build.gradle.kts` — `mockito-kotlin:5.4.0` 테스트 의존성 추가
  - `wiki-api/.../presentation/search/SearchApiController.kt` — `GET /semantic`, `GET /integrated?mode=` 엔드포인트 추가, SemanticSearchService 주입
  - `wiki-api/.../presentation/search/response/SemanticSearchResponse.kt` — 새 파일. documentId, title, snippet, similarity, tags 응답 DTO
  - `wiki-api/build.gradle.kts` — h2, spring-security-test, mockito-kotlin 추가
  - `wiki-domain/src/test/.../search/SemanticSearchServiceTest.kt` — 8개 단위 테스트 (TC-1~TC-8)
  - `wiki-api/src/test/.../SemanticSearchIntegrationTest.kt` — 6개 통합 테스트 (TC-I-1~TC-I-6)
  - `ROADMAP.md` — N-2 BE 체크리스트 5개 항목 [x] 완료 표시
- **Decisions:**
  - `SemanticSearchService.embed()`와 `hybridSemanticQuery()`를 `open` 메서드로 노출하여 테스트에서 오버라이드 가능하게 설계 (실제 OpenAI/pgvector 없이 단위 테스트 가능)
  - RRF k=60 (표준값). HYBRID 모드는 keyword + semantic 각 최대 50건씩 fetch 후 merge
  - TagDocumentMapping 엔티티 구조상 `document.id`로 그룹핑 (`findByDocumentIdIn` JPQL)
  - 통합 테스트는 H2 + MockitoBean으로 외부 의존성 제거 (Spring Boot 4.x의 `@MockitoBean` 사용)
- **Issues:** 없음
- **Next:** FE 검색 탭 UI(NAW-116) 완료 후 E2E 연동 테스트
---
