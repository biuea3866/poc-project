# `.claude/hooks/`

프로젝트 로컬 hook 스크립트 모음. 목적별 하위폴더로 분류되며,
[`_run.sh`](./_run.sh) 한 launcher를 통해 안전하게 호출된다.

> 등록 위치는 [`.claude/settings.json`](../settings.json) 한 곳. **전역(`~/.claude/settings.json`)에는
> 등록하지 않는다** — 한 프로젝트의 디렉토리 이동/삭제가 다른 프로젝트의 hook을 깨뜨리지 않도록 격리한다.

## 구조

```
hooks/
├── _run.sh                  ← graceful launcher (모든 hook이 이걸 거침)
├── README.md
├── code-rules/              ← 코드/명령 패턴 강제 (harness-rules.json 기반)
│   └── harness-check.py
├── verify/                  ← 사후 검증 (PostToolUse)
│   └── subagent-verify.py
├── workflow-gates/          ← 워크플로우 단계 게이트 (Bash)
│   ├── feature-gate.sh
│   ├── feature-tdd-gate.sh
│   ├── push-review.sh       (현재 미등록 — 비용 발생 가능, 옵션)
│   └── push-test.sh
└── util/                    ← 수동 호출용 유틸리티 (hook 아님)
    ├── lockfile-writer.py
    └── resource-resolver.py
```

## `_run.sh` — graceful launcher

모든 hook 호출은 settings.json에서 다음 형식으로 등록한다:

```json
{ "type": "command",
  "command": "bash /abs/.claude/hooks/_run.sh /abs/.claude/hooks/<dir>/<script> [args...]" }
```

`_run.sh`는 다음을 보장한다:

- 대상 스크립트 파일이 **없으면 silently exit 0** (block 안 함)
  → 스크립트 이동/삭제로 인한 환경 잠금 deadlock 방지
- 있으면 정상 실행하고 **exit code를 그대로 전달**
  → exit 2 블로킹 시맨틱 보존
- `.py`는 `python3`로, `.sh`는 `bash`로 launch (확장자 기반)

## 현재 등록된 hook (settings.json 기준)

| Event | Matcher | 스크립트 | 의도 |
|---|---|---|---|
| PreToolUse | `Write\|Edit` | `code-rules/harness-check.py file-guard` | 파일 경로/이름 가드 |
| PreToolUse | `Write\|Edit` | `code-rules/harness-check.py code-pattern` | `harness-rules.json` 금지 패턴 차단 |
| PreToolUse | `Write\|Edit` | `workflow-gates/feature-gate.sh` | main/dev 브랜치 + 파이프라인 미진입 상태 구현 코드 차단 |
| PreToolUse | `Bash` | `code-rules/harness-check.py git-guard` | git 명령 가드 (예: main 직접 push) |
| PreToolUse | `Bash` | `code-rules/harness-check.py bash-file-guard` | bash 명령이 건드리는 파일 가드 |
| PreToolUse | `Bash` | `workflow-gates/feature-tdd-gate.sh` | push 시 src/main 변경 있는데 src/test 변경 없으면 차단 |
| PreToolUse | `Bash` | `workflow-gates/push-test.sh` | push 전 변경된 Gradle 모듈 테스트 자동 실행 |
| PostToolUse | `Agent` | `verify/subagent-verify.py` | 구현 서브에이전트의 push/PR/리뷰어 호출 검증 |

> 미등록: `workflow-gates/push-review.sh` — push 마다 `claude -p`로 자동 PR 리뷰를 돌리는 게이트.
> 비용 발생 가능성 때문에 기본 비활성. 필요 시 settings.json에 위 패턴으로 등록.

## `code-rules/harness-check.py` 서브커맨드

| check_type | 의미 |
|---|---|
| `file-guard` | 작성 대상 파일 경로/이름 규칙 |
| `code-pattern` | 파일 내용 금지 패턴 검사 |
| `git-guard` | git 명령 가드 |
| `bash-file-guard` | bash 명령의 파일 경로 가드 |
| `jira-guard` | 활성 Jira 티켓 컨텍스트 검사 |
| `pipeline-gate` | 파이프라인 단계 진입 조건 검사 |

규칙은 [`harness-rules.json`](../harness-rules.json)에 집중. 스크립트 수정 없이 JSON만 편집.

## `verify/subagent-verify.py` 동작

PostToolUse(Agent) 시점에 호출:

1. 종료된 서브에이전트의 `subagent_type` 확인 — `*implementer`, `tdd-implement`,
   `kotlin-spring-impl` 등 **구현 역할**일 때만 검증 가동.
2. 트랜스크립트(JSONL)를 읽어 해당 서브에이전트의 도구 호출을 추적.
3. 세 항목 누락 여부 판정:
   - **원격 push** — `git push` / `gh pr create`
   - **PR 생성** — `gh pr create` / GitHub MCP `create_pull_request`
   - **리뷰어 호출** — `pr-reviewer` / `code-reviewer` / `be-senior` 등
4. 누락 시 `<system-reminder>` 형태로 메인 세션에 추가 컨텍스트 주입 → 다음 턴에서 강제 처리.

PostToolUse는 block 불가 (도구 이미 실행됨). 관찰·강제는 `additionalContext` 채널.

## 새 hook 추가 절차

1. 목적에 맞는 하위폴더 선택 (또는 새로 생성):
   - **`code-rules/`** — 파일 내용/명령 패턴 자체에 대한 규칙 (`harness-rules.json` 데이터 주도)
   - **`workflow-gates/`** — 워크플로우 단계·브랜치·테스트 조건 (Bash 스크립트가 자연스러움)
   - **`verify/`** — PostToolUse 사후 검증
   - **`util/`** — hook은 아니지만 같이 두면 좋은 유틸리티 (settings.json 등록 X)
2. 스크립트 작성 (`.py` 또는 `.sh`). stdin으로 Claude Code hook payload(JSON) 수신.
   - 차단: stderr 출력 + `exit 2`
   - 통과: `exit 0` (선택적으로 `hookSpecificOutput` JSON을 stdout으로)
3. 실행 권한: `chmod +x .claude/hooks/<dir>/<name>`
4. [`.claude/settings.json`](../settings.json)에 `_run.sh` 래퍼로 등록:
   ```json
   {
     "matcher": "Write|Edit",
     "hooks": [
       { "type": "command",
         "command": "bash /Users/biuea/feature/flag_project/.claude/hooks/_run.sh /Users/biuea/feature/flag_project/.claude/hooks/<dir>/<script>" }
     ]
   }
   ```
5. 문법 검증: `python3 -m py_compile <script.py>` 또는 `bash -n <script.sh>`.

## 격리 원칙

- 전역 hook 금지. 모든 hook은 프로젝트 내부에서 닫혀야 한다.
- hook 스크립트는 자신이 사는 디렉토리(`__file__`) 기준으로 경로를 풀어 프로젝트 이동에 견고하게.
- 외부 의존성(MCP 토큰, 사용자명 등)은 하드코딩 금지. 환경변수나 `settings.local.json`에서.
- 스크립트 이동/리네임 전에 settings.json을 먼저 업데이트 — `_run.sh`가 deadlock을 막아주지만, 그래도 좋은 습관.
