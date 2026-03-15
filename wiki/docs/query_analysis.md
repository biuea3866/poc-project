# DBA Query Analysis Report

## 발견된 문제 목록

| # | 분류 | 심각도 | 위치 | 요약 |
|---|------|--------|------|------|
| 1 | N+1 | HIGH | Document.kt:72 | `latestRevisionId`가 전체 revisions 컬렉션 로드 |
| 2 | N+1 | MEDIUM | DocumentRevisionData.from() | LAZY 엔티티(`parent`, `children`)를 JSON 직렬화 시 로드 |
| 3 | N+1 | MEDIUM | TagService.saveTags() | 루프 내 `findByTagConstant`, `findByNameAndTagType` 반복 호출 |
| 4 | N+1 | LOW | SaveDocumentOutput.of() | `tag.tagType.tagConstant` LAZY 접근 (같은 트랜잭션이라 1차 캐시 히트 가능) |
| 5 | 인덱스 | HIGH | 7개 테이블 전체 | FK 컬럼 및 WHERE 조건 컬럼에 인덱스 누락 |
| 6 | Cascade | HIGH | Document.agentLogs, DocumentRevision.agentLogs | 감사 로그에 orphanRemoval=true → 의도치 않은 삭제 위험 |
| 7 | 비효율 | LOW | DocumentRevisionData | Entity 참조를 JSON에 저장 → 순환 참조 및 불필요한 LAZY 트리거 |

---

## 1. N+1 문제

### 1-1. Document.latestRevisionId (HIGH)

**문제:** `Document.kt:72`의 `val latestRevisionId get() = this.revisions.last().id`가 전체 `revisions` LAZY 컬렉션을 로드하여 단 하나의 ID만 가져옴. `SaveDocumentFacade`에서 호출됨.

**권장:** `DocumentRevisionRepository.findTopByDocumentIdOrderByCreatedAtDesc()`를 사용하거나, `DocumentService.saveDocument()`에서 revision을 직접 반환하여 컬렉션 접근 회피.

**현재 상태:** `SaveDocumentFacade`에서는 `documentService.saveDocument()` 직후 같은 트랜잭션 내에서 접근하므로 1차 캐시에 있어 즉시 문제는 아니지만, 다른 컨텍스트에서 호출 시 full scan 발생.

### 1-2. DocumentRevisionData.from() — LAZY 엔티티 직렬화 (HIGH)

**문제:** `DocumentRevisionData`가 `parent: Document?`와 `children: List<Document>`를 필드로 가짐. JSON 직렬화 시 LAZY 프록시 로드 → N+1 + 순환 참조 위험.

**수정:** `parentId: Long?`와 `childrenIds: List<Long>`으로 변경하여 엔티티 참조 대신 ID만 저장.

### 1-3. TagService.saveTags() 루프 내 쿼리 (MEDIUM)

**문제:** `command.tags.map { }` 루프 내에서:
- `tagTypeRepository.findByTagConstant()` — 태그당 1회
- `tagRepository.findByNameAndTagType()` — 태그당 1회
- `tagRepository.save()` — 태그당 1회

태그 N개 → 최소 3N개 쿼리.

**권장:** 배치 조회로 개선:
```kotlin
// TagConstant 목록으로 한 번에 조회
val tagTypes = tagTypeRepository.findByTagConstantIn(tagConstants)
// 이름+타입 조합으로 한 번에 조회
val existingTags = tagRepository.findByNameAndTagTypeIn(names, tagTypes)
```

---

## 2. 누락된 인덱스

### 수정 완료: 총 17개 인덱스 추가

| 테이블 | 인덱스 | 커버하는 쿼리 |
|--------|--------|---------------|
| `document` | `idx_document_parent_id_deleted_at` (parent_id, deleted_at) | `findByParentIdAndDeletedAtIsNull` |
| `document` | `idx_document_created_by_deleted_at` (created_by, deleted_at) | `findByCreatedByAndDeletedAtIsNull` |
| `document` | `idx_document_deleted_at` (deleted_at) | `findByIdAndDeletedAtIsNull`, soft-delete 필터 |
| `document_revision` | `idx_doc_revision_document_id` (document_id) | FK 조인, `findByDocumentId` |
| `document_revision` | `idx_doc_revision_document_id_created_at` (document_id, created_at DESC) | `findTopByDocumentIdOrderByCreatedAtDesc` |
| `document_summary` | `idx_doc_summary_document_id_revision_id` (document_id, document_revision_id) | `findByDocumentIdAndDocumentRevisionId` |
| `document_summary` | `idx_doc_summary_document_id` (document_id) | `findTopByDocumentIdOrderByIdDesc` |
| `tag` | `idx_tag_name_tag_type_id` (name, tag_type_id) | `findByNameAndTagType` |
| `tag` | `idx_tag_tag_type_id` (tag_type_id) | FK 조인 |
| `tag_document_mapping` | `idx_tag_doc_mapping_tag_id` (tag_id) | FK 조인 |
| `tag_document_mapping` | `idx_tag_doc_mapping_document_id` (document_id) | FK 조인 |
| `tag_document_mapping` | `idx_tag_doc_mapping_document_revision_id` (document_revision_id) | FK 조인 |
| `ai_agent_log` | `idx_ai_agent_log_document_id` (document_id) | FK 조인 |
| `ai_agent_log` | `idx_ai_agent_log_document_revision_id` (document_revision_id) | FK 조인 |
| `ai_agent_log` | `idx_ai_agent_log_executor_id` (executor_id) | 실행자별 로그 조회 |
| `tag_type` | `idx_tag_type_tag_constant` (tag_type) | `findByTagConstant`, `existsByTagConstant` |
| `user` | `idx_user_email_deleted_at` (email, deleted_at) | `findByEmailAndDeletedAtIsNull` |
| `user` | `idx_user_deleted_at` (deleted_at) | `findByIdAndDeletedAtIsNull` |

---

## 3. 비효율적 쿼리

### 3-1. UserService.signUp() — soft-delete 미고려

**문제:** `userRepository.findByEmail(normalizedEmail)`은 soft-delete된 유저도 찾음. 이전에 탈퇴한 이메일로 재가입 불가.

**권장:** `findByEmailAndDeletedAtIsNull`로 변경 검토 (비즈니스 요구사항에 따라 결정).

---

## 4. Cascade / orphanRemoval 위험

### 4-1. Document.agentLogs — orphanRemoval 제거 (수정 완료)

**문제:** `CascadeType.ALL + orphanRemoval = true`가 감사/이력 성격의 AI Agent Log에 적용됨. Document에서 agentLog를 리스트에서 제거하면 DB에서 물리 삭제됨.

**수정:** `cascade = [CascadeType.PERSIST, CascadeType.MERGE]`로 변경, `orphanRemoval` 제거.

### 4-2. DocumentRevision.agentLogs — 동일 문제 (수정 완료)

**수정:** 동일하게 `PERSIST, MERGE`로 변경.

### 4-3. Document.children — orphanRemoval + soft-delete 충돌 (주의)

**문제:** `children`에 `orphanRemoval = true`가 있어, 자식 Document를 리스트에서 제거하면 물리 삭제됨. 이는 soft-delete 패턴과 충돌.

**현재 상태:** 미수정 — 자식 문서 이동(reparenting) 기능 구현 시 반드시 검토 필요. 현재 코드에서는 children에서 remove하는 로직이 없어 즉시 위험은 낮음.

---

## 수정 완료 파일 목록

| 파일 | 수정 내용 |
|------|-----------|
| `domain/document/entity/Document.kt` | 인덱스 3개 추가, agentLogs cascade 변경 |
| `domain/document/entity/DocumentRevision.kt` | 인덱스 2개 추가, agentLogs cascade 변경, `DocumentRevisionData` 엔티티→ID 참조로 변경 |
| `domain/document/entity/DocumentSummary.kt` | 인덱스 2개 추가 |
| `domain/tag/entity/Tag.kt` | 인덱스 2개 추가 |
| `domain/tag/entity/TagDocumentMapping.kt` | 인덱스 3개 추가 |
| `domain/tag/entity/TagType.kt` | 인덱스 1개 추가 |
| `domain/ai/AiAgentLog.kt` | 인덱스 3개 추가 |
| `domain/user/User.kt` | 인덱스 2개 추가 |
