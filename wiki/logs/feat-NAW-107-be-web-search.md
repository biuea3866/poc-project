# feat/NAW-107-be-web-search 작업 로그

---
### 2026-03-15 17:00
- **Agent:** Claude
- **Task:** 웹 검색 + 통합 검색 API 구현
- **Changes:**
  - wiki-domain/src/main/kotlin/com/biuea/wiki/domain/search/SearchService.kt (신규)
  - wiki-domain/src/main/kotlin/com/biuea/wiki/infrastructure/document/DocumentRepository.kt (LIKE 검색 쿼리 추가)
  - wiki-api/src/main/kotlin/com/biuea/wiki/presentation/search/SearchApiController.kt (신규)
  - wiki-api/src/main/kotlin/com/biuea/wiki/presentation/search/response/SearchApiControllerResponse.kt (신규)
  - wiki/logs/feat-NAW-107-be-web-search.md (신규)
- **Decisions:**
  - `GET /api/v1/search/web`은 외부 API 미연동 상태이므로 Mock 응답 반환. 추후 외부 검색 API 연동 시 SearchService.searchWeb() 교체.
  - `GET /api/v1/search/integrated`도 함께 구현 — LIKE 검색 기반, 동일 SearchApiController에 배치.
  - wiki-api 모듈에서 spring-data 의존성 직접 사용 불가(implementation 스코프)하므로, PageRequest 생성을 SearchService(domain 레이어)로 위임.
  - 검색 API는 인증 필요(authenticated). permitAll 추가하지 않음.
- **Next:** 외부 웹 검색 API 연동 (Serper, Google Custom Search 등), RAG 벡터 검색 통합
---
