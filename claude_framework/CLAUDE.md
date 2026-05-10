# claude_framework — 프로젝트 템플릿

Claude Code 기반 멀티 레포 워크스페이스 템플릿. **4계층 구조(Command → Pipeline → Agent → Skill)** + **다층 방어(로컬 훅 → pre-commit → CI Senior Gate → nightly 감사)** + **메타-피드백 루프**(prompt 자체 개선) 를 내장한다.

> 자세한 설계 결정과 마이그레이션 계획은 [`REFACTOR.md`](./REFACTOR.md) 참조.

## 디렉토리 레이아웃

```
claude_framework/
├── .claude/
│   ├── harness-rules.json                          # ★ 단일 룰 진실 원천
│   ├── settings.json                               # 베이스 훅 설정 (체크인)
│   ├── settings.local.json                         # 개인 퍼미션 (gitignore)
│   ├── settings.json.feedback-loop.example         # 메타-피드백 활성화 스니펫
│   ├── mcp.json                                    # MCP 서버 설정
│   ├── harness-check.py                            # PreToolUse 정적 체커 (legacy 위치)
│   └── scripts/                                    # 신규 스크립트
│       ├── harness-check.py                        # 0.5초 정적 차단
│       ├── harness-audit.py                        # 전수/diff 감사
│       ├── senior-gate.py                          # Critical 4유형 LLM-free 검출
│       └── qa-followup-extract.py                  # QA → Issue 파서
├── agents/                                         # 페르소나 + 모델 + 도구
│   ├── be-implementer.md / fe-implementer.md       # sonnet, 구현
│   ├── pr-reviewer.md / be-senior.md / be-tech-lead.md / fe-lead.md / security-reviewer.md
│   ├── prd-analyst.md / project-analyst.md / ticket-splitter.md
│   ├── harness-auditor.md / convention-detective.md / pipeline-runner.md / repo-classifier.md
│   ├── feedback-loop-guardian.md                   # sonnet, 루프 건강 + 효과 측정
│   └── process-reviewer.md                         # sonnet, 메타-피드백 (Stop/SubagentStop 훅)
├── skills/                                         # "어떻게" 재사용 절차
├── commands/                                       # 사용자 트리거 슬래시 커맨드
├── pipelines/                                      # 파이프라인 — 무엇을, 어떤 순서로
│   ├── prd/PIPELINE.md
│   ├── project-analysis/PIPELINE.md
│   ├── be-implementation/PIPELINE.md
│   ├── pr-review/PIPELINE.md
│   ├── feedback-loop/PIPELINE.md                   # ★ 메타-피드백 운영 절차
│   ├── refactoring/PIPELINE.md
│   ├── release/PIPELINE.md
│   ├── api-change/PIPELINE.md
│   ├── incident/PIPELINE.md
│   └── inquiry/PIPELINE.md
├── docs/
│   └── feedback-loop/
│       └── proposals/                              # process-reviewer 가 생성하는 제안 파일
│           ├── README.md
│           ├── archived/                           # 보류
│           └── closed/                             # 기각
├── .github/workflows/
│   ├── pr-senior-review.yml                        # PR open/sync, Critical 차단
│   ├── harness-audit.yml                           # nightly 03:00 KST + push 시
│   └── qa-followup-tickets.yml                     # docs/qa/*.md push → Issue
├── templates/                                      # 새 프로젝트 install.sh 가 복사
├── be-repos/  fe-repos/  devops-repos/             # 멀티 레포 워크트리 (사용자가 mkdir 또는 /init --classify-repos)
└── REFACTOR.md  README.md  ADOPTION.md  PLUGIN.md
```

## 4계층 구조 (Command → Pipeline → Agent → Skill)

| 계층 | 책임 | 위치 | 참조 가능 대상 |
|------|------|------|----------------|
| Command | 사용자 트리거 (UX) | `.claude/commands/<name>.md` | Pipeline, Agent |
| Pipeline | 무엇을·어떤 순서로 | `pipelines/<name>/PIPELINE.md` | Agent, Skill |
| Agent | 누가 (페르소나 + 모델 + 도구) | `.claude/agents/<name>.md` 또는 `agents/<name>.md` | Skill, Rule |
| Skill | 어떻게 (절차) | `.claude/skills/<name>/SKILL.md` 또는 `skills/<name>/SKILL.md` | Rule |
| Rule | 하지 말 것 | `.claude/harness-rules.json` | (참조됨) |

**의존 방향**: Command → Pipeline → Agent → Skill → Rule. **역방향 금지**.

**흐름**: 사용자 `/<cmd>` → Command 본문이 Pipeline 진입 → Pipeline 단계별 Agent 스폰 → Agent 가 Skill 본문 로드해 실행 → Rule 은 정적 검증으로 자동 반영.

## 다층 방어 (4 layers)

| 층 | 시점 | 도구 | 비용 |
|----|------|------|------|
| 1. PreToolUse 훅 | 매 Write/Edit/Bash | `harness-check.py` (정적 0.5s) | 무료 |
| 2. pre-commit | 커밋 직전 | `git-hooks/pre-commit` | 수초 |
| 3. PR Senior Gate | PR open/sync | `senior-gate.py` + `harness-audit.py --diff-files` + 분야별 LLM 리뷰어 5종 | 분 단위 |
| 4. Nightly Harness Audit | 03:00 KST | `harness-audit.py` 전수 + silent-pass 감지 | 분 단위 |

한 층이 무력화돼도 다음 층이 잡는다.

## 하네스 룰 (단일 진실 원천)

`harness-rules.json` 한 파일에 모든 룰. `harness-check.py` 가 PreToolUse 훅에서 호출.

| 체크 타입 | 시점 | 역할 |
|---|---|---|
| `file-guard` | Write/Edit | `.env`, `credentials.json` 등 민감 파일 차단 |
| `code-pattern` | Write/Edit | 금지 패턴(`@Query`, `LocalDateTime`, FK 등) 검출 |
| `git-guard` | Bash | `push --force`, 보호 브랜치 직접 push, **upstream 이 main/dev 인 채로 push** 등 차단 |
| `build-check` | PostToolUse Bash | 빌드 실패 로그 감지 |

**git_upstream_guard (동적)**: `git push` 호출 시 현재 브랜치의 실제 upstream 을 git 으로 조회해 보호 브랜치이면 차단. fail-safe 로 git 호출 실패 시 통과.

룰 추가/수정은 `harness-rules.json` 만 편집. 체커 로직은 건드릴 필요 없음.

## 메타-피드백 루프 (Stop / SubagentStop 훅)

코드 산출물(PR diff) 리뷰 외에 **참조한 command/skill/rule 자체** 가 잘못돼 실패를 유발했는지 점검하는 별도 축.

**트리거 조건 (정확히 4가지)**:
1. PR Senior Gate fail
2. 자동 재시도 N회 중 1회 이상 실패
3. PreToolUse 훅 차단
4. QA 보고서에 "후속 티켓" 신규 추가

**process-reviewer (sonnet)** 가 발화 → `docs/feedback-loop/proposals/<YYYYMMDD>-<topic>.md` 제안 파일 생성. **자동 .md 수정 절대 금지** — 사람·main-orchestrator 검토 후 PR 로만 반영.

**feedback-loop-guardian (sonnet)** 가 nightly 효과 측정 → 같은 trigger 재발 빈도로 `effective`/`partial`/`ineffective` 판정.

**안전 장치**:
- `stop_hook_active` 가드 (재귀 차단)
- 일일 발화 상한 5회
- 비교 기준 강제 (PRD/ADR/harness-rules.json 외 LLM 의견만으로 판정 금지)

활성화: `.claude/settings.json.feedback-loop.example` 의 hooks 객체를 `settings.json` 에 병합.

상세 절차: [`pipelines/feedback-loop/PIPELINE.md`](./pipelines/feedback-loop/PIPELINE.md)

## 분석 파이프라인

| 파이프라인 | 진입 문서 | 비고 |
|---|---|---|
| PRD 분석 | `pipelines/prd/PIPELINE.md` | |
| 프로젝트 분석 (TDD/티켓) | `pipelines/project-analysis/PIPELINE.md` | |
| BE 구현 | `pipelines/be-implementation/PIPELINE.md` | PRD → ADR → TDD → 티켓 → 구현 |
| PR 리뷰 | `pipelines/pr-review/PIPELINE.md` | 분야별 5인 병렬 |
| 메타-피드백 | `pipelines/feedback-loop/PIPELINE.md` | Stop 훅 + 제안 파일 |
| 리팩토링 | `pipelines/refactoring/PIPELINE.md` | |
| 배포 | `pipelines/release/PIPELINE.md` | |
| API 변경 | `pipelines/api-change/PIPELINE.md` | |
| 장애 대응 | `pipelines/incident/PIPELINE.md` | |
| 문의/버그 트리아지 | `pipelines/inquiry/PIPELINE.md` | |

## 모델·비용 분리

| 역할 | 모델 |
|------|------|
| 메인 분석/리뷰 | Opus (`pr-reviewer`, `be-senior`, `be-tech-lead`, `security-reviewer`, `fe-lead`) |
| 구현 | Sonnet (`be-implementer`, `fe-implementer`, `ticket-splitter`) |
| 메타·체커 | Sonnet (`process-reviewer`, `feedback-loop-guardian`, `convention-detective`) |

> 메인 에이전트 모델은 사용자가 `/model` 로 선택. 강제 불가. 서브 에이전트만 frontmatter `model:` 로 고정.

## 활성화 방법

1. `settings.json` hooks 가 `${CLAUDE_PROJECT_DIR}/.claude/scripts/harness-check.py` 경로 등록 (legacy `${CLAUDE_PROJECT_DIR}/.claude/harness-check.py` 도 동일 동작)
2. Claude Code 실행 시 해당 디렉토리에서 세션 시작
3. 메타-피드백 활성화 (선택): `settings.json.feedback-loop.example` 의 `hooks` 병합
4. `harness-check.py` 는 `cwd` 가 `claude_framework/` 또는 `.claude/harness-rules.json` 보유 프로젝트일 때만 동작

## 템플릿 확장 가이드

- **새 룰 추가**: `harness-rules.json` `forbidden_patterns` 에 1개 추가
- **새 파이프라인 추가**: `pipelines/<name>/PIPELINE.md` 작성
- **에이전트 추가**: `agents/<name>.md` frontmatter + 사용 스킬 참조 명시
- **스킬 추가**: `skills/<name>/SKILL.md` (언제/원칙/절차/완료체크)
- **메타-피드백 제안 반영**: `docs/feedback-loop/proposals/` 의 제안 파일 → 사람 승인 → `refactor/feedback/<date>` 브랜치 PR

## 안티패턴 (피해야 할 설계)

`REFACTOR.md` §7 참조. 핵심:
- `/hooks`/`/rules` 디렉토리 분리 (단일 진실 원천 깨짐)
- 로컬 훅에서 LLM 리뷰 동기 호출 (블로킹)
- 단일 만능 리뷰 에이전트
- 메타-리뷰의 .md 직접 수정 (GIGO 누적)
- Stop 훅 재귀 미차단
- 보호 브랜치 upstream 미검증
