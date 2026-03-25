# 구현 파이프라인

> 티켓 → 코드 생성 → 컴파일 검증 → 테스트 실행 → PR 생성

---

## 실행 원칙

- **입력**: be-implementation 파이프라인이 산출한 티켓 (ticket-XX.md)
- **출력**: 실제 코드 + 통과된 테스트 + PR
- **Phase 1~4 전체를 순서대로 수행**. 각 Phase 내 깊이는 티켓 크기에 맞게 조절.

---

## 파이프라인 흐름

```
[INPUT] 티켓 (ticket-XX.md)
    │
    ▼
━━ Phase 1: 티켓 분석 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    │  티켓의 작업 내용, 코드 예시, AC를 파싱
    │  선행 티켓 완료 여부 확인
    │  수정 대상 파일 목록 확인
    │
    │  출력: 구현 계획 (어떤 파일을 어떤 순서로 작성할지)
    │
    ▼
━━ Phase 2: 코드 생성 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    │  티켓의 코드 예시를 기반으로 실제 코드 작성
    │  기존 코드베이스 패턴을 따름
    │
    │  규칙:
    │    - 티켓에 정의된 파일 목록만 생성/수정
    │    - 기존 코드 스타일/패턴 준수
    │    - 설계 원칙 (엔티티 캡슐화, Service 얇게 등) 준수
    │    - import 정리, 불필요한 코드 없음
    │
    ▼
━━ Phase 3: 컴파일 + 테스트 검증 ━━━━━━━━━━━━━━━━━━━━━━
    │  1. 컴파일 확인 (./gradlew compileKotlin 또는 compileTestKotlin)
    │  2. 기존 테스트 깨짐 없음 확인 (./gradlew test)
    │  3. 신규 테스트 작성 + 통과 확인
    │  4. 실패 시 → 수정 → 재검증 (최대 3회)
    │
    │  출력: 빌드 성공 로그, 테스트 결과
    │
    ▼
━━ Phase 4: AC 검증 + PR 생성 ━━━━━━━━━━━━━━━━━━━━━━━━━
    │  1. 티켓 AC 체크리스트 대조 (전부 충족하는지)
    │  2. git add + commit (커밋 메시지: 티켓 번호 + 제목)
    │  3. PR 생성 (제목, 본문에 티켓 참조)
    │
    │  출력: PR URL
    │
    ▼
[OUTPUT]
  ├── 구현된 코드 (커밋)
  ├── 테스트 코드 (커밋)
  ├── PR (리뷰 대기)
  └── 구현 결과 리포트 (results/)
```

---

## Phase별 상세

### Phase 1: 티켓 분석

```
1. 티켓 파일 읽기
2. "선행 티켓" 확인 → 해당 코드가 이미 존재하는지 검증
3. "수정 파일 목록" 파싱 → 대상 레포/모듈/경로 확인
4. "코드 예시" 파싱 → 구현 참조 코드 추출
5. "AC" 파싱 → 완료 기준 체크리스트 추출
6. 구현 순서 결정:
   - 엔티티/enum 먼저
   - Repository 다음
   - Service 다음
   - Controller/Facade 마지막
   - 테스트는 각 단계마다 함께
```

### Phase 2: 코드 생성 규칙

**생성 전 반드시 확인:**
- 대상 레포의 기존 패키지 구조
- 기존 import 패턴 (패키지명, 별칭)
- 기존 코드 스타일 (들여쓰기, 네이밍)
- 기존 테스트 패턴 (Kotest BehaviorSpec, 픽스처)

**코드 작성 순서:**
```
1. DDL/Flyway (greeting-db-schema) → DB 먼저
2. 엔티티 + enum → 도메인 모델
3. Repository → 데이터 접근
4. Service → 비즈니스 로직
5. Controller/Facade → API
6. 테스트 → 각 단계마다
7. 설정 (application.yml, Config) → 마지막
```

**Worktree 사용:**
```bash
# 구현 전 worktree 생성
bin/worktree-create.sh {repo} {branch}

# worktree에서 작업
cd .worktrees/{repo}/{branch}

# 완료 후 PR 생성
```

### Phase 3: 검증 체크리스트

| # | 검증 항목 | 명령어 | 통과 기준 |
|---|----------|--------|----------|
| 1 | 컴파일 | `./gradlew compileKotlin` | BUILD SUCCESSFUL |
| 2 | 테스트 컴파일 | `./gradlew compileTestKotlin` | BUILD SUCCESSFUL |
| 3 | 기존 테스트 | `./gradlew test` | 기존 테스트 전부 통과 |
| 4 | 신규 테스트 | `./gradlew test --tests "패키지.*"` | 신규 테스트 통과 |
| 5 | 린트 (해당 시) | `./gradlew ktlintCheck` | 위반 0건 |

**실패 시 대응:**
```
컴파일 실패 → 에러 메시지 분석 → 코드 수정 → 재컴파일 (최대 3회)
테스트 실패 → 실패 테스트 분석 → 코드 또는 테스트 수정 → 재실행 (최대 3회)
3회 실패 → 사용자에게 보고 (자동 수정 중단)
```

### Phase 4: PR 생성

**커밋 메시지:**
```
[#{티켓번호}] {티켓 제목}

- {변경 사항 요약}
- {테스트 추가/수정 내역}

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
```

**PR 본문:**
```markdown
## Summary
- 티켓: #{번호} {제목}
- TDD 참조: {섹션}

## Changes
- {파일별 변경 내용}

## Test
- {테스트 실행 결과}
- {커버리지 (해당 시)}

## AC Checklist
- [x] {AC 1}
- [x] {AC 2}
```

---

## 구현 결과 리포트

구현 완료 후 결과를 `.analysis/implementation/results/` 에 저장:

```
results/{날짜}_{티켓번호}/
├── implementation_report.md    ← 구현 결과 요약
├── build_log.txt               ← 빌드 로그
├── test_result.txt             ← 테스트 결과
└── pr_url.txt                  ← PR URL
```

---

## 출력 어조

산출물은 **팀원과 공유하는 문서**입니다. 읽는 사람이 지치지 않도록 아래 원칙을 따릅니다.

- **핵심부터** — 결론·액션 아이템을 앞에 씁니다.
- **짧게** — 한 문장으로 쓸 수 있으면 세 문장으로 쓰지 않습니다.
- **구체적으로** — "여러 곳" 대신 "3곳" 처럼 씁니다.
