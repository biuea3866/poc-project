# claude_framework — 프로젝트 템플릿

Claude Code 기반 멀티 레포 워크스페이스 템플릿. `.claude/` 하네스와 `.analysis/` 파이프라인을 내장한다.

## 3가지 실행 모드

이 프레임워크는 세 가지 상태로 존재할 수 있다. 경로/훅이 각각 다르므로 주의.

| 모드 | 위치 | 훅 경로 | 언제 |
|---|---|---|---|
| **dogfood** | `claude_framework/` 저장소 자체 | `${CLAUDE_PROJECT_DIR}/.claude/harness-check.py` | 플러그인 자체 개발 |
| **plugin** | `~/.claude/plugins/cache/<marketplace>/claude-framework/<ver>/` | 플러그인 로더가 자동 로드 | 사용자가 `/plugin install` 후 다른 프로젝트에서 사용 |
| **adopted** (`/init` 후) | 사용자 프로젝트의 `.claude/`, `.analysis/` | `${CLAUDE_PROJECT_DIR}/.claude/harness-check.py` | `/init`으로 이식된 프로젝트 |

### dogfood 모드 (이 저장소 자체)
- Claude Code 실행 시 cwd가 `claude_framework/` 루트
- `settings.json`의 훅 경로: `${CLAUDE_PROJECT_DIR}/.claude/harness-check.py`
- `.analysis/`도 이 저장소 내부. 플러그인 자체 개선에 사용.

### plugin 모드 (다른 프로젝트에서 설치 후 사용)
- Claude Code의 플러그인 시스템이 자동 로드
- 에이전트/스킬/커맨드는 플러그인 네임스페이스로 접근 (`@claude-framework:be-implementer` 등)
- 사용자 프로젝트에는 아무 파일도 깔리지 않음 (스캐폴드는 `/init` 실행 시에만)
- common/ 문서는 `${CLAUDE_PLUGIN_ROOT}/common/`에 존재

### adopted 모드 (`/init` 실행된 프로젝트)
- 프로젝트 루트에 `.claude/`, `.analysis/`, `CLAUDE.md`가 생성됨
- `.claude/harness-rules.json`은 프로젝트 팀 전용 룰 (플러그인 base와 병합)
- 에이전트/스킬/커맨드는 여전히 플러그인이 제공 (프로젝트로 복사하지 않음)
- common/ 는 `.claude/common/`에 미러되거나 플러그인 원본 참조

## 디렉토리 레이아웃 (dogfood / adopted 공통 구조)

```
<project-root>/
├── .claude/
│   ├── harness-rules.json       # 하지 말아야 할 룰 (단일 진실 원천)
│   ├── harness-check.py         # 훅에서 호출되는 파이썬 체커
│   ├── settings.json            # 템플릿 베이스 훅 설정 (체크인)
│   ├── settings.local.json      # 개인 퍼미션 오버라이드 (gitignore)
│   ├── mcp.json                 # MCP 서버 설정
│   └── git-hooks/               # pre-commit 등 Git 훅 스크립트
├── .analysis/
│   ├── prd/                     # PRD 분석
│   ├── project-analysis/        # 프로젝트 분석 (TDD, 티켓)
│   ├── be-implementation/       # BE 구현 산출물
│   ├── pr-review/               # PR 리뷰 기록
│   ├── inquiry/                 # 문의/버그 대응
│   ├── incident/                # 장애 포스트모템
│   ├── release/                 # 릴리즈 영향 분석
│   ├── refactoring/             # 리팩토링 계획
│   └── api-change/              # API 변경 계획
├── be-repos/                    # (멀티레포) 백엔드 워크트리
├── fe-repos/                    # (멀티레포) 프론트엔드 워크트리
└── devops-repos/                # (멀티레포) 인프라/DevOps 워크트리
```

## 하네스 룰 (Claude 훅)

`harness-check.py`는 PreToolUse/PostToolUse 훅에서 호출되어 `harness-rules.json`의 룰을 검증.

| 체크 타입 | 시점 | 역할 |
|---|---|---|
| `file-guard` | PreToolUse (Write/Edit) | `.env`, `credentials.json` 등 민감 파일 차단 |
| `code-pattern` | PreToolUse (Write/Edit) | 금지 패턴(`@Query`, `LocalDateTime`, FK 등) 검출 |
| `git-guard` | PreToolUse (Bash) | `push --force`, main 직접 push 등 차단 |
| `build-check` | PostToolUse (Bash) | 빌드 실패 로그 감지 |

**룰 추가/수정은 `harness-rules.json`만 편집**하면 된다. 체커 로직은 건드릴 필요 없음.

### 3-파일 병합
```
plugin harness-rules.json (base)
  ↓ merge
.claude/harness-rules.json (팀 공유, tracked)
  ↓ merge
.claude/harness-rules.local.json (개인, gitignore)
```

자세히는 `PLUGIN.md`의 "커스터마이징" 섹션 참조.

### 3-layer Feedback Loop

로컬 훅 1개만 두면 **훅 자체가 silent pass** 될 때 전부 뚫린다. 실제 사례: rental-commerce Sprint 4 에서 `harness-rules.json` 콤마 누락 → 훅 silent exit 0 → `@RoleRequired` 누락 관리자 엔드포인트 잠입. 이를 방지하기 위해 시점이 다른 4개 레이어로 중첩 방어:

| Layer | 시점 | 스크립트 | 막는 실패 유형 |
|---|---|---|---|
| 1. Local hook | 파일 저장 순간 | `.claude/harness-check.py` | regex 로 잡히는 패턴 위반 |
| 2. PR Senior Gate | PR open/sync (GHA) | `scripts/senior-gate.py` + `scripts/harness-audit.py --diff-files` | `@RoleRequired` 누락 등 구조적 Critical |
| 3. Nightly Audit | 매일 03:00 KST (GHA) | `scripts/harness-audit.py` (전수) | Layer 1 silent pass, legacy 위반 |
| 4. QA Follow-up | `docs/qa/*.md` push (GHA) | `scripts/qa-followup-extract.py` | QA 후속 티켓 누락 |

- 주기 점검 에이전트: `feedback-loop-guardian`
- 파이프라인 문서: `.analysis/feedback-loop/PIPELINE.md`
- 템플릿 위치: `templates/.claude/scripts/`, `templates/.github/workflows/`

## 분석 파이프라인 (10종)

| 파이프라인 | 진입 문서 | 담당 에이전트 |
|---|---|---|
| PRD 분석 | `.analysis/prd/PIPELINE.md` | prd-analyst |
| 프로젝트 분석 | `.analysis/project-analysis/PIPELINE.md` | project-analyst → ticket-splitter |
| BE 구현 | `.analysis/be-implementation/PIPELINE.md` | be-implementer + be-senior |
| PR 리뷰 | `.analysis/pr-review/PIPELINE.md` | pr-reviewer + harness-auditor (+tech-lead/senior 병행) |
| 문의/버그 | `.analysis/inquiry/PIPELINE.md` | be-senior → be-implementer |
| 장애 대응 | `.analysis/incident/PIPELINE.md` | be-tech-lead + be-senior |
| 릴리즈 영향 | `.analysis/release/PIPELINE.md` | be-tech-lead + be-senior + harness-auditor |
| 리팩토링 | `.analysis/refactoring/PIPELINE.md` | be-tech-lead → be-implementer |
| API 변경 | `.analysis/api-change/PIPELINE.md` | be-tech-lead + fe-lead |
| 피드백 루프 | `.analysis/feedback-loop/PIPELINE.md` | feedback-loop-guardian + harness-auditor |

각 파이프라인은 자체 `PIPELINE.md`의 지시를 따라 수행하고, 산출물을 같은 디렉토리에 누적.

## 4계층 구조 (Command / Pipeline / Agent / Skill)

| 계층 | 질문 | 위치 |
|---|---|---|
| Command | 사용자 트리거 (UX) | `commands/<name>.md` |
| Pipeline | 무엇을, 어떤 순서로 | `.analysis/<name>/PIPELINE.md` |
| Agent | 누가 실행 (페르소나 + 도구) | `agents/<name>.md` |
| Skill | 어떻게 (절차/체크리스트/템플릿) | `skills/<name>/SKILL.md` |

**참조 방향**: Command → Pipeline → Agent → Skill (역방향 금지).

**흐름**: 사용자가 `/<cmd>` 호출 → Command 본문이 Pipeline/Agent를 호출 → Agent가 Skill 본문 로드해 절차대로 실행.

## 활성화 방법 (모드별)

### dogfood
```
cd claude_framework/
# Claude Code 실행
# settings.json 훅이 ${CLAUDE_PROJECT_DIR}/.claude/harness-check.py 자동 로드
```

### plugin (다른 프로젝트에서 사용)
```
/plugin marketplace add <path-or-url>
/plugin install claude-framework
# 다른 프로젝트에서 /init 실행
```

### adopted (/init으로 이식된 후)
```
cd my-project/
# Claude Code 실행
# 자동으로 .claude/harness-check.py 훅 활성
# 에이전트/스킬/커맨드는 플러그인에서 제공됨
```

## 템플릿 확장 가이드

- **새 언어/프레임워크 규칙 추가**: `harness-rules.json`의 `forbidden_patterns`에 룰 1개
- **새 파이프라인 추가**: `.analysis/<name>/PIPELINE.md` + 담당 에이전트/스킬 표 포함
- **에이전트 추가**: `agents/<name>.md`에 frontmatter + 사용 스킬 참조 + `.claude-plugin/plugin.json`에 등록
- **스킬 추가**: `skills/<name>/SKILL.md`에 언제/원칙/절차/완료체크 구조
- **커스터마이징(프로젝트별)**: `.claude/agents/<name>.md`의 `extends: claude-framework:<name>`로 부분 확장
