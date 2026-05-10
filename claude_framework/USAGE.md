# claude_framework 이식·사용 가이드

`README.md` 의 가벼운 진입점. 자세한 설계는 `REFACTOR.md`, 플러그인 방식은 `PLUGIN.md`, 수동 복사 방식은 `ADOPTION.md` 참조.

---

## 1. 이식 (Adoption)

새 프로젝트에 framework 를 가져오는 두 가지 방법.

### 방법 A — 플러그인 (권장)

```bash
# 1) 플러그인 설치 (최초 1회)
mkdir -p ~/.claude/plugins && cd ~/.claude/plugins
git clone https://github.com/biuea3866/poc-project claude-framework-src
ln -s claude-framework-src/claude_framework claude-framework

# 2) Claude Code 에서 등록
/plugin marketplace add ~/.claude/plugins/claude-framework
/plugin install claude-framework@claude-framework-local

# 3) 어느 프로젝트에서든 init
cd /path/to/your-project
/init --scan --classify-repos
```

`/init` 옵션:
- `--scan` — 기존 코드에서 컨벤션 자동 추출 → harness-rules 주입
- `--classify-repos` — 하위 서브디렉토리를 BE/FE/DevOps 자동 분류

상세: [`PLUGIN.md`](./PLUGIN.md)

### 방법 B — 직접 복사 + install.sh

```bash
# 1) 자산 복사
cp -r /path/to/claude_framework/{.claude,.analysis,.github,agents,skills,commands,docs} <your-project>/
cp /path/to/claude_framework/{install.sh,REFACTOR.md,CLAUDE.md} <your-project>/

# 2) 검증
cd <your-project>
bash install.sh
# → "매니페스트/리소스 검증 통과 (4계층 + 다층 방어 + 메타-피드백 자산 포함)"
```

`install.sh` 가 다음을 자동 검증한다 (실패하면 즉시 중단 — silent-pass 차단):
- `harness-rules.json` JSON 유효성
- 5개 Python 스크립트 컴파일
- 4개 GitHub Actions 워크플로우 존재
- 3개 신규 에이전트 + 8개 신규 PIPELINE.md
- `docs/feedback-loop/proposals/` + `settings.json.feedback-loop.example`

상세: [`ADOPTION.md`](./ADOPTION.md)

### 작업용 워크트리 디렉토리

`be-repos/`, `fe-repos/`, `devops-repos/` 는 사용자가 필요할 때 직접 생성. `/init --classify-repos` 가 자동으로 만든다. 수동:

```bash
mkdir -p be-repos fe-repos devops-repos
```

---

## 2. 활성화 (Activation)

### 2-1. 기본 (PreToolUse 훅 + git_upstream_guard)

`.claude/settings.json` 의 hooks 가 자동 등록됨. 별도 작업 없음.

검증:
```bash
# JSON 유효성 (silent-pass 차단)
python3 -c "import json; json.load(open('.claude/harness-rules.json'))"

# 가짜 위험 명령 차단 확인
CLAUDE_TOOL_INPUT='{"command":"git push -u origin main"}' \
  python3 .claude/scripts/harness-check.py git-guard
# 기대: BLOCKED + exit 2
```

### 2-2. 메타-피드백 루프 (선택)

기본 비활성. 활성화하려면 `.claude/settings.json` 의 `hooks` 객체에 다음을 병합:

```jsonc
// .claude/settings.json.feedback-loop.example 의 hooks 키를 복사
"Stop": [...],
"SubagentStop": [...]
```

활성화 후:
- Stop / SubagentStop 훅이 트리거 조건 충족 시 `process-reviewer` 발화
- 제안 파일은 `docs/feedback-loop/proposals/<YYYYMMDD>-<topic>.md` 에 자동 생성
- **자동 .md 수정 절대 금지** — 사람이 검토 후 PR 로만 반영

안전 장치 (자동 적용):
- `stop_hook_active` 환경 변수 가드 (재귀 차단)
- 일일 발화 상한 5회
- 비교 기준 (PRD/ADR/harness-rules.json) 외 의견 금지

### 2-3. GitHub Actions

`.github/workflows/` 4개가 자동 등록됨:

| 워크플로우 | 트리거 |
|-----------|--------|
| `pr-senior-review.yml` | PR open/sync |
| `harness-audit.yml` | nightly 03:00 KST + harness-rules.json push |
| `qa-followup-tickets.yml` | docs/qa/*.md push |
| `feedback-loop-health.yml` | nightly 03:30 KST |

별도 secret 없이 동작 (`GITHUB_TOKEN` 만 사용).

---

## 3. 일상 사용 (Daily Use)

### 3-1. 슬래시 커맨드 카탈로그

```
설계 / 분석
  /analyze-prd <link>             — PRD 분석
  /plan-project <prd-path>        — 설계+TDD+티켓
  /split-tickets <feature-dir>    — 티켓만 분해

구현
  /tdd-implement <ticket> [be|fe] — 단일 티켓 TDD
  /parallel-tickets <tickets.md>  — 병렬 (worktree + agent team)

리뷰
  /review-pr <num>                — Senior Gate → 5인 병렬 리뷰
  /security-review <num>          — 보안 전용
  /audit-harness [path]           — 하네스 룰 전수 감사

피드백 루프
  /audit-feedback-loop [date]     — 루프 건강 점검
  /process-review [trigger]       — 메타-피드백 수동 트리거

설정
  /init [--scan] [--classify-repos] — 프로젝트 이식 (플러그인 모드)
```

### 3-1-a. Command ↔ Pipeline 매핑

| Command | 진입하는 Pipeline | 산출물 위치 |
|---------|-------------------|-------------|
| `/analyze-prd` | `pipelines/prd/` | `pipelines/prd/<date>-<feature>/` |
| `/plan-project` | `pipelines/project-analysis/` + `pipelines/be-implementation/` | `pipelines/be-implementation/<feature>/` |
| `/review-pr` | `pipelines/pr-review/` | GitHub PR 코멘트 |
| `/audit-feedback-loop` | `pipelines/feedback-loop/` | `pipelines/feedback-loop/<date>-health.md` |

**커맨드 없는 파이프라인** (오케스트레이션으로만 진입): `incident`, `inquiry`, `multi-repo`, `refactoring`, `release`, `api-change`. main-orchestrator 가 상황 감지 시 자동 진입.

**파이프라인 없는 커맨드** (단일 액션): `/init`, `/audit-harness`, `/tdd-implement`, `/split-tickets`, `/parallel-tickets`, `/security-review`, `/process-review`. 단일 에이전트/스킬 호출.

### 3-2. 일반적인 흐름 (Sprint 1개 가정)

```
1. /analyze-prd <PRD URL>
   → pipelines/prd/<feature>/{requirements,acceptance}.md

2. /plan-project pipelines/prd/<feature>/requirements.md
   → pipelines/project-analysis/<feature>/{design,adr,tickets}.md
   → pipelines/be-implementation/<feature>/ 도 함께

3. /parallel-tickets pipelines/project-analysis/<feature>/03-tickets.md 4
   → 4개 worktree 동시 구현, 각 PR 자동 생성

4. /review-pr <num>  (각 PR)
   → senior-gate → pr-reviewer + be-senior + be-tech-lead + security-reviewer 병렬
   → request-changes / comment / approve

5. (PR 머지 후 nightly)
   → harness-audit.yml 전수 감사
   → feedback-loop-health.yml 루프 건강 점검
   → 위반 검출 시 Issue 자동 발행
```

### 3-3. 메타-피드백 흐름 (활성화 시)

```
1. PR #X 가 senior-gate 에서 fail
   → Stop 훅이 process-reviewer 발화 (트리거 조건 1번)

2. process-reviewer 가 분석
   → docs/feedback-loop/proposals/<date>-<topic>.md 생성

3. 사람이 제안 파일 검토
   ├─ 승인 → feedback-loop-guardian 이 refactor/feedback/<date> PR 생성 → 사람 머지
   ├─ 보류 → docs/feedback-loop/proposals/archived/
   └─ 기각 → docs/feedback-loop/proposals/closed/ (closed_reason 기록)

4. nightly 03:30 KST
   → feedback-loop-guardian 이 효과 측정
   → 같은 trigger 재발 빈도로 effective / partial / ineffective 판정
   → ineffective 2회 연속 → high Issue 자동 발행
```

---

## 4. 룰 추가·수정

### 4-1. 새 금지 패턴

`.claude/harness-rules.json` 의 `forbidden_patterns.rules` 에 추가:

```json
{
  "id": "no-foo",
  "pattern": "Foo\\(",
  "file_glob": "*.kt",
  "exclude_glob": "**/test/**",
  "message": "Foo 사용 금지 — Bar 사용",
  "severity": "error"
}
```

`harness-check.py` 가 자동 적용. **별도 코드 수정 불필요.**

### 4-2. 프로젝트별 오버라이드

`.claude/harness-rules.local.json` (gitignore) 에 작성. plugin base + project + local 3-layer 자동 병합.

### 4-3. 룰 비활성화

```json
{
  "_rule_disabled": ["no-foo"]
}
```

### 4-4. 룰 필드 재정의

```json
{
  "_rule_overrides": {
    "no-foo": { "severity": "warning" }
  }
}
```

---

## 5. 트러블슈팅

| 증상 | 원인 | 해결 |
|------|------|------|
| 훅이 동작하지 않음 | settings.json 의 hooks 경로 오류 | `${CLAUDE_PROJECT_DIR}/.claude/scripts/harness-check.py` 인지 확인 |
| 어제까지 통과했는데 갑자기 위반 검출 | harness-rules.json silent-pass | `python3 -c "import json; json.load(open('.claude/harness-rules.json'))"` 으로 JSON 유효성 확인 |
| `git push` 가 보호 브랜치로 직행했다 | upstream 이 origin/main 으로 잘못 설정 | `git branch --unset-upstream` → `git push -u origin <feature-branch>` |
| process-reviewer 가 폭주 | 트리거 조건 너무 느슨 | nightly feedback-loop-health 보고서 확인, 일일 상한 자동 적용됨 |
| Stop 훅 무한 루프 | `stop_hook_active` 가드 누락 | settings.json.feedback-loop.example 그대로 사용 (가드 포함) |
| install.sh 가 fail | 자산 누락 | 에러 메시지의 파일 경로 확인 후 복사 누락 보강 |

---

## 6. 핵심 자산 한눈에

```
.claude/
├── harness-rules.json                 ★ 단일 룰 진실 원천
├── settings.json                      hooks 등록 (체크인)
├── settings.json.feedback-loop.example 메타-피드백 활성화 스니펫
└── scripts/
    ├── harness-check.py               PreToolUse 정적 + git_upstream_guard 동적
    ├── senior-gate.py                 Critical 4유형 LLM-free
    ├── harness-audit.py               전수/diff 감사
    ├── qa-followup-extract.py         QA → Issue 파서
    └── feedback-loop-stats.py         가디언 보조 통계

agents/                                14종 (구현 + 리뷰 5종 + 메타 2종 + 분석)
pipelines/                             10개 PIPELINE.md
commands/                              11종 슬래시 커맨드
docs/feedback-loop/proposals/          제안 파일 보관소
.github/workflows/                     4개 자동 워크플로우
```

---

## 7. 더 읽기

- [`REFACTOR.md`](./REFACTOR.md) — 4계층 + 다층 방어 + 메타-피드백 설계 원칙·안티패턴
- [`PLUGIN.md`](./PLUGIN.md) — 플러그인 방식 상세
- [`ADOPTION.md`](./ADOPTION.md) — 수동 복사 이식 상세
- [`CLAUDE.md`](./CLAUDE.md) — 디렉토리·하네스 구조 요약
- 각 PIPELINE.md — 단계별 절차
- 각 agent.md — 역할별 페르소나·도구
