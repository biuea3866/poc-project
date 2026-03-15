# feat/NAW-112-be-document-api-controller 작업 로그

---
### 2026-03-15 00:00
- **Agent:** Claude
- **Task:** Document API Controller 전체 구현 (NAW-112)
- **Changes:**
  - `wiki-domain/src/main/kotlin/com/biuea/wiki/domain/document/DocumentServiceCommand.kt` — `UpdateDocumentCommand` 추가
  - `wiki-domain/src/main/kotlin/com/biuea/wiki/domain/document/DocumentService.kt` — 전체 CRUD + restore/publish/trash/revisions/tags/ai-status 메서드 구현, `PageResult` 래퍼 사용으로 spring-data 타입 캡슐화
  - `wiki-domain/src/main/kotlin/com/biuea/wiki/domain/common/PageResult.kt` — 새 파일. `Page<T>` → 도메인 친화적 `PageResult<T>` 래퍼 (wiki-api에서 spring-data 타입 접근 불가 문제 해결)
  - `wiki-api/src/main/kotlin/com/biuea/wiki/presentation/document/DocumentApiController.kt` — 새 파일. 11개 엔드포인트 구현
  - `wiki-api/src/main/kotlin/com/biuea/wiki/presentation/document/request/DocumentApiControllerRequest.kt` — 새 파일. CreateDocumentRequest, UpdateDocumentRequest, TagInputRequest
  - `wiki-api/src/main/kotlin/com/biuea/wiki/presentation/document/response/DocumentApiControllerResponse.kt` — 새 파일. DocumentResponse, DocumentListResponse, RevisionResponse, RevisionListResponse, TagResponse, AiStatusResponse
  - `wiki-api/src/main/kotlin/com/biuea/wiki/presentation/common/ApiExceptionHandler.kt` — IllegalArgumentException(404), IllegalStateException(400) 핸들러 추가
- **Decisions:**
  - `DocumentService`에 `TagDocumentMappingRepository`, `AiAgentLogRepository` 의존성 추가하여 controller가 직접 repository 접근하지 않도록 함 (DDD 레이어 원칙)
  - `PageResult<T>` 래퍼 도입: wiki-api 모듈이 wiki-domain의 spring-data-jpa를 `implementation` 스코프로 의존하여 `Page`, `PageRequest` 타입 접근 불가. 기존 SearchService 패턴과 동일하게 page/size int 파라미터 사용
  - SSE 스트리밍(`/ai-status/stream`)은 Kafka/SSE 인프라 미완성으로 이번 PR에서 제외
- **Issues:** wiki-worker 모듈에 기존 컴파일 에러 존재 (TaggerConsumer, TaggerService — AiProcessingFailedEvent 등 미구현 참조). 본 작업과 무관
- **Next:** SSE 엔드포인트 구현 (Kafka consumer + SseEmitter), Comment API 구현
---
