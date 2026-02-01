---
### 2026-02-01 16:54
- **Agent:** Codex
- **Task:** 전역 태그/문서-태그 매핑 구조로 리팩터링 및 문서 저장 로직 추가
- **Changes:** wiki/requirement.md, wiki/docker/mysql/init.sql, wiki/src/main/kotlin/com/biuea/wiki/domain/document/*, wiki/src/main/kotlin/com/biuea/wiki/presentation/document/*, wiki/logs/feat-tag-refactor.md
- **Decisions:** 태그는 전역 테이블로 관리하고 문서-태그는 매핑 테이블로 분리
- **Issues:** 없음
- **Next:** 마이그레이션 스크립트 적용 및 기존 데이터 변환 필요
---
---
### 2026-02-01 16:59
- **Agent:** Codex
- **Task:** 태그 타입(tag_type) 도입 및 타입 기반 저장 로직 추가
- **Changes:** wiki/requirement.md, wiki/docker/mysql/init.sql, wiki/src/main/kotlin/com/biuea/wiki/domain/document/TagType.kt, wiki/src/main/kotlin/com/biuea/wiki/domain/document/Tag.kt, wiki/src/main/kotlin/com/biuea/wiki/domain/document/DocumentServiceCommand.kt, wiki/src/main/kotlin/com/biuea/wiki/domain/document/DocumentService.kt, wiki/src/main/kotlin/com/biuea/wiki/presentation/document/TagTypeRepository.kt, wiki/logs/feat-tag-refactor.md
- **Decisions:** 태그 타입은 별도 테이블로 관리하고 FE에서 문자열로 타입을 전달받아 저장
- **Issues:** 없음
- **Next:** FE에서 tag type 목록 조회 API 추가 고려
---
