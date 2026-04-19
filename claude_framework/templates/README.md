# templates/ — /init 커맨드가 복사할 파일들

이 디렉토리는 **plugin으로 설치된 claude-framework**가 사용자 프로젝트에 `/init` 시 스캐폴드할 파일들을 모아둔다.

## 구조

```
templates/
├── .claude/
│   ├── harness-check.py         # 훅 스크립트
│   ├── settings.json            # PreToolUse/PostToolUse 훅 설정
│   ├── mcp.json                 # MCP 서버 템플릿 (환경변수 자리)
│   ├── git-hooks/pre-commit     # 커밋 훅
│   └── presets/                 # 스택별 harness-rules.json 프리셋
│       ├── kotlin.json          # Kotlin/Spring (완전판)
│       ├── node.json            # Node/TypeScript
│       ├── python.json          # Python
│       └── go.json              # Go
├── .analysis/                   # 파이프라인 정의
│   ├── README.md
│   ├── prd/PIPELINE.md
│   └── project-analysis/PIPELINE.md
└── CLAUDE.md.template           # 플레이스홀더 포함 CLAUDE.md
```

## 스택 프리셋 선택 로직 (/init)

| 감지 신호 | 기본 프리셋 |
|---|---|
| `build.gradle.kts` + `*.kt` | kotlin |
| `package.json` + TS | node |
| `pyproject.toml` / `requirements.txt` | python |
| `go.mod` | go |

**자동 감지 실패 시**: 사용자에게 `--stack=...` 명시 요청.

## CLAUDE.md.template 플레이스홀더

`/init`이 다음 플레이스홀더를 Phase 2/3 결과로 치환:
- `{{PROJECT_NAME}}` → 디렉토리 이름
- `{{STACK_LANGUAGE}}` → 감지 결과
- `{{BUILD_TOOL}}` → Gradle/Maven/npm/pip/go mod
- `{{TEST_FRAMEWORK}}` → Kotest/JUnit/Vitest/pytest
- `{{ARCHITECTURE}}` → Hexagonal/Layered/Flat (convention-detective 판정)
- `{{BE_REPOS}}`, `{{FE_REPOS}}`, `{{DEVOPS_REPOS}}` → repo-classifier 결과
- `{{NAMING_CONVENTIONS}}` → 감지된 접미사 (ApiController, UseCase 등)
- `{{EXISTING_VIOLATIONS_TABLE}}` → convention-detective 위반 카운트

## 사용자 수정 보호

`CLAUDE_FRAMEWORK:BEGIN/END` 마커 사이만 `/init` 재실행 시 갱신. 마커 밖은 사용자 작성 내용 보존.

## 제외 파일 (플러그인 네임스페이스로 노출)

`agents/`, `skills/`, `commands/`, `common/`은 **복사되지 않음** — 플러그인이 설치되면 Claude Code가 `claude-framework:*` 네임스페이스로 자동 노출.

프로젝트별로 오버라이드하려면 `.claude/agents/` 등에 동일 이름으로 파일 생성 → 프로젝트 정의가 플러그인 정의를 덮어씀.
