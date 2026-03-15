# feat/NAW-105-be-auth-test-tag-api 작업 로그

---
### 2026-03-15 16:50
- **Agent:** Claude
- **Task:** Auth 통합 테스트 검증 + 태그 타입 API 구현
- **Changes:**
  - wiki-api/src/main/kotlin/com/biuea/wiki/presentation/tag/TagApiController.kt (신규)
  - wiki-api/src/main/kotlin/com/biuea/wiki/presentation/tag/response/TagApiControllerResponse.kt (신규)
  - wiki-api/src/main/kotlin/com/biuea/wiki/config/SecurityConfig.kt (태그 API permitAll 추가)
  - wiki-api/src/test/kotlin/com/biuea/wiki/integration/AuthApiScenarioIntegrationTest.kt (태그 타입 테스트 추가, GET 메서드 지원)
- **Decisions:**
  - `GET /api/v1/tags/types`는 DB 조회 없이 `TagConstant.entries` enum 값을 직접 반환. 태그 타입은 코드에서 관리하는 enum이므로 DB 조회 불필요.
  - 해당 엔드포인트는 FE 선택지 제공용이므로 인증 없이 접근 가능하도록 `permitAll()` 설정.
  - Auth 통합 테스트(AuthApiScenarioIntegrationTest)는 이미 main에 존재. 태그 타입 조회 테스트만 추가.
- **Next:** 문서 CRUD API 구현 및 통합 테스트 작성
---
