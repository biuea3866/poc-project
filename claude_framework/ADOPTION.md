# 다른 프로젝트에 claude_framework 이식 가이드 (수동 방식)

> 플러그인 방식으로 `/init` 한 줄 설치가 가능하면 그걸 먼저 권장합니다. → **[PLUGIN.md](./PLUGIN.md)**
>
> 이 문서는 플러그인 인프라 없이 복사로 이식하거나, 커스터마이징을 완전히 장악하고 싶을 때 참조하세요.

## 3단계 요약

```
1. 복사    → 2. 프로젝트화    → 3. 룰 커스터마이징
  (복제)       (CLAUDE.md)       (harness-rules.json)
```

---

## 1단계: 복사

### 방법 A — 서브디렉토리 (기존 repo에 얹기)
```bash
cd <your-project-root>
cp -r /Users/biuea/feature/flag_project/claude_framework ./
```

### 방법 B — 프로젝트 루트를 통째로 시작
```bash
cp -r /Users/biuea/feature/flag_project/claude_framework my-new-project
cd my-new-project
git init && git add . && git commit -m "chore: bootstrap with claude_framework"
```

### 무엇을 복사하는가
- `.claude/` — 하네스/에이전트/스킬/커맨드/공통가이드 (필수)
- `.analysis/` — 파이프라인 정의 (필수)
- `CLAUDE.md` — 프로젝트 가이드 (수정 필요)
- `README.md`, `ADOPTION.md` — 문서 (선택)

### 제외할 것
- `be-repos/`, `fe-repos/`, `devops-repos/` — 빈 디렉토리. 실제 레포를 마운트할 때 생성.
- `.claude/settings.local.json` — 개인 퍼미션. gitignore 대상.

---

## 2단계: 프로젝트화

### 2-1. `CLAUDE.md` 재작성 (반드시)

템플릿 CLAUDE.md는 일반론이다. **프로젝트 특화 정보를 추가**해야 한다:

```markdown
# <Project Name>

## 플랫폼 개요
<제품 소개, 비즈니스 도메인>

## 기술 스택
| 영역 | 기술 |
|---|---|
| BE | <Kotlin/Node/Python/Go> |
| DB | <MySQL/Postgres/MongoDB> |
| ... | ... |

## 개발 환경
- JAVA_HOME=<...>
- 필요한 CLI: <gh, gradlew, docker, ...>

## 레포 맵
| 레포 | 역할 |
|---|---|
| ... | ... |

## 도메인 용어
| 용어 | 의미 |
|---|---|
| ... | ... |

---

## Claude 프레임워크 사용
이 저장소는 `claude_framework`를 사용한다. 상세는 [CLAUDE.md](./claude_framework/CLAUDE.md) 참조.
```

### 2-2. MCP 설정

```bash
# .claude/mcp.json의 환경변수 채우기
# 또는 ~/.zshrc에 export
export ATLASSIAN_SITE_URL="https://your-org.atlassian.net"
export ATLASSIAN_EMAIL="you@your-org.com"
export ATLASSIAN_API_TOKEN="..."
export NOTION_API_TOKEN="..."
export GITHUB_PERSONAL_ACCESS_TOKEN="..."
```

### 2-3. `settings.local.json` 생성 (개인 퍼미션)

`.claude/settings.local.json`은 gitignore 대상. 개인 퍼미션은 여기에:

```bash
cp .claude/settings.local.json.example .claude/settings.local.json  # 있으면
# 없으면 직접 생성
```

### 2-4. Git 훅 연결 (선택)

```bash
git config core.hooksPath claude_framework/.claude/git-hooks
# or (루트에 복사한 경우)
git config core.hooksPath .claude/git-hooks
```

---

## 3단계: 룰 커스터마이징

### 3-1. `harness-rules.json` 수정

**기본은 Kotlin/Spring 편향**. 다른 스택은 룰을 재작성하거나 제거해야 한다.

#### 스택별 체크리스트

| 스택 | 유지할 룰 | 제거할 룰 | 추가할 룰 |
|---|---|---|---|
| **Kotlin/Spring** | 전부 유지 | — | 프로젝트 특화 네이밍 |
| **Node/TypeScript** | file_guard, git_guard | `*.kt` 룰 전부, `layer_dependency` | `any` 타입 금지, `console.log` 금지 |
| **Python** | file_guard, git_guard | Kotlin 룰 | `print(` 금지, `except:` 맨 사용 금지 |
| **Go** | file_guard, git_guard | Kotlin 룰 | `panic(` 금지, `interface{}` 금지 |

#### 커스텀 룰 추가 예시 (TypeScript)

```json
{
  "forbidden_patterns": {
    "rules": [
      {
        "id": "no-any-type",
        "pattern": ":\\s*any[\\s,)>]",
        "file_glob": "*.ts",
        "message": "any 타입 금지 — unknown + 타입 가드 사용",
        "severity": "error"
      },
      {
        "id": "no-console-log",
        "pattern": "console\\.(log|info|debug)",
        "file_glob": "src/**/*.ts",
        "exclude_glob": "**/*.test.ts",
        "message": "console.log 금지 — logger 사용",
        "severity": "warning"
      }
    ]
  }
}
```

### 3-2. 도메인 특화 룰 추가

프로젝트에 맞는 규칙을 `forbidden_patterns.rules`에 추가:

```json
{
  "id": "no-hardcoded-workspace-id",
  "pattern": "workspaceId\\s*=\\s*\\d+",
  "file_glob": "*.kt",
  "message": "workspaceId 하드코딩 금지 — 테스트 fixture 사용",
  "severity": "error"
}
```

### 3-3. `be-code-convention.md` 수정

`.claude/common/be-code-convention.md`는 **rental-commerce 기반 예시**. 프로젝트 도메인명(rental, product, user)을 자신의 도메인으로 치환하거나, 불필요하면 삭제.

### 3-4. 보호 브랜치 조정

`harness-rules.json`의 `git_guard.blocked_commands`에서 보호 브랜치 이름 수정:
- `main/master/dev` → 프로젝트 브랜치 전략에 맞게

---

## 4단계: 에이전트/스킬 조정

### 4-1. 필요 없는 에이전트 삭제

기본 8종 중 프로젝트와 무관한 것 제거:
- FE 없으면 `fe-implementer`, `fe-lead` 삭제
- 단일 언어 프로젝트면 페르소나 줄이기

### 4-2. 프로젝트 특화 에이전트 추가

예: 결제 도메인 프로젝트라면 `payment-reviewer.md`:
```markdown
---
name: payment-reviewer
description: 결제 로직 전용 리뷰어 — PCI-DSS, 멱등성, 이중 결제 방지, 환불 정책 검증
model: opus
---
...
```

### 4-3. 파이프라인 추가

기본은 `prd/`, `project-analysis/` 2종. 필요에 따라:
- `be-refactoring/` — PRD 없는 기술 부채 작업 (rental-commerce 참고)
- `incident/` — 장애 대응
- `inquiry/` — 문의/버그 처리
- `pr-review/` — 독립 리뷰 파이프라인

---

## 5단계: 검증

### 5-1. 하네스 스모크 테스트
```bash
cd <project-root>  # claude_framework가 루트인 경우
# 또는 cd <project-root>/claude_framework

# 1. 금지 패턴 차단 확인
CLAUDE_TOOL_INPUT='{"file_path":"test.kt","content":"val now = LocalDateTime.now()"}' \
  python3 .claude/harness-check.py code-pattern
# 기대: exit 2 + 메시지

# 2. git-guard 확인
CLAUDE_TOOL_INPUT='{"command":"git push origin main --force"}' \
  python3 .claude/harness-check.py git-guard
# 기대: exit 2
```

### 5-2. Claude Code 실행 + 커맨드 테스트
```bash
# Claude Code 세션 시작 후
/analyze-prd <샘플 PRD>
# → .analysis/prd/YYYY-MM-DD-*.md 생성 확인
```

### 5-3. 에이전트 스폰 확인
```
Agent(subagent_type="be-tech-lead", prompt="간단한 자문: Hello")
# → opus 모델로 응답이 와야 함
```

---

## 체크리스트 (신규 프로젝트 이식 시)

### 필수
- [ ] `.claude/`, `.analysis/` 복사
- [ ] `CLAUDE.md` 프로젝트 특화 정보로 재작성
- [ ] `harness-rules.json` 스택 맞게 조정
- [ ] `git_guard.protected_branches` 프로젝트 브랜치 전략 반영
- [ ] MCP 환경변수 세팅
- [ ] `settings.local.json` 생성 + gitignore 확인
- [ ] 스모크 테스트 (code-pattern, git-guard) exit 2 확인

### 권장
- [ ] Git 훅 연결 (`core.hooksPath`)
- [ ] 불필요한 에이전트 제거 (FE 없으면 fe-* 삭제 등)
- [ ] 프로젝트 특화 에이전트/스킬 추가
- [ ] 팀원 대상 ADOPTION.md 공유

### 선택
- [ ] `.analysis/be-refactoring/`, `incident/` 등 추가 파이프라인 이식
- [ ] 스택별 리뷰어 확장 (`kotlin-reviewer`, `python-reviewer` 등)
- [ ] `be-code-convention.md`를 프로젝트 도메인명으로 치환

---

## 자주 하는 실수

### 1. `settings.json` 경로 그대로 사용
하네스 훅 경로는 `${CLAUDE_PROJECT_DIR}/.claude/harness-check.py`. 템플릿을 서브디렉토리로 넣었다면 `${CLAUDE_PROJECT_DIR}/claude_framework/.claude/harness-check.py`로 수정 필요.

### 2. 금지 룰을 그대로 적용
`no-local-datetime`은 Kotlin에만. Node 프로젝트에서는 무해한 `LocalDateTime` 문자열도 차단될 수 있음 → `file_glob: "*.kt"` 조건 확인.

### 3. `!!` 룰이 의미 없는 스택에 남김
TypeScript `obj!.foo`도 `!!` 패턴에 걸림. `file_glob: "*.kt"` 명시 필수.

### 4. 페르소나 에이전트에 opus 모델 과다 사용
`be-tech-lead`, `be-senior`, `fe-lead`는 opus지만 **매번 호출 금지**. 설계/리뷰 게이트에서만 병행 스폰. 일반 질문은 기본 모델.

### 5. CLAUDE.md에 템플릿 원본 문구 방치
"Claude Code 기반 멀티 레포 워크스페이스 템플릿" 같은 템플릿 서술을 프로젝트 설명으로 치환하지 않으면 Claude가 템플릿 사용자라고 오인.

---

## 3-레이어 커스터마이징 (Phase A)

수동 복사 방식에서도 커스터마이징 레이어는 사용 가능:

- `.claude/harness-rules.json` — 팀 공유 추가 룰 (git tracked)
- `.claude/harness-rules.local.json` — 개인 오버라이드 (gitignore)
- Agent/Skill의 `extends: claude-framework:<name>` frontmatter로 부분 확장

자세한 병합 규칙과 CLI(`resource-resolver.py`, `lockfile-writer.py`)는 [PLUGIN.md의 커스터마이징 섹션](./PLUGIN.md#커스터마이징-override--extend) 참조.

## 업그레이드 (claude_framework 자체 업데이트 추적)

템플릿이 업데이트될 때 기존 프로젝트에 반영하는 법:

```bash
# 템플릿 원본 repo를 remote로 추가
git remote add claude_framework https://github.com/biuea3866/poc-project.git
git fetch claude_framework main

# 변경 사항 체리픽
git log claude_framework/main -- claude_framework/ --oneline
git checkout <commit> -- claude_framework/.claude/common/
```

또는 **git subtree**로 관리하면 더 깔끔:
```bash
git subtree add --prefix=claude_framework <url> main --squash
git subtree pull --prefix=claude_framework <url> main --squash
```

---

## FAQ

**Q. claude_framework를 프로젝트 루트로 쓰는 게 나은가, 서브디렉토리로 쓰는 게 나은가?**
A. 단일 레포 프로젝트는 루트, 멀티 레포(워크스페이스) 프로젝트는 서브디렉토리. `settings.json`의 훅 경로만 맞춰주면 된다.

**Q. 팀원이 `settings.local.json`을 공유하고 싶다면?**
A. 공유용은 `settings.json`에 넣고, 개인 오버라이드만 `settings.local.json`에. local은 항상 gitignore.

**Q. 하네스 룰이 너무 엄격해서 개발 속도가 느려진다면?**
A. 단계적 적용 — `severity: warning`으로 먼저 배포해 위반 수를 파악하고, 점진적으로 `error`로 승격. `harness-audit` 커맨드로 현재 위반 수 측정.

**Q. 다른 언어(Python/Go/Rust) 프로젝트에 쓸 수 있나?**
A. 뼈대는 재사용 가능. 다만 `harness-rules.json`의 Kotlin 룰을 제거하고 해당 언어 룰을 추가해야 함. `be-code-convention.md`도 해당 스택의 컨벤션으로 재작성 필요.
