---
name: tdd-loop
description: Red→Green→Refactor 사이클을 강제하는 TDD 실행 절차. 테스트 먼저 작성하고 실패 확인 후 최소 구현, 그 다음 통합 테스트 추가. Kotlin/Spring Boot는 Kotest+Testcontainers, FE는 Vitest/Playwright 사용. be-implementer/fe-implementer 에이전트가 참조한다.
---

# TDD Loop Skill

## 언제 사용하나
- 새 기능 구현 또는 버그 수정
- "구현해줘", "만들어줘", "고쳐줘" 요청

## 절대 원칙
1. **테스트 먼저** — 어떤 상황에서도 구현 먼저 쓰지 않는다
2. **Red 확인** — 테스트 실행해서 실패 메시지 확인한 뒤에 구현 시작
3. **최소 구현** — 테스트 통과에 딱 필요한 만큼만
4. **Refactor는 Green 이후** — 리팩토링하다 깨지면 즉시 롤백
5. **통합 테스트 필수** — Service/UseCase는 Testcontainers로 실제 인프라 검증

## 사이클

### Red
1. TC 번호 확인 (`02-tdd.md`에서)
2. 테스트 파일 생성 — 네이밍: `<클래스명>Test.kt` (Kotest) / `<component>.test.tsx` (Vitest)
3. Given/When/Then 구조로 테스트 작성
4. 실행 → 실패 확인 (컴파일 에러도 Red의 일부)

### Green
1. 최소 구현 작성 — 하드코딩/리턴 상수도 허용 (여러 TC 추가되며 일반화)
2. 테스트 통과 확인
3. 다른 테스트 깨지지 않았는지 전체 실행

### Refactor
1. 중복 제거
2. 네이밍 개선 (풀네임 강제: `workspaceId` ✓, `ws` ✗)
3. 엔티티에 비즈니스 로직 캡슐화, Service 얇게
4. 전체 테스트 재실행 → 모두 통과해야 커밋 가능

## 스택별 세부

### Kotlin / Spring Boot
- Kotest BehaviorSpec (Given/When/Then)
- MockK (단위), Testcontainers (통합: MySQL, Redis, Kafka)
- `BaseIntegrationTest` 상속
- QueryDSL + CustomRepository+Impl (`@Query` 금지)
- `ZonedDateTime` 사용 (`LocalDateTime` 금지)
- `@Transactional`은 UseCase/DomainService에만

### Node / TypeScript
- Vitest 또는 Jest
- Component: Testing Library + `render` + `userEvent`
- API: supertest (Integration)
- E2E: Playwright (Page Object Model)
- `any` 금지, Zod로 API 스키마 검증

## 하네스 통합
- 모든 Write/Edit는 `.claude/harness-check.py`가 자동 검증 (PreToolUse 훅)
- 금지 패턴에 걸리면 실패 — 구현 방향 자체를 재검토
- pre-commit 훅이 커밋 전 한번 더 검증

## 커밋 규칙
- 사용자가 명시적으로 요청하기 전까지 **자동 커밋 금지**
- 커밋 단위: 1 TC 또는 1 Refactor 스텝
- 메시지 형식: `test: <TC-번호> <제목>` / `feat: <구현 요약>` / `refactor: <의도>`

## 완료 체크
- [ ] 모든 TC Green
- [ ] 통합 테스트 ≥1 포함
- [ ] 커버리지 목표 달성 (일반 80% / 핵심 95%)
- [ ] 하네스 룰 위반 0
- [ ] 불필요한 주석/죽은 코드 제거