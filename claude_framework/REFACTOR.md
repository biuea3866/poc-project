# 아키텍처 리팩토링

`agents`, `skills`, `commands`, `rules`, `hooks` 가 3계층(Command → Agent → Skill) 단방향 의존으로 동작하는 **다층 피드백 루프** 구조를 구축한다.

> **3계층으로 통합** (2026-05-10): 기존 4계층의 Pipeline 층(`.analysis/` 또는 `pipelines/`)은 별도 디렉토리로 분리돼 있었으나, command ↔ pipeline 매핑이 1:1 이거나 1:0 인 경우가 대부분이라 **Command 본문에 PIPELINE 절차를 인라인** 하는 방식으로 통합. 산출물은 `outputs/<command-name>/` 으로 분리해 절차서와 결과물을 깔끔히 분리.

## 1. 설계 원칙

1. **단일 진실 원천(Single Source of Truth)**: 룰은 `harness-rules.json` 한 파일. 디렉토리로 분산하지 않는다 (Sprint 4 silent-pass 사건 방지).
2. **단방향 의존**: Command → Pipeline → Agent → Skill. 역방향 참조 금지.
3. **계층별 책임 분리**:
   - Command = "사용자 트리거" (UX)
   - Pipeline = "무엇을, 어떤 순서로"
   - Agent = "누가" (페르소나 + 모델 + 도구)
   - Skill = "어떻게" (절차/체크리스트/템플릿)
   - Rule = "하지 말 것" (금지 패턴)
4. **다층 방어**: 로컬 훅(0.5s 정적) → pre-commit → CI Senior Gate(LLM) → nightly 전수 감사. 한 층이 무력화돼도 다음 층이 잡는다.
5. **모델·비용 분리**: Opus(분석/리뷰), Sonnet(구현), Haiku(체커/포맷).
6. **루프 안전장치**: 자동 재시도 N회 + 토큰/비용 상한 + 사람 개입 트리거.
7. **메타-피드백**: 코드 산출물뿐 아니라 그 산출물을 만든 prompt(command/skill/rule) 자체도 리뷰 대상. 단, **자동 .md 수정 금지 — 제안 파일 → 사람 승인 → PR** 경로만 허용.

## 2. 디렉토리 구조

```
claude_framework/
├── .claude/
│   ├── settings.json                    # hooks 등록 (PreToolUse / PostToolUse / pre-commit)
│   ├── settings.local.json              # 개인 퍼미션 오버라이드 (gitignore)
│   ├── harness-rules.json               # ★ 단일 룰 진실 원천
│   ├── mcp.json                         # MCP 서버 설정
│   ├── scripts/
│   │   ├── harness-check.py             # PreToolUse 정적 검사 (0.5초 차단)
│   │   ├── harness-audit.py             # CI/nightly 전수 감사
│   │   ├── senior-gate.py               # PR open 시 Critical 4유형 LLM-free 검출
│   │   └── qa-followup-extract.py       # QA 보고서 → Issue 자동 발행 파서
│   ├── agents/                          # 페르소나 + 모델 + 도구
│   │   ├── main-orchestrator.md         # opus, 작업 분해/티켓화 전담
│   │   ├── be-implementer.md            # sonnet, BE 구현
│   │   ├── fe-implementer.md            # sonnet, FE 구현
│   │   ├── pr-reviewer.md               # opus, 룰셋·레이어 경계
│   │   ├── be-senior.md                 # opus, 운영 리스크·엣지 케이스
│   │   ├── be-tech-lead.md              # opus, 서비스 경계·데이터 소유권
│   │   ├── security-reviewer.md         # opus, OWASP·@RoleRequired
│   │   ├── feedback-loop-guardian.md    # sonnet, 루프 건강 점검 + 제안 파일 PR화
│   │   ├── process-reviewer.md          # sonnet, Stop/SubagentStop 훅에서 호출 (메타-피드백)
│   │   └── ticket-splitter.md           # sonnet, 1명/1일/1PR 단위 분해
│   ├── skills/                          # "어떻게" 재사용 절차
│   │   ├── tdd-loop/SKILL.md
│   │   ├── kotlin-spring-impl/SKILL.md
│   │   ├── pr-review-checklist/SKILL.md
│   │   ├── ticket-breakdown/SKILL.md
│   │   └── ...
│   └── commands/                        # 사용자 트리거 슬래시 커맨드
│       ├── analyze-prd.md
│       ├── plan-project.md
│       ├── tdd-implement.md
│       ├── review-pr.md
│       ├── audit-harness.md
│       └── parallel-tickets.md
├── commands/ + outputs/                           # 파이프라인 (무엇을, 어떤 순서로)
│   ├── prd/PIPELINE.md
│   ├── project-analysis/PIPELINE.md
│   ├── pr-review/PIPELINE.md
│   ├── feedback-loop/PIPELINE.md
│   ├── incident/PIPELINE.md
│   ├── refactoring/PIPELINE.md
│   └── ...
├── docs/
│   └── feedback-loop/
│       └── proposals/                   # process-reviewer 가 생성하는 메타-피드백 제안
│           ├── <YYYYMMDD>-<topic>.md    # 자동 .md 수정 금지 — 사람 검토용 append-only
│           ├── archived/                # 보류된 제안
│           └── closed/                  # 기각된 제안 (이유 기록)
└── .github/workflows/                   # 원격 다층 방어
    ├── pr-senior-review.yml             # PR open/sync, Critical 차단
    ├── harness-audit.yml                # nightly 03:00 KST 전수 감사 + silent-pass 감지
    └── qa-followup-tickets.yml          # docs/qa/*.md push → Issue
```

## 3. 3계층 의존 (Command → Agent → Skill)

| 계층 | 책임 | 위치 | 참조 가능 대상 |
|------|------|------|----------------|
| Command | 사용자 트리거 + 절차서 (인라인 PIPELINE) | `commands/<name>.md` | Agent, Skill, Rule |
| Agent | 누가 (페르소나 + 모델 + 도구) | `agents/<name>.md` | Skill, Rule |
| Skill | 어떻게 (절차/체크리스트/템플릿) | `skills/<name>/SKILL.md` | Rule |
| Rule | 하지 말 것 + 컨벤션 | `.claude/harness-rules.json` + `rules/*.md` | (참조만 됨) |

산출물 위치: `outputs/<command-name>/<date>-<topic>/` — 절차서(commands)와 결과물(outputs) 분리.

역방향 의존(Skill → Agent, Agent → Command 등) 금지.

## 4. 워크플로우

```
1. 사용자 → /<command> 호출
   예) /analyze-prd, /tdd-implement, /review-pr

2. Command 본문 → 해당 Pipeline 진입
   - PIPELINE.md 의 단계별 지시를 순차 수행
   - 산출물 경로 자동 관리 (outputs/<name>/<date>/)

3. Pipeline → 단계별 Agent 스폰
   - 모델 명시 (opus/sonnet/haiku)
   - 병렬 가능한 단계는 동시 스폰 (예: be-senior + security-reviewer)
   - 각 Agent 는 Skill(절차) + Rule(금지 패턴) 참조

4. Agent 작업 중 — 로컬 다층 방어
   a. PreToolUse 훅 (harness-check.py): 0.5초 정적 차단
      - file-guard: .env / credentials 차단
      - code-pattern: @Query, LocalDateTime, FK 등 금지 패턴
      - git-guard: --force / main 직접 push 차단
   b. pre-commit 훅: 변경 파일만 빠른 검사
   c. PostToolUse 훅: 빌드 실패 로그 감지

5. Agent 완료 → main-orchestrator 검수 또는 사용자 확인

6. 푸시·PR 생성 후 — 원격 다층 방어
   a. GitHub Actions: harness-rules.json JSON 유효성 우선 검증 (silent-pass 방지)
   b. pr-senior-review.yml 실행
      - senior-gate.py: Critical 4유형 LLM-free 검출
        · Admin/관리자 엔드포인트 @RoleRequired 누락
        · HttpServletRequest 직접 주입
        · Listener 의 Repository 직접 호출
        · UseCase @Transactional 누락
      - Critical 발견 → REQUEST_CHANGES + workflow fail
   c. pr-reviewer / be-senior / be-tech-lead / security-reviewer 병렬 스폰
      - 각자 관점으로 리뷰 코멘트
      - 결과 병합 후 verdict (approve / request-changes / comment)

7. 리뷰 실패 시 피드백 루프
   - 자동 재시도 최대 N회 (기본 3회, command 인자로 조정)
   - 토큰/비용 상한 도달 시 즉시 중단
   - 의도적 위반(예: legacy 호환) 은 PR description 의 ALLOW 블록으로 명시 가능
   - N회 초과 시 사람 개입 요청 (Slack/Issue)

8. 머지 후 — 백그라운드 검증
   a. nightly harness-audit (03:00 KST): 전수 감사
   b. 어제까지 통과했는데 오늘 위반 검출 → silent-pass 의심 → Issue 자동 발행
   c. docs/qa/*.md push → qa-followup-extract.py 가 "후속 티켓" 섹션 파싱 → Jira/Issue 발행

9. 메인/서브 에이전트 종료 시 — 프로세스 리뷰 (메타-피드백 루프)
   목적: command/skill/rule 자체가 잘못 작성돼 모든 에이전트가 같은 방향으로
        잘못 가는 "프롬프트 드리프트" 를 잡는다.
   a. Stop / SubagentStop 훅 발화 — stop_hook_active 가드 필수 (재귀 차단)
   b. 트리거 조건 — 아래 중 하나라도 true 일 때만 process-reviewer 스폰
      · 직전 작업이 PR Senior Gate 에서 fail
      · 자동 재시도 N회 중 1회 이상 실패
      · 룰 위반이 PreToolUse 훅에서 차단됨
      · QA 보고서에 "후속 티켓" 항목이 추가됨
   c. process-reviewer 에이전트 스폰 (sonnet, SubagentStop 비활성)
      · 입력: 작업 로그 + 참조한 command/skill/rule 경로 + 산출물(PR diff, QA 문서)
      · 비교 기준 (이외는 판정 근거 부족 — 단순 LLM 의견 추가 금지):
        - PRD/ADR (outputs/<name>/)
        - harness-rules.json
        - 이전 머지된 패턴
      · 출력: docs/feedback-loop/proposals/<YYYYMMDD>-<topic>.md
        - 어떤 prompt(command/skill/rule) 가 어떤 실패를 유발했는지
        - 권장 수정 (diff 형식)
        - 위험도(low/med/high) + 영향 범위
        - 메타: trigger 조건, 세션 ID, 관련 PR
   d. ★ 자동 .md 수정 절대 금지 — 제안 파일만 생성
   e. 일일 발화 상한 (기본 5회) 초과 시 즉시 중단

10. 제안 파일 검토 → main-orchestrator 또는 사용자가
    · 승인: feedback-loop-guardian 이 PR 생성 (브랜치: refactor/feedback/<date>)
    · 보류: 제안 파일 docs/feedback-loop/proposals/archived/ 로 이동
    · 기각: docs/feedback-loop/proposals/closed/ 로 이동, 이유 기록

11. 피드백 PR 머지 후 → 다음 세션부터 갱신된 prompt 적용
    · feedback-loop-guardian 이 머지 후 nightly 점검에서 효과 측정
      - 같은 실패 유형 재발 빈도
      - 제안 → 머지 → 재발 감소 비율
    · 효과 미흡 시 추가 제안 생성 (자기 보정)
```

## 5. 리뷰 에이전트 분리 전략

리뷰 책임을 두 축으로 분리:

### 5.1 출력물 리뷰 (산출물 = PR diff, 문서)
PR open/sync 시 GitHub Actions 가 병렬 스폰:

| 에이전트 | 모델 | 관점 | 주요 체크 |
|----------|------|------|-----------|
| `pr-reviewer` | opus | 룰셋·레이어 경계 | harness-rules 위반, Controller→Facade→Service 흐름 |
| `be-senior` | opus | 운영 리스크 | 트랜잭션 위치, 동시성, 멱등성, 로깅 |
| `be-tech-lead` | opus | 시스템 설계 | 서비스 경계, 데이터 소유권, 마이그레이션 |
| `security-reviewer` | opus | 보안 | OWASP, 인증·인가, 입력 검증, 시크릿 |
| `fe-lead` | opus | FE 아키텍처 | BFF Facade 경유, 상태관리, 디자인 시스템 |

### 5.2 프로세스 리뷰 (산출물 = 참조한 command/skill/rule 자체)
Stop/SubagentStop 훅에서 트리거 조건 충족 시에만 스폰:

| 에이전트 | 모델 | 관점 | 주요 체크 |
|----------|------|------|-----------|
| `process-reviewer` | sonnet | 프롬프트 드리프트 | 잘못 작성된 command/skill/rule 가 실패 유발했는지, 권장 수정안 제안 |
| `feedback-loop-guardian` | sonnet | 루프 건강·효과 측정 | 제안 PR 머지 후 재발률, 일일 발화 상한, 자동 .md 수정 시도 감지 |

> 5.2 의 두 에이전트는 **.md 직접 수정 권한이 없다.** `docs/feedback-loop/proposals/` 에 제안 파일만 생성하고, 사람·main-orchestrator 검토 후 PR 로만 반영한다.

## 6. 모델·비용 분리

| 역할 | 모델 | 이유 |
|------|------|------|
| 메인 오케스트레이터 / 설계 / 리뷰 | Opus | 복잡한 추론, 시스템 관점 |
| 구현 (BE/FE/DevOps) | Sonnet | 비용·속도 균형 |
| 단순 체커 / 포맷 / 상태 갱신 | Haiku | 대량 호출 최적화 |

> 메인 에이전트의 모델은 사용자가 `/model` 로 선택하는 영역이라 강제할 수 없다. 서브 에이전트만 frontmatter `model:` 로 고정한다.

## 7. 안티패턴 (피해야 할 설계)

- **`/hooks` 디렉토리에 훅 로직 분산**: 단일 진입점(`.claude/settings.json` + `harness-check.py`) 깨짐. silent-pass 위험.
- **`harness-rules.json` 분산**: 금지 룰을 여러 JSON 파일로 쪼개면 단일 진실 원천이 깨지고 silent-pass 가 더 잘 일어남. (참고: 디렉토리 이름 자체는 무관 — 공유 가이드/컨벤션 마크다운 모음으로 `rules/` 디렉토리 사용은 허용. 핵심은 `harness-rules.json` 의 룰을 분산하지 않는 것.)
- **로컬 훅에서 LLM 리뷰 동기 호출**: PreToolUse 가 `git push` 가로채 LLM 호출 시 30초~수분 블로킹. 무거운 리뷰는 GitHub Actions 로 비동기.
- **Skill 안에 Rule 섞기**: Skill 은 "어떻게(절차)", Rule 은 "하지 말 것". 섞이면 우선순위 충돌.
- **단일 만능 리뷰 에이전트**: 분야별 깊이 부족. 병렬 분리가 효과적.
- **메인 에이전트 모델 강제**: 사용자가 `/model` 로 선택. 강제 불가.
- **무한 피드백 루프**: 재시도 횟수·비용 상한 없으면 비용 폭발.
- **Stop 훅 재귀 미차단**: 메타-피드백 루프에서 `stop_hook_active` 가드 없으면 리뷰 에이전트가 또 Stop 을 발화시켜 무한 호출. SubagentStop 도 같은 위험.
- **리뷰 에이전트의 .md 직접 수정**: prompt 를 LLM 이 LLM 출력을 보고 직접 고치면 GIGO 가 누적되고 추적 불가. 반드시 `docs/feedback-loop/proposals/` 의 제안 파일만 생성하고 사람·main-orchestrator 검토 후 PR 로 반영.
- **샘플링 없는 메타-리뷰**: 모든 Stop 이벤트마다 process-reviewer 발화하면 토큰 폭발 + 메인 + 서브 4개 동시 종료 시 4번 동시 호출. 트리거 조건(fail/재시도/룰차단/QA 후속티켓) + 일일 발화 상한 필수.
- **메타-리뷰의 비교 기준 부재**: PRD/ADR/harness-rules 같은 명시 기준 없이 "다른 LLM 의견" 만으로 판정하면 단순 의견 추가에 불과. 비교 근거 명시 강제.
- **제안 파일 자동 머지**: 자동 PR 머지하면 며칠 뒤 추적 불가. 항상 사람 승인 단계 거치고 commit 메타에 trigger 정보 기록.
- **feature 브랜치 upstream 을 보호 브랜치로 설정**: feature 브랜치에 있어도 upstream 이 `origin/main`/`origin/dev` 로 잘못 설정돼 있으면 인자 없는 `git push` 가 보호 브랜치로 직행. `git_upstream_guard` 동적 검사 + 정적 패턴(`--set-upstream-to`, `-u origin main`, refspec `HEAD:main`, `--all`, `--mirror`, `push.default=matching`) 으로 다층 차단.

## 8. 마이그레이션 단계

1. **현 구조 점검**: 기존 `agents/`, `commands/`, `skills/`, `templates/` 자산 인벤토리.
2. **Pipeline 정리**: `outputs/<name>/PIPELINE.md` 누락분 추가 (incident, refactoring, release 등).
3. **Agent 분리**: 단일 reviewer → pr-reviewer / be-senior / be-tech-lead / security-reviewer.
4. **Command 진입점 정비**: 사용자가 자주 쓰는 흐름을 `/analyze-prd`, `/tdd-implement`, `/review-pr` 등으로 일원화.
5. **Workflow 다층화**: nightly harness-audit + PR senior-gate + qa-followup 3개 워크플로우 추가.
6. **JSON 유효성 게이트**: 모든 워크플로우 맨 앞에 `python3 -c 'import json; json.load(...)'` 추가 (silent-pass 방지).
7. **재시도/비용 상한**: feedback-loop-guardian 에 상한 로직 추가.
8. **메타-피드백 인프라**: process-reviewer 에이전트 작성 + Stop/SubagentStop 훅 등록 (`stop_hook_active` 가드 필수) + `docs/feedback-loop/proposals/` 디렉토리 + 일일 발화 상한.
9. **문서 일치**: CLAUDE.md / README.md / ADOPTION.md 의 구조 설명을 신규 구조에 맞춰 갱신.

## 9. 성공 기준

- 새 프로젝트가 `install.sh` 한 번으로 4계층 구조 + 다층 방어 + 분리된 리뷰 에이전트를 모두 받음.
- harness-rules.json JSON 콤마 누락 같은 silent-pass 가 nightly 워크플로우에서 즉시 감지됨.
- 보안 Critical(예: @RoleRequired 누락) 이 PR open 즉시 차단됨.
- 단일 만능 리뷰 대신 분야별 병렬 리뷰로 검출 깊이↑.
- 피드백 루프가 재시도 상한·비용 상한 안에서 종료 (무한 루프 방지).
- 메타-피드백 루프가 동작: 같은 종류 실패가 반복될 때 process-reviewer 가 제안 파일을 생성하고, 사람 승인 후 PR 머지로 prompt 가 갱신되며, 다음 세션에서 동일 실패 재발률이 측정 가능하게 감소.
- Stop 훅 재귀나 자동 .md 수정 같은 위험 패턴이 발생하지 않음 (회귀 테스트로 검증).
