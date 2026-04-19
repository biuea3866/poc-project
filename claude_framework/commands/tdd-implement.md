---
description: 단일 티켓을 TDD 사이클로 구현 (Red→Green→Refactor)
argument-hint: <ticket id or path> [be|fe]
---

$1 티켓을 구현해줘. 스택은 ${2:-be}.

**에이전트 선택**
- `be`: `be-implementer` (Kotlin/Spring Boot, `tdd-loop` 스킬)
- `fe`: `fe-implementer` (React/Next.js, `tdd-loop` 스킬)

**절차**
1. 티켓 읽기 — Acceptance Criteria + test_cases 확인
2. feature 브랜치 생성 (main 직접 금지): `feature/<ticket-id>-<slug>`
3. `tdd-loop` 스킬의 Red→Green→Refactor 사이클
4. 하네스 룰 준수 (훅이 자동 검증, 위반 시 재작업)
5. 통합 테스트 포함
6. 커밋 (사용자 명시 요청 시에만 push)

완료 후 변경 파일 목록, 테스트 결과, 하네스 위반 여부를 보고해줘.