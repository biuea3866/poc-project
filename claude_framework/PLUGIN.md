# claude-framework 플러그인 사용법

Claude Code의 네이티브 플러그인으로 claude-framework를 설치하면, 어느 프로젝트에서든 `/init` 한 줄로 4계층 구조를 이식할 수 있다.

---

## 설치

### 방법 1 — 플러그인 마켓플레이스 (향후)
```
/plugin install claude-framework
```

### 방법 2 — Git URL 직접 설치
```bash
# Claude Code 설정 디렉토리에 클론
mkdir -p ~/.claude/plugins
cd ~/.claude/plugins
git clone https://github.com/biuea3866/poc-project.git claude-framework-src

# claude_framework 하위만 심볼릭 링크로 플러그인화
ln -s claude-framework-src/claude_framework claude-framework
```

또는 **sparse checkout**:
```bash
cd ~/.claude/plugins
git clone --filter=blob:none --sparse https://github.com/biuea3866/poc-project.git claude-framework
cd claude-framework
git sparse-checkout set claude_framework
mv claude_framework/* . && rm -rf claude_framework
```

### 설치 확인
Claude Code 재시작 후:
```
/plugin list
# → claude-framework v1.1.0 표시되어야 함
```

플러그인 제공 요소 (자동 네임스페이스):
- 에이전트: `claude-framework:prd-analyst`, `claude-framework:be-tech-lead`, ...
- 스킬: `claude-framework:prd-analysis`, `claude-framework:tdd-loop`, ...
- 커맨드: `/init`, `/analyze-prd`, `/parallel-tickets`, ...

---

## 사용: /init

### 시나리오 1 — 신규 프로젝트
```bash
mkdir my-new-service && cd my-new-service
git init
# Claude Code 실행 후
/init --stack=kotlin
# → .claude/, .analysis/, CLAUDE.md 스캐폴드 완료
```

### 시나리오 2 — 기존 프로젝트 이식 (컨벤션 스캔 포함)
```bash
cd existing-kotlin-project
# Claude Code 실행 후
/init --scan
# → 기존 코드 샘플링 → 컨벤션 추출 → 룰 델타 제안 → 승인 → 주입
```

### 시나리오 3 — 멀티 레포 워크스페이스
```bash
cd doodlin-workspace  # 여러 레포가 하위에 있는 루트
/init --scan --classify-repos
# 동시에:
#   1. 기본 스캐폴드
#   2. 기존 컨벤션 추출
#   3. 하위 레포 BE/FE/DevOps 자동 분류 + 심볼릭 링크 제안
```

### 시나리오 4 — 완전 자동 (주의)
```bash
/init --scan --classify-repos --auto
# → 사용자 승인 없이 제안을 전부 자동 적용
# → 실패 시 자동 롤백. 처음 쓸 때는 권장하지 않음.
```

---

## 플래그 참조

| 플래그 | 동작 |
|---|---|
| `--stack=<kotlin\|node\|python\|go\|auto>` | 스택 지정. `auto`(기본)는 빌드 파일로 감지. |
| `--scan` | 기존 코드베이스 스캔 → 룰 델타 제안 → 승인 후 `harness-rules.json`에 주입 |
| `--classify-repos` | 하위 1-level 디렉토리를 BE/FE/DevOps/Data/Mobile로 분류 → 심볼릭 링크 또는 매니페스트 생성 |
| `--auto` | 사용자 승인 건너뛰고 자동 적용 (스모크 테스트 실패 시 롤백) |
| `--force` | 기존 `.claude/` 덮어쓰기 |
| `--dry-run` | 실제 파일 변경 없이 계획만 보기 |

---

## /init 내부 동작

```
┌── Phase 0: 전제 확인 ──┐
│ - git repo 확인          │
│ - 기존 .claude/ 체크     │
│ - 스택 감지              │
└──────────┬─────────────┘
           ↓
┌── Phase 1: 기본 스캐폴드 ──┐
│ 플러그인의 templates/ 복사: │
│  .claude/harness-check.py   │
│  .claude/settings.json      │
│  .claude/mcp.json           │
│  .claude/git-hooks/         │
│  .claude/presets/<stack>.json → harness-rules.json │
│  .analysis/                  │
│  CLAUDE.md (없으면)          │
│                              │
│ ⚠️ agents/skills/commands는   │
│    복사 안 함. 플러그인       │
│    네임스페이스로 제공.        │
└──────────┬─────────────────┘
           ↓
┌── Phase 2: --scan (선택) ──┐
│ Agent(convention-detective) │
│  ├─ 샘플링 (카테고리별 5-10) │
│  ├─ 6종 패턴 추출            │
│  ├─ 룰 델타 JSON 생성        │
│  └─ 사용자 승인 요청          │
│ 승인 → harness-rules.json 병합 │
│ 백업 + 스모크 테스트          │
└──────────┬─────────────────┘
           ↓
┌── Phase 3: --classify-repos (선택) ──┐
│ Agent(repo-classifier)                 │
│  ├─ 1-level 서브디렉토리 스캔          │
│  ├─ 파일 시그니처로 카테고리 점수화     │
│  ├─ BE/FE/DevOps/Data/Mobile 분류       │
│  └─ 심볼릭 링크 스크립트 또는 매니페스트 제안 │
└──────────┬─────────────────────────────┘
           ↓
┌── Phase 4: CLAUDE.md 생성 ──┐
│ 플레이스홀더 치환:            │
│  {{STACK_LANGUAGE}}, {{BE_REPOS}},│
│  {{NAMING_CONVENTIONS}}, ...     │
│ CLAUDE_FRAMEWORK:BEGIN/END 마커  │
└──────────┬────────────────────┘
           ↓
┌── Phase 5: 최종 보고 ──┐
│ 다음 단계 안내:          │
│  1. MCP 환경변수         │
│  2. settings.local.json  │
│  3. git core.hooksPath   │
└─────────────────────────┘
```

---

## 컨벤션 스캔이 만드는 것

### 네이밍 감지 예시
`src/`에 `~ApiController.kt` 18건, `~UseCase.kt` 24건 발견:

```json
// harness-rules.json의 architecture_convention.naming_convention 갱신
{
  "controller": "~ApiController",
  "usecase": "~UseCase"
}
```

### 레이어 구조 감지
`domain/ application/ infrastructure/ presentation/` 전부 존재 → Hexagonal 확정:

```json
// layer_dependency 룰 자동 활성화
{
  "rules": [
    { "id": "no-infra-in-domain", "file_glob": "*/domain/**/*.kt", ... },
    ...
  ]
}
```

### 기존 위반 처리 예시
`LocalDateTime` 3건 기 존재:
```json
{
  "id": "no-local-datetime",
  "severity": "warning",      // error가 아닌 warning으로 도입
  "exclude_glob": "src/main/kotlin/legacy/TimeUtil.kt,..."  // 기존 파일 제외
}
```
→ 신규 코드는 자동 차단, 기존 3건은 별도 마이그레이션 티켓으로 정리 권장.

### 도메인 경계 자동 생성
`domain/` 하위에 `rental/`, `product/`, `user/`, `notification/` 발견:
```json
// 4개 cross-domain-import 룰 자동 생성
{ "id": "no-cross-domain-import-rental", "file_glob": "*/domain/rental/**/*.kt", ... }
{ "id": "no-cross-domain-import-product", ... }
...
```

---

## 저장소 분류가 만드는 것

### 심볼릭 링크 방식 (기본)
```
workspace/
├── claude_framework/          ← 이 플러그인은 설치되어 있음
├── be-repos/
│   ├── rental-commerce -> ../../rental-commerce      (링크)
│   └── notification -> ../../notification
├── fe-repos/
│   └── next-greeting -> ../../next-greeting
└── devops-repos/
    └── infra-terraform -> ../../infra-terraform
```

### 매니페스트 방식 (대안)
`be-repos/.mounts.json`:
```json
{
  "mounts": [
    { "name": "rental-commerce", "path": "../../rental-commerce", "category": "be" }
  ]
}
```

→ `.claude/settings.local.json`의 `permissions.additionalDirectories`에 경로 주입.

---

## 재실행 (업데이트)

`/init`은 **멱등성 보장**. 재실행 시:
- 기본 스캐폴드: 이미 있으면 skip (`--force`로 덮어쓰기)
- `--scan`: 현재 코드베이스 재스캔 → 델타만 제안
- `--classify-repos`: 새 저장소 있으면 추가 제안
- `CLAUDE.md`: `CLAUDE_FRAMEWORK:BEGIN/END` 마커 내부만 갱신, 마커 밖 사용자 내용 보존

---

## 트러블슈팅

**Q. `/init` 했는데 하네스가 안 걸립니다.**
A. `.claude/settings.json`의 훅 경로가 `${CLAUDE_PROJECT_DIR}/.claude/harness-check.py`인지 확인. 서브디렉토리로 설치했다면 경로 수정 필요.

**Q. 스캔이 너무 많은 룰을 제안합니다.**
A. 빈도 임계치가 기본 3회. `--scan --threshold=5` 같은 플래그는 아직 없지만, 제안 받은 뒤 항목별 y/n/skip으로 거절 가능.

**Q. `--classify-repos`가 프로젝트를 못 찾습니다.**
A. `.git/` 또는 빌드 파일이 없는 디렉토리는 제외됨. 수동으로 `be-repos/.mounts.json`에 추가하거나 `README.md`라도 넣고 재실행.

**Q. 플러그인 업데이트는?**
A. `cd ~/.claude/plugins/claude-framework && git pull`. 플러그인 자체 업데이트 후 각 프로젝트에서 `/init` 재실행하면 새 룰/에이전트 반영.

---

## 개발자용: 플러그인 구조

```
claude_framework/
├── .claude-plugin/
│   └── plugin.json           # 매니페스트
├── .claude/
│   ├── agents/               # 플러그인이 제공하는 에이전트
│   ├── skills/               # 플러그인이 제공하는 스킬
│   ├── commands/             # 슬래시 커맨드 (/init 포함)
│   ├── common/               # 공통 가이드 문서
│   ├── harness-rules.json    # 플러그인 자체 개발용 (dogfood)
│   ├── harness-check.py      # 플러그인 자체 개발용
│   └── settings.json         # 플러그인 자체 개발용
├── templates/                # /init이 복사할 파일들
│   ├── .claude/
│   │   ├── harness-check.py
│   │   ├── settings.json
│   │   ├── mcp.json
│   │   ├── git-hooks/pre-commit
│   │   └── presets/          # 스택별 harness-rules.json
│   ├── .analysis/
│   └── CLAUDE.md.template
├── .analysis/                # 플러그인 자체 개발용 (dogfood)
├── CLAUDE.md
├── README.md
├── ADOPTION.md               # 수동 이식 가이드 (플러그인 없을 때)
└── PLUGIN.md                 # 이 파일
```

**dogfood 철학**: 플러그인 자체도 claude_framework로 개발 — `.claude/`와 `.analysis/`가 플러그인 루트에도 존재. 플러그인 기여자는 `/parallel-tickets`, `/review-pr` 등을 플러그인 개발 자체에 사용.
