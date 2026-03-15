# feat/NAW-118-be-domain-encapsulation 작업 로그

---
### 2026-03-15 17:35
- **Agent:** Claude
- **Task:** PR #44 코드 리뷰 피드백 반영 — 도메인 로직 캡슐화 + 캐시 테스트 추가
- **Changes:**
  - `wiki-domain/.../document/entity/Document.kt` — `softDelete()` (자식 재귀 삭제 포함), `restore()` (삭제 상태 검증 + 삭제된 부모 분리), `publish()`, `update(title, content, updatedBy)` 엔티티 메서드 추가
  - `wiki-domain/.../document/DocumentService.kt` — 서비스에서 직접 상태 변경하던 로직을 엔티티 메서드 호출로 리팩토링
  - `wiki-domain/.../document/entity/DocumentTest.kt` — 새 파일. 엔티티 상태 전이 단위 테스트 9개 (softDelete, restore, publish, update 등)
  - `wiki-api/.../integration/DocumentApiIntegrationTest.kt` — 새 파일. Document API 통합 테스트 7개 (CRUD, trash/restore, revisions, cache eviction, pagination, tags, ai-status)
- **Decisions:**
  - `delete()` → `softDelete()`로 리네이밍: 자식 재귀 삭제까지 엔티티가 책임지도록 (서비스에서 `children.forEach` 제거)
  - `restore()`에 `check(isDeleted())` 가드 추가: 비삭제 상태 문서 복구 시도 방지
  - 캐시 테스트는 Testcontainers 기반 통합 테스트로 구현 (CacheManager 직접 주입하여 eviction 검증)
- **Issues:** 없음
- **Next:** NAW-114 (RAG 벡터 검색 BE API) 또는 다음 배분 대기
---
