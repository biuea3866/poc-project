# claude_framework

Claude Code 기반 멀티 레포 워크스페이스 템플릿이자 **플러그인**. 하네스 룰(훅), 분석 파이프라인, 역할별 에이전트/스킬/커맨드를 4계층으로 구조화하여 "PRD → 설계 → 구현 → 리뷰" 전체 플로우를 자동화한다.

- 🔌 **플러그인 설치 후 `/init`** — 기존 프로젝트에도 한 줄 이식, `--scan`으로 컨벤션 자동 추출, `--classify-repos`로 하위 레포 분류
- 🎛 **3-레이어 커스터마이징** — 플러그인 base + 프로젝트 공유 + 개인 오버라이드 자동 병합, 플러그인 업데이트받으면서 커스텀 유지
- 🧬 **Agent/Skill `extends` 패턴** — 플러그인 원본 프롬프트 상속 후 프로젝트 확장만 덧붙이기

---

## TL;DR

### 플러그인 방식 (권장)

```bash
# 1. 플러그인 설치 (최초 1회)
mkdir -p ~/.claude/plugins && cd ~/.claude/plugins
git clone https://github.com/biuea3866/poc-project claude-framework-src
ln -s claude-framework-src/claude_framework claude-framework

# 2. 어느 프로젝트에서든 init
cd /path/to/your-project
/init --scan --classify-repos
#   --scan           : 기존 코드에서 컨벤션 자동 추출 → harness-rules 주입
#   --classify-repos : 하위 서브디렉토리 BE/FE/DevOps 자동 분류

# 3. 전체 플로우
/analyze-prd <PRD URL>
/plan-project .analysis/prd/<산출물>.md
/parallel-tickets .analysis/project-analysis/<feature>/03-tickets.md 4
#   팀장 Opus 4.7 + 팀원 Sonnet 4.6, worktree 격리 병렬 구현
```

### 수동 복사 방식

```bash
cp -r claude_framework my-project && cd my-project
git config core.hooksPath .claude/git-hooks
# Claude Code 실행 후 위와 동일하게 커맨드 사용
```

상세: [PLUGIN.md](./PLUGIN.md) (플러그인) / [ADOPTION.md](./ADOPTION.md) (수동 복사)

---

## 4계층 아키텍처

```
┌──────────────────────────────────────────────────────────────┐
│ Command  (사용자 UX — 슬래시 커맨드)                           │
│   /analyze-prd, /plan-project, /parallel-tickets ...          │
└──────────────────────────────┬───────────────────────────────┘
                               ↓ 호출
┌──────────────────────────────────────────────────────────────┐
│ Pipeline  (무엇을, 어떤 순서로 — 설계 문서)                     │
│   .analysis/prd/PIPELINE.md                                   │
│   .analysis/project-analysis/PIPELINE.md                      │
└──────────────────────────────┬───────────────────────────────┘
                               ↓ 참조
┌──────────────────────────────────────────────────────────────┐
│ Agent  (누가 실행 — 페르소나 + 도구)                            │
│   pipeline-runner,                                            │
│   prd-analyst, project-analyst, ticket-splitter,              │
│   be-tech-lead, be-senior, fe-lead (페르소나, opus),            │
│   be-implementer, fe-implementer (구현 IC, sonnet),             │
│   pr-reviewer, harness-auditor,                               │
│   convention-detective, repo-classifier (/init 전용)            │
└──────────────────────────────┬───────────────────────────────┘
                               ↓ 참조
┌──────────────────────────────────────────────────────────────┐
│ Skill  (어떻게 — 절차/체크리스트/템플릿)                        │
│   prd-analysis, project-analysis-flow, mermaid-diagrams,      │
│   ticket-breakdown, tdd-loop, kotlin-spring-impl,             │
│   pr-review-checklist, harness-audit,                         │
│   codebase-convention-scan                                    │
└──────────────────────────────────────────────────────────────┘

[가로 레이어] Harness Rule (Claude 훅 기반 — 모든 레이어에 자동 적용)
  .claude/harness-rules.json  →  .claude/harness-check.py (PreToolUse/PostToolUse)
```

**참조 방향은 단방향**. Command → Pipeline → Agent → Skill. 역방향 참조 금지.

---

## 디렉토리 구조

```
claude_framework/
├── .claude-plugin/
│   └── plugin.json               # 플러그인 매니페스트 (v1.1.0)
├── .claude/
│   ├── harness-rules.json        # 금지 룰 단일 진실 원천 (플러그인 base)
│   ├── harness-rules.local.json  # 개인 오버라이드 (gitignore)
│   ├── harness-check.py          # 훅에서 호출 — 3-파일 병합 로직 포함
│   ├── resource-resolver.py      # Agent/Skill extends 병합 리졸버
│   ├── lockfile-writer.py        # claude-framework.lock.json 생성
│   ├── settings.json             # 템플릿 베이스 훅 설정 (체크인)
│   ├── settings.local.json       # 개인 퍼미션 오버라이드 (gitignore)
│   ├── mcp.json                  # MCP 서버 (atlassian/notion/github)
│   ├── agents/                   # 역할별 에이전트 (11종)
│   │   ├── pipeline-runner.md    # 파이프라인 오케스트레이터 (opus)
│   │   ├── prd-analyst.md        # PRD 분석 (opus)
│   │   ├── project-analyst.md    # 설계/TDD (opus)
│   │   ├── ticket-splitter.md    # 티켓 분해 (sonnet)
│   │   ├── be-tech-lead.md       # 아키텍처/서비스 영향 (opus)
│   │   ├── be-senior.md          # 프로덕션 안전성 (opus)
│   │   ├── fe-lead.md            # FE 아키텍처 (opus)
│   │   ├── be-implementer.md     # BE TDD 구현 (sonnet)
│   │   ├── fe-implementer.md     # FE TDD 구현 (sonnet)
│   │   ├── pr-reviewer.md        # PR 리뷰 (sonnet)
│   │   ├── harness-auditor.md    # 룰 감사 (sonnet)
│   │   ├── convention-detective.md  # /init --scan 컨벤션 추출 (opus)
│   │   └── repo-classifier.md    # /init --classify-repos 분류 (opus)
│   ├── skills/                   # 재사용 절차 (9종)
│   │   ├── prd-analysis/
│   │   ├── project-analysis-flow/
│   │   ├── mermaid-diagrams/
│   │   ├── ticket-breakdown/
│   │   ├── tdd-loop/
│   │   ├── kotlin-spring-impl/   # ★ Kotlin/Spring 7대 원칙 (문법/함수형/패턴/OOP/Rich Domain/풀네임/Enum)
│   │   ├── pr-review-checklist/
│   │   ├── harness-audit/
│   │   └── codebase-convention-scan/  # /init --scan용
│   ├── commands/                 # 슬래시 커맨드 (8종)
│   │   ├── init.md               # ★ 플러그인 이식 + 스캔 + 저장소 분류
│   │   ├── analyze-prd.md
│   │   ├── plan-project.md
│   │   ├── split-tickets.md
│   │   ├── tdd-implement.md
│   │   ├── review-pr.md
│   │   ├── audit-harness.md
│   │   └── parallel-tickets.md   # ★ git worktree + agent team 병렬
│   ├── common/                   # 공통 가이드 (7종)
│   │   ├── output-style.md
│   │   ├── mermaid.md
│   │   ├── ticket-guide.md
│   │   ├── jira-sync.md
│   │   ├── tdd-template.md
│   │   ├── document-sync.md
│   │   └── be-code-convention.md
│   └── git-hooks/
│       └── pre-commit            # 커밋 전 harness-check 실행
│
├── templates/                   # /init 이 복사할 파일들 (플러그인 전용)
│   ├── .claude/
│   │   ├── harness-check.py
│   │   ├── resource-resolver.py
│   │   ├── lockfile-writer.py
│   │   ├── settings.json
│   │   ├── mcp.json
│   │   ├── git-hooks/pre-commit
│   │   ├── presets/{kotlin,node,python,go}.json  # 스택별 룰 프리셋
│   │   └── harness-rules{,.local}.json.example
│   ├── .analysis/
│   ├── .gitignore
│   └── CLAUDE.md.template
│
├── .analysis/                    # 분석 파이프라인 산출물
│   ├── README.md
│   ├── prd/
│   │   └── PIPELINE.md           # PRD 분석 파이프라인
│   └── project-analysis/
│       └── PIPELINE.md           # 설계+TDD+티켓 파이프라인
│
├── be-repos/                     # BE 레포 워크트리 마운트
├── fe-repos/                     # FE 레포 워크트리 마운트
├── devops-repos/                 # 인프라/DevOps 워크트리 마운트
│
├── CLAUDE.md                     # Claude 전용 프로젝트 가이드
└── README.md                     # (이 파일)
```

---

## 하네스 룰 (Claude 훅)

모든 Write/Edit/Bash가 `harness-check.py`를 통과한다. 룰은 **3-파일 병합**으로 구성되며, 플러그인 업데이트와 프로젝트 커스텀이 공존한다.

### 3-파일 병합

```
plugin base (~/.claude/plugins/claude-framework/.claude/harness-rules.json)
  + project (.claude/harness-rules.json)           ← 팀 공유, git tracked
    + local (.claude/harness-rules.local.json)     ← 개인, gitignore
```

- 룰 배열: `id` 기준 dedupe, 뒤 레이어가 이김
- `_rule_overrides`: 특정 룰의 severity 등 필드만 재정의
- `_rule_disabled`: 특정 룰 비활성화
- 객체: deep merge


### 체크 타입

| 타입 | 시점 | 역할 | 위반 시 |
|---|---|---|---|
| `file-guard` | PreToolUse (Write/Edit) | `.env`/credentials/key 등 민감 파일 차단 | exit 2 (Claude 재작업) |
| `code-pattern` | PreToolUse (Write/Edit) | 금지 패턴 (`@Query`, `LocalDateTime`, FK 등) 검출 | exit 2 |
| `git-guard` | PreToolUse (Bash) | `push --force`, main 직접 push 차단 | exit 2 |
| `build-check` | PostToolUse (Bash) | 빌드 실패 로그 감지 | stderr 경고 |

### 기본 금지 룰 (Kotlin/SQL 예시)

- `@Query` 금지 → QueryDSL CustomRepository+Impl
- `LocalDateTime` 금지 → `ZonedDateTime`
- `ConsumerRecord<String, String>` 금지 → DTO 직접 매핑
- Consumer에서 Repository 직접 호출 금지
- SQL: `FOREIGN KEY`, `JSON`, `ENUM`, `BOOLEAN` 금지, `DATETIME(6)` 필수
- Kafka 토픽 하드코딩 금지

### 스모크 테스트

```bash
cd claude_framework

# 금지 패턴 검출 확인
CLAUDE_TOOL_INPUT='{"file_path":"test.kt","content":"val now = LocalDateTime.now()"}' \
  python3 .claude/harness-check.py code-pattern
# → exit 2 + "LocalDateTime 사용 금지"

# 위험 git 명령 차단 확인
CLAUDE_TOOL_INPUT='{"command":"git push origin main --force"}' \
  python3 .claude/harness-check.py git-guard
# → exit 2 + "--force 차단"
```

---

## Command 카탈로그

| Command | 용도 | 내부 호출 |
|---|---|---|
| `/analyze-prd <link>` | PRD 파이프라인 실행 | pipeline-runner + `prd-analysis` skill |
| `/plan-project <prd-path>` | 설계+TDD+티켓 파이프라인 | project-analyst + ticket-splitter |
| `/split-tickets <feature-dir>` | 티켓 분해 단독 실행 | ticket-splitter + `ticket-breakdown` |
| `/tdd-implement <ticket> [be\|fe]` | 단일 티켓 TDD 구현 | be/fe-implementer + `tdd-loop` |
| `/review-pr <num>` | PR 리뷰 + Verdict | pr-reviewer + `pr-review-checklist` |
| `/audit-harness [path]` | 룰 전수 감사 | harness-auditor + `harness-audit` |
| **`/parallel-tickets <tickets.md> [N=4]`** | **병렬 티켓 구현** | **팀장=opus + 팀원=sonnet** |
| **`/init [--scan] [--classify-repos] [--stack=..]`** | **플러그인 설치 후 프로젝트 이식** | **convention-detective + repo-classifier** |

---

## 사용 시나리오

### 시나리오 1: 신규 기능 End-to-End

```bash
# 1. PM이 준 PRD 링크로 분석 시작
/analyze-prd https://notion.so/xxx

# 산출물 검토 후 Open Questions 해결

# 2. 설계+TDD+티켓 분해
/plan-project .analysis/prd/2026-04-18-posting-feature.md

# 3. 병렬 구현 (팀장 opus + 팀원 sonnet, 동시 4개)
/parallel-tickets .analysis/project-analysis/2026-04-18-posting-feature/03-tickets.md 4
```

### 시나리오 2: 단일 티켓 처리

```bash
/tdd-implement T-03 be
# → feature/T-03-xxx 브랜치 자동 생성, be-implementer가 Red→Green→Refactor 수행
```

### 시나리오 3: PR 리뷰

```bash
/review-pr 123
# → 하네스/아키텍처/테스트/보안 체크리스트 실행
# → FE/DevOps는 자동 approve, BE는 comment만
```

### 시나리오 4: 주기 감사

```bash
/audit-harness be-repos/posting-service
# → harness-rules.json 위반 건 전수 리포트 (수정은 X, 발견만)
```

---

## /parallel-tickets 상세

가장 고급 커맨드. 티켓을 git worktree 기반으로 병렬 실행하며, 모델을 역할별로 분할한다.

### 모델 분할

| 역할 | 에이전트 | 모델 | 근거 |
|---|---|---|---|
| 팀장 | pipeline-runner | **Opus 4.7** | 의존성 해결, 충돌 조정, 실패 복구 결정 |
| BE 팀원 | be-implementer | **Sonnet 4.6** | 개별 티켓 TDD 구현 (반복 작업) |
| FE 팀원 | fe-implementer | **Sonnet 4.6** | 개별 티켓 TDD 구현 |
| 리뷰어 | pr-reviewer | **Sonnet 4.6** | 체크리스트 자동 리뷰 |

### 실행 흐름 (팀장 시점)

```
1. 03-tickets.md 파싱 (YAML 블록 + 종속성)
2. Topological sort → Wave 편성
   Wave 0: deps 없음 (병렬 가능)
   Wave 1: Wave 0만 참조 (병렬 가능)
   ...
3. 각 Wave 시작 시:
   - 티켓별 git worktree 생성
   - Agent(isolation=worktree, model=sonnet, run_in_background=true)로 병렬 스폰
4. 팀원 완료 알림 수집
   - 성공 → gh pr create → pr-reviewer 자동 리뷰
   - 실패 → SendMessage로 수정 지시 또는 재설계 에스컬레이션
5. Wave 전체 완료 → 다음 Wave
6. 최종 리포트: 티켓별 상태, PR 링크, 소요 시간
```

### 안전장치

- **격리**: `isolation: worktree` → 티켓마다 독립 체크아웃
- **동시성 제한**: `max-parallel` 인자로 조절 (기본 4)
- **동일 파일 충돌 방지**: 팀장이 wave 편성 시 사전 체크
- **에러 격리**: `run_in_background=true` → 한 worker 실패가 다른 worker에 전파 X
- **하네스 상속**: 각 worktree에서도 `.claude/settings.json`의 훅이 적용됨

---

## 어떻게 동작하는가 (Runtime 플로우)

### 전체 런타임 그림

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. Claude Code 세션 시작                                          │
│    cwd = claude_framework/                                        │
│    → .claude/settings.json 읽어 훅 등록                            │
│    → .claude/settings.local.json 읽어 개인 퍼미션 병합             │
│    → .claude/mcp.json 읽어 MCP 서버 connect                       │
│    → .claude/agents/, skills/, commands/ 자동 디스커버리          │
└────────────────────────────────────┬────────────────────────────┘
                                     ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. 사용자 입력                                                    │
│    (a) /command 입력   → 2a 분기                                  │
│    (b) 자연어 입력      → Claude가 직접 에이전트/스킬 선택         │
└────────────────────────────────────┬────────────────────────────┘
                                     ↓
┌───────────────────────── 2a. Command 실행 ─────────────────────┐
│  .claude/commands/<name>.md 본문을 "시스템 프롬프트"로 주입      │
│  $1, $2, $ARGUMENTS 치환 후 Claude가 본문대로 실행              │
│  본문은 Pipeline 또는 Agent 호출 프롬프트                        │
└────────────────────────────────────┬───────────────────────────┘
                                     ↓
┌───────────────────────── 3. Pipeline 실행 ────────────────────┐
│  pipeline-runner 에이전트가 PIPELINE.md를 읽고 단계 수행         │
│  각 단계마다 적절한 서브에이전트 spawn (Agent 툴)                │
└────────────────────────────────────┬───────────────────────────┘
                                     ↓
┌────────────────────────── 4. Agent 실행 ──────────────────────┐
│  Agent 툴이 새 Claude 세션을 fork (컨텍스트 격리)                │
│  subagent_type으로 .claude/agents/<name>.md 로드                │
│  에이전트 프롬프트에 "사용 스킬" 명시 → 스킬 본문 로드            │
│  tools 제한 + model 지정 (opus/sonnet/haiku)                    │
└────────────────────────────────────┬───────────────────────────┘
                                     ↓
┌────────────────────────── 5. Skill 적용 ──────────────────────┐
│  SKILL.md 본문이 에이전트의 실행 컨텍스트로 들어감               │
│  절차/체크리스트/산출물 템플릿에 따라 작업 수행                   │
└────────────────────────────────────┬───────────────────────────┘
                                     ↓
┌──────────────────── 6. Tool 호출 (Write/Edit/Bash) ───────────┐
│  매 도구 호출마다 훅 실행:                                       │
│    PreToolUse  → harness-check.py (file-guard/code-pattern/   │
│                                     git-guard)                 │
│    exit 2 → Claude에게 차단 메시지 전달 → 재작업                │
│    exit 0 → 도구 실제 실행                                      │
│    PostToolUse → harness-check.py build-check                 │
└────────────────────────────────────┬───────────────────────────┘
                                     ↓
┌─────────────────────────── 7. 결과 ─────────────────────────────┐
│  에이전트 결과가 상위 세션에 반환                                │
│  pipeline-runner가 Exit Criteria 체크 → 다음 단계 또는 완료      │
│  최종 산출물: .analysis/<pipeline>/ 또는 코드 변경 + PR          │
└─────────────────────────────────────────────────────────────────┘
```

### 훅 동작 메커니즘 (가장 중요)

Claude Code는 settings.json에 정의된 훅을 읽어 **도구 호출 직전/직후에 외부 커맨드를 실행**한다.

```json
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Write|Edit",
        "hooks": [
          { "type": "command", "command": "python3 ${CLAUDE_PROJECT_DIR}/.claude/harness-check.py code-pattern" }
        ]
      }
    ]
  }
}
```

실행 순서:
1. Claude가 `Write(file_path="X.kt", content="...LocalDateTime...")` 호출 시도
2. Claude Code 런타임이 `CLAUDE_TOOL_INPUT` 환경변수에 도구 인자(JSON) 담아 훅 실행
3. `harness-check.py code-pattern`이:
   - `harness-rules.json` 로드
   - `file_glob`(`*.kt`)이 매치하는 룰 필터
   - 정규식 매칭 → `LocalDateTime` 발견
   - stderr에 에러 메시지 + `sys.exit(2)`
4. exit code 2 → Claude Code가 도구 실행을 **차단**하고 에러 메시지를 Claude에게 반환
5. Claude가 다시 `ZonedDateTime`으로 수정해 재시도

결과: **사람이 리뷰하지 않아도 금지 패턴이 코드에 들어가지 않는다.**

### 에이전트 컨텍스트 격리

`Agent` 툴은 매 호출마다 **새 Claude 세션을 fork**한다:

- 상위 세션의 대화 히스토리 X (프롬프트로 전달한 것만 보임)
- 자체 tool 권한, model 설정
- 종료 시 결과(문자열)만 상위에 반환

→ 메인 세션의 컨텍스트 윈도우 오염 없이 대용량 분석/구현 작업 위임 가능.

### /parallel-tickets 런타임 상세

```
시각 t=0: 팀장(opus) 세션 시작
  ├─ Read: 03-tickets.md
  ├─ Parse: 티켓 10개, wave 편성 (W0=3개, W1=4개, W2=3개)
  └─ Wave 0 시작 ──────────────────────────────────┐
                                                  ↓
시각 t=1: 팀장이 단일 메시지에 Agent 툴 3개 호출 (병렬)
  ├─ Agent(be-implementer, sonnet, isolation=worktree,  │
  │         name="worker-T-01", run_in_background=true) │  ← 백그라운드
  ├─ Agent(fe-implementer, sonnet, ...T-02...)          │  ← 백그라운드
  └─ Agent(be-implementer, sonnet, ...T-03...)          │  ← 백그라운드
                                                         │
시각 t=2~10: 팀장은 다른 작업 가능 (또는 대기)              │
  - 각 worker가 자기 worktree에서 tdd-loop 실행 중        │
  - worker의 Write/Edit도 harness-check.py 훅 통과       │
  - worker 완료 시 팀장에게 알림                          │
                                                         │
시각 t=11: T-02 완료 알림                                │
  ├─ 팀장이 Bash: gh pr create                          │
  ├─ Agent(pr-reviewer, sonnet, PR=456) → approve       │
  └─ FE 라벨이면 자동 머지 / BE면 사람 대기               │
                                                         │
시각 t=13: T-01, T-03도 완료                              │
  └─ Wave 0 전체 완료 → Wave 1 시작 (같은 패턴 반복)
```

### 파일 시스템 레이아웃 (런타임)

`/parallel-tickets` 실행 중 생성되는 임시 구조:

```
claude_framework/
├── be-repos/posting-service/       # 원본 체크아웃 (main)
└── worktrees/                      # 티켓별 격리 worktree
    ├── T-01/                       # git worktree add -b feature/T-01 ...
    ├── T-02/
    └── T-03/
# 티켓 머지 후 worktree remove로 정리
```

각 worktree는 독립 브랜치 + 독립 파일시스템 뷰 → 병렬 안전.

---

## 확장 가이드

| 하고 싶은 것 | 어디를 수정 |
|---|---|
| 새 언어/프레임워크 금지 룰 추가 | `.claude/harness-rules.json` → `forbidden_patterns.rules` |
| 새 파이프라인 추가 | `.analysis/<name>/PIPELINE.md` + 담당 에이전트/스킬 표 |
| 새 에이전트 | `.claude/agents/<name>.md` (frontmatter + 사용 스킬 참조) |
| 새 스킬 | `.claude/skills/<name>/SKILL.md` (when/원칙/절차/완료체크) |
| 새 커맨드 | `.claude/commands/<name>.md` (frontmatter + 얇은 프롬프트) |
| MCP 서버 추가 | `.claude/mcp.json` |
| 퍼미션 조정 | `.claude/settings.local.json` (개인) 또는 `settings.json` (팀 공통) |

---

## 설계 원칙

1. **단일 진실 원천(SSOT)** — 룰은 `harness-rules.json` 한 곳. 훅/pre-commit/감사 모두 여기만 본다.
2. **계층 단방향** — Command → Pipeline → Agent → Skill. 역참조 금지.
3. **빈 껍데기 금지** — 모든 산출물 섹션은 실제 내용으로 채워져야 완료.
4. **TDD 강제** — 모든 구현은 Red→Green→Refactor. 테스트 없는 PR 금지.
5. **병렬성 기본** — 독립 티켓은 worktree + background Agent로 병렬 실행.
6. **모델 역할 분할** — 판단/조정은 Opus, 반복 구현은 Sonnet. 비용/속도 최적화.
7. **자동 승인 제한** — FE/DevOps PR은 자동 approve 허용, BE PR은 사람 대기.

---

## 다른 프로젝트로 이식

두 가지 방법:
- **[PLUGIN.md](./PLUGIN.md)** — Claude Code 플러그인으로 설치 후 `/init` 한 줄로 이식. 코드베이스 스캔(`--scan`)과 하위 저장소 자동 분류(`--classify-repos`) 지원. ★ **권장**
- **[ADOPTION.md](./ADOPTION.md)** — 수동 복사 방식. 플러그인 인프라가 없을 때 또는 커스터마이징이 필요할 때.

## 참고 문서

- [ADOPTION.md](./ADOPTION.md) — 다른 프로젝트 이식 가이드
- [CLAUDE.md](./CLAUDE.md) — Claude가 세션 시작 시 로드하는 프로젝트 가이드
- [.analysis/README.md](./.analysis/README.md) — 파이프라인 카탈로그
- [.claude/agents/README.md](./.claude/agents/README.md) — 에이전트 카탈로그
- [.claude/skills/README.md](./.claude/skills/README.md) — 스킬 카탈로그
- [.claude/commands/README.md](./.claude/commands/README.md) — 커맨드 카탈로그
- [.claude/common/README.md](./.claude/common/README.md) — 공통 가이드 카탈로그
