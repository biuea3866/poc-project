# Closet Harness Engineering System

Closet 프로젝트의 코드 품질, 워크플로우, 파이프라인을 **시스템적으로 강제**하는 하네스 엔지니어링 프레임워크.

## 아키텍처

```
.claude/
├── harness-rules.json        ← 모든 규칙 중앙 정의
├── harness-check.py          ← 실시간 규칙 검증 (Claude Hook)
├── harness-workflow.py       ← 워크플로우 상태 머신
├── harness-gc.py             ← GC 스캐너 (코드베이스 전체 스캔)
├── harness-gc-runner.py      ← GC 파이프라인 (워커/리뷰어 루프)
├── pipeline-runner.py        ← 파이프라인 프레임워크 러너
├── git-hooks/
│   └── pre-commit            ← Git pre-commit 훅
├── workflow-state.json       ← 워크플로우 상태 (자동 관리)
├── gc-state.json             ← GC 상태 (자동 관리)
├── gc-report.json            ← GC 스캔 리포트
├── pipeline-state.json       ← 파이프라인 상태 (자동 관리)
└── pipeline-history.json     ← 파이프라인 실행 기록
```

## 3계층 강제 시스템

| 계층 | 시점 | 도구 | 역할 |
|------|------|------|------|
| **Claude Hook** | Write/Edit/Bash 전/후 | `harness-check.py`, `harness-workflow.py` | 실시간 차단 + 상태 추적 |
| **Git pre-commit** | `git commit` 시 | `git-hooks/pre-commit` | 패턴 검출 + ktlint + main 차단 |
| **GC 파이프라인** | 수동/주기적 | `harness-gc.py`, `harness-gc-runner.py` | 전체 스캔 + 자동 리팩토링 루프 |

## 1. 규칙 시스템 (`harness-rules.json`)

모든 규칙은 이 파일 하나에 정의된다. 규칙을 추가/수정하려면 이 파일만 편집하면 된다.

### 현재 적용 규칙 (12개)

| ID | 차단 대상 | 적용 파일 | 자동수정 |
|---|---|---|---|
| `no-jpa-query` | `@Query()` → QueryDSL | `*.kt` | - |
| `no-consumer-record` | `ConsumerRecord<String, String>` → DTO | `*.kt` | - |
| `no-local-datetime` | `LocalDateTime` → `ZonedDateTime` | `*.kt` | O |
| `no-fqcn` | FQCN 인라인 → import | `*.kt` | - |
| `no-objectmapper-consumer` | Consumer 내 ObjectMapper | `*Consumer*.kt` | - |
| `no-repository-in-consumer` | Consumer 내 Repository 직접호출 | `*Consumer*.kt` | - |
| `no-db-fk` | SQL `FOREIGN KEY` | `*.sql` | - |
| `no-db-enum` | SQL `ENUM()` | `*.sql` | - |
| `no-db-json` | SQL `JSON` | `*.sql` | - |
| `no-db-boolean-type` | `BOOLEAN` → `TINYINT(1)` | `*.sql` | O |
| `no-db-datetime-no-precision` | `DATETIME` → `DATETIME(6)` | `*.sql` | O |
| `no-kafka-topic-hardcode` | 토픽 하드코딩 → `ClosetTopics` | `*.kt` | - |

### 규칙 추가 방법

`harness-rules.json`의 `forbidden_patterns.rules` 배열에 추가:

```json
{
  "id": "no-xxx",
  "pattern": "정규식 패턴",
  "file_glob": "*.kt",
  "exclude_glob": "**/test/**",
  "message": "차단 메시지",
  "severity": "error"
}
```

## 2. 워크플로우 상태 머신 (`harness-workflow.py`)

5단계 개발 플로우를 시스템적으로 강제한다.

```
idle → ticket → testing → implementing → reviewing → approved → PR/merge → idle
```

### 강제 규칙

| Phase | 허용 | 차단 | 전환 조건 |
|---|---|---|---|
| `idle` | 설정/문서 파일 | 소스 코드 전부 | 노션 티켓 등록 |
| `ticket` | 테스트 파일 | 구현 소스 코드 | 테스트 파일 작성 |
| `testing` | 테스트 + 구현 | - | 구현 파일 작성 |
| `implementing` | 전부 | PR 생성 | 리뷰 요청 |
| `reviewing` | 수정 가능 | PR 생성 | 리뷰 승인/반려 |
| `approved` | PR 생성 | - | PR 머지 |

### 명령어

```bash
python3 .claude/harness-workflow.py status                   # 현재 상태
python3 .claude/harness-workflow.py set-ticket <id> <title>  # 티켓 등록
python3 .claude/harness-workflow.py review-request           # 리뷰 요청
python3 .claude/harness-workflow.py review-approve           # 리뷰 승인
python3 .claude/harness-workflow.py review-reject <reason>   # 리뷰 반려
python3 .claude/harness-workflow.py reset                    # 초기화
```

## 3. GC 시스템 (`harness-gc.py` + `harness-gc-runner.py`)

코드베이스 전체를 스캔하여 규칙 위반을 찾고, 워커/리뷰어 피드백 루프로 자동 리팩토링한다.

### GC 플로우

```
scan → plan → worker fix → reviewer validate → complete/reject → loop
```

### 명령어

```bash
# 스캐너
python3 .claude/harness-gc.py scan              # 전체 스캔 (텍스트)
python3 .claude/harness-gc.py scan --json       # JSON 리포트
python3 .claude/harness-gc.py fix --dry-run     # 자동 수정 미리보기
python3 .claude/harness-gc.py fix               # 자동 수정 실행
python3 .claude/harness-gc.py watch             # 변경 파일만 스캔

# GC 러너 (워커/리뷰어 루프)
python3 .claude/harness-gc-runner.py plan              # 스캔 + 태스크 생성
python3 .claude/harness-gc-runner.py status             # GC 진행 상태
python3 .claude/harness-gc-runner.py worker-prompt <id> # 워커 프롬프트 생성
python3 .claude/harness-gc-runner.py reviewer-prompt <id> # 리뷰어 프롬프트 생성
python3 .claude/harness-gc-runner.py complete <id>      # 태스크 완료
python3 .claude/harness-gc-runner.py reject <id> <이유> # 태스크 반려
python3 .claude/harness-gc-runner.py report             # 최종 리포트
```

### 워커/리뷰어 격리 구조

```
Main Agent (오케스트레이터)
  │
  ├── Worker Agent A (closet-order 모듈)   ← worktree 격리
  ├── Worker Agent B (closet-shipping 모듈) ← worktree 격리
  │
  └── Reviewer Agent (독립 검증)
        ├── harness-gc.py watch 실행
        ├── 빌드 검증
        └── approve / reject
```

## 4. 파이프라인 프레임워크 (`pipeline-runner.py`)

`.analysis/` 디렉토리의 파이프라인들을 하네스 시스템과 통합하여 Phase 추적 + 게이트 강제를 수행한다.

### 사용 가능한 파이프라인

| 파이프라인 | Phase | Gates | 용도 |
|---|---|---|---|
| `prd` | 4 | 0 | PRD 분석 |
| `be-implementation` | 4 | 0 | BE 구현 설계 (TDD + 티켓) |
| `implementation` | 5 | 5 | 구현 (티켓 → 코드 → 테스트 → PR) |
| `verification` | 4 | 1 | 검증 (설계 원칙 + AC) |
| `pr-review` | 3 | 0 | PR 코드 리뷰 |
| `gc` | 5 | 3 | GC (스캔 → 수정 → 검증 루프) |
| `refactoring` | 4 | 2 | 리팩토링 |
| `incident` | 4 | 1 | 장애 대응 |
| `release` | 3 | 0 | 배포 영향 분석 |
| `api-change` | 3 | 0 | API 변경 분석 |

### 게이트 조건

| 게이트 | 조건 | 적용 파이프라인 |
|---|---|---|
| `ticket_required` | 워크플로우에 노션 티켓 등록 필수 | implementation |
| `test_first` | 테스트 코드 먼저 작성 | implementation, refactoring |
| `tests_exist` | 테스트 파일 1개 이상 존재 | implementation |
| `build_pass` | Gradle 빌드 성공 | implementation, refactoring, incident |
| `review_required` | 리뷰어 에이전트 승인 | implementation, gc |
| `harness_scan` | 하네스 GC 스캔 실행 | verification, gc |
| `harness_clean` | 규칙 위반 0건 | gc |

### 명령어

```bash
python3 .claude/pipeline-runner.py list                          # 파이프라인 목록
python3 .claude/pipeline-runner.py start implementation --name X # 시작
python3 .claude/pipeline-runner.py status                        # 현재 상태
python3 .claude/pipeline-runner.py advance                       # 다음 Phase
python3 .claude/pipeline-runner.py advance --force               # 게이트 무시 진행
python3 .claude/pipeline-runner.py gate-check                    # 게이트 체크
python3 .claude/pipeline-runner.py complete                      # 완료
python3 .claude/pipeline-runner.py abort                         # 중단
python3 .claude/pipeline-runner.py history                       # 실행 기록
```

## 5. Git pre-commit 훅

```bash
# 설치
cp .claude/git-hooks/pre-commit .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

체크 항목:
1. main 브랜치 직접 커밋 차단
2. 금지 패턴 검출 (@Query, ConsumerRecord, LocalDateTime, FQCN)
3. SQL 금지 패턴 (FK, ENUM, JSON)
4. 민감 파일 커밋 차단 (.env, credentials)
5. ktlint 체크 (Kotlin 파일)

## 6. Claude Hook 설정

`~/.claude/settings.json`에 설정된 훅:

| Hook | Matcher | 스크립트 | 역할 |
|---|---|---|---|
| `UserPromptSubmit` | * | `harness-workflow.py inject-reminder` | 워크플로우 상태 리마인더 주입 |
| `PreToolUse` | Write/Edit | `harness-check.py file-guard` | 민감 파일 차단 |
| `PreToolUse` | Write/Edit | `harness-check.py code-pattern` | 금지 패턴 차단 |
| `PreToolUse` | Write/Edit | `harness-workflow.py check-write` | 워크플로우 단계별 차단 |
| `PreToolUse` | Bash | `harness-check.py git-guard` | git push main 차단 |
| `PreToolUse` | Bash | `harness-workflow.py check-bash` | PR 생성 조건 체크 |
| `PostToolUse` | Write/Edit | `harness-workflow.py track-write` | 파일 작성 추적 |
| `PostToolUse` | Bash | `harness-check.py build-check` | 빌드 실패 감지 |

## 전체 개발 플로우

```
1. 파이프라인 시작
   python3 .claude/pipeline-runner.py start implementation --name "기능명"

2. 노션 티켓 등록
   python3 .claude/harness-workflow.py set-ticket <id> <title>

3. 테스트 작성 (Red)
   → *Test.kt / *Spec.kt 파일 작성
   → 워크플로우 자동 전환: ticket → testing

4. 구현 (Green)
   → src/main/ 소스 코드 작성
   → 워크플로우 자동 전환: testing → implementing

5. 빌드 + 테스트 검증
   ./gradlew compileKotlin && ./gradlew test

6. 리뷰
   python3 .claude/harness-workflow.py review-request
   → 리뷰어 에이전트 스폰
   → 승인: python3 .claude/harness-workflow.py review-approve
   → 반려: python3 .claude/harness-workflow.py review-reject "사유"

7. PR 생성 + 머지
   gh pr create ... && gh pr merge ...

8. 티켓 완료 → 다음 자동 시작
   python3 .claude/harness-orchestrator.py done
   → 자동으로 다음 티켓의 워크플로우가 설정됨
```

## 로드맵 자동 수행

"closet 로드맵 수행해"라고 하면 전체 티켓을 순차적으로 자동 수행한다.

### 오케스트레이터 (`harness-orchestrator.py`)

```bash
# 노션에서 티켓을 가져와 큐에 추가
python3 .claude/harness-orchestrator.py add <id> <title>

# JSON으로 일괄 추가 (노션 검색 결과 파이프)
echo '[{"id":"CP-101","title":"..."},...]' | python3 .claude/harness-orchestrator.py add-bulk

# 큐 확인
python3 .claude/harness-orchestrator.py queue

# 다음 티켓 시작 (워크플로우 자동 설정)
python3 .claude/harness-orchestrator.py next

# 티켓 완료 → 다음 자동 시작
python3 .claude/harness-orchestrator.py done

# 진행률
python3 .claude/harness-orchestrator.py progress
```

### 자동 수행 플로우

```
사용자: "closet 로드맵 수행해"
  ↓
에이전트: 노션에서 티켓 조회 → 오케스트레이터 큐에 추가
  ↓
orchestrator.py next → 첫 번째 티켓 자동 시작
  ↓
┌─────────────────────────────────────────┐
│  티켓 루프 (모든 티켓 완료까지 반복)    │
│                                         │
│  1. 테스트 작성 (ticket → testing)      │
│  2. 구현 (testing → implementing)       │
│  3. 리뷰 (implementing → approved)      │
│  4. PR + 머지                           │
│  5. orchestrator.py done → 다음 티켓    │
└─────────────────────────────────────────┘
  ↓
모든 티켓 완료!
```

### Notion 자동 연동

노션에서 티켓을 생성하면 (`mcp__notion__notion-create-pages`) PostToolUse 훅이 자동으로:
1. 페이지 ID + 제목 추출
2. `workflow-state.json`에 자동 등록
3. Phase: idle → ticket 자동 전환

별도의 `set-ticket` 명령 불필요.

### 의존성 관리

티켓 간 의존성을 지정하면 선행 티켓 완료 후에만 시작:

```bash
python3 .claude/harness-orchestrator.py add "CP-102" "검색 필터" --deps CP-101
```

`CP-101` 완료 전에는 `CP-102`가 `next`에서 선택되지 않음.
