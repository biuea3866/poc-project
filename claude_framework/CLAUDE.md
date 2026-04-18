# claude_framework — 프로젝트 템플릿

Claude Code 기반 멀티 레포 워크스페이스 템플릿. `.claude/` 하네스와 `.analysis/` 파이프라인을 내장한다.

## 디렉토리 레이아웃

```
claude_framework/
├── .claude/
│   ├── harness-rules.json       # 하지 말아야 할 룰 (단일 진실 원천)
│   ├── harness-check.py         # 훅에서 호출되는 파이썬 체커
│   ├── settings.json            # 템플릿 베이스 훅 설정 (체크인)
│   ├── settings.local.json      # 개인 퍼미션 오버라이드 (gitignore)
│   ├── mcp.json                 # MCP 서버 설정
│   ├── agents/                  # 역할별 에이전트 (8종)
│   ├── skills/                  # 재사용 절차/노하우 (7종)
│   ├── commands/                # 슬래시 커맨드 (7종, 병렬 실행 포함)
│   └── git-hooks/               # pre-commit 등 Git 훅 스크립트
├── .analysis/
│   ├── prd/                     # PRD 분석 파이프라인
│   └── project-analysis/        # 프로젝트 분석 (TDD, 티켓, 상세 설계)
├── be-repos/                    # 백엔드 서비스 워크트리
├── fe-repos/                    # 프론트엔드 워크트리
└── devops-repos/                # 인프라/DevOps 워크트리
```

## 하네스 룰 (Claude 훅)

`harness-check.py`는 PreToolUse/PostToolUse 훅에서 호출되어 `harness-rules.json`의 룰을 검증한다.

| 체크 타입 | 시점 | 역할 |
|---|---|---|
| `file-guard` | PreToolUse (Write/Edit) | `.env`, `credentials.json` 등 민감 파일 차단 |
| `code-pattern` | PreToolUse (Write/Edit) | 금지 패턴(`@Query`, `LocalDateTime`, FK 등) 검출 |
| `git-guard` | PreToolUse (Bash) | `push --force`, main 직접 push 등 차단 |
| `build-check` | PostToolUse (Bash) | 빌드 실패 로그 감지 |

**룰 추가/수정은 `harness-rules.json`만 편집하면 됩니다.** 체커 로직은 건드릴 필요 없음.

## 활성화 방법

Claude Code가 `claude_framework/`를 프로젝트 루트로 인식해야 한다:

1. `settings.local.json`의 hooks가 `${CLAUDE_PROJECT_DIR}/claude_framework/.claude/harness-check.py` 경로로 등록됨
2. Claude Code 실행 시 해당 디렉토리에서 세션 시작
3. `harness-check.py`는 `cwd`가 `claude_framework/` 하위일 때만 동작 (다른 프로젝트에 간섭 X)

## 분석 파이프라인

| 파이프라인 | 진입 문서 |
|---|---|
| PRD 분석 | `.analysis/prd/PIPELINE.md` |
| 프로젝트 분석 (TDD/티켓) | `.analysis/project-analysis/PIPELINE.md` |

각 파이프라인은 자체 `PIPELINE.md`의 지시를 따라 수행하고, 산출물을 같은 디렉토리에 누적한다.

## 4계층 구조 (Command / Pipeline / Agent / Skill)

| 계층 | 질문 | 위치 |
|---|---|---|
| Command | 사용자 트리거 (UX 레이어) | `.claude/commands/<name>.md` |
| Pipeline | 무엇을, 어떤 순서로 | `.analysis/<name>/PIPELINE.md` |
| Agent | 누가 실행 (페르소나 + 도구) | `.claude/agents/<name>.md` |
| Skill | 어떻게 (절차/체크리스트/템플릿) | `.claude/skills/<name>/SKILL.md` |

**참조 방향**: Command → Pipeline → Agent → Skill (역방향 금지).

**흐름**: 사용자가 `/<cmd>` 호출 → Command 본문이 Pipeline/Agent를 호출 → Agent가 Skill 본문 로드해 절차대로 실행.

## 템플릿 확장 가이드

- **새 언어/프레임워크 규칙 추가**: `harness-rules.json`의 `forbidden_patterns`에 룰 1개 추가
- **새 파이프라인 추가**: `.analysis/<name>/PIPELINE.md` 작성 + 담당 에이전트/스킬 표 포함
- **에이전트 추가**: `.claude/agents/<name>.md`에 frontmatter + 사용 스킬 참조 명시
- **스킬 추가**: `.claude/skills/<name>/SKILL.md`에 언제/원칙/절차/완료체크 구조
