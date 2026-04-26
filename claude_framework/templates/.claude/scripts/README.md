# `.claude/scripts/` — 3-layer Feedback Loop

Sprint 4 (rental-commerce) 에서 harness-rules.json 의 JSON 파싱 오류 1건으로 훅이 silent pass 되면서 보안 Critical(관리자 엔드포인트 `@RoleRequired` 누락)이 잠입한 사건을 방지하기 위한 **3-layer 피드백 루프**입니다.

훅 1개에만 의존하지 말고, 서로 다른 시점·시스템에서 중첩 검증하도록 설계했습니다.

---

## 구조

```
Layer 1 — Local hook (real-time)
  .claude/harness-check.py (PreToolUse)
  ↓ 파일 저장 순간 차단
  ↓ 장점: 즉시성
  ↓ 단점: 훅이 죽으면 silent pass (← 바로 이 사고)

Layer 2 — PR gate (per-PR)
  .github/workflows/pr-senior-review.yml
    → scripts/senior-gate.py  (보안/구조 Critical)
    → scripts/harness-audit.py --diff-files  (룰 전수 적용)
  ↓ 장점: 훅이 silent pass 되어도 CI 에서 잡음
  ↓ 단점: PR 만들 때만 돌음

Layer 3 — Nightly audit (full sweep)
  .github/workflows/harness-audit.yml (cron)
    → scripts/harness-audit.py (전수)
  ↓ 장점: 과거에 잠입한 위반, 새 룰 추가 후 legacy 위반 전부 탐지
  ↓ 장점: 룰 커버리지 갭 감지 (scanned_files=0 → 룰 오작동)

Layer 4 (선택) — QA follow-up
  .github/workflows/qa-followup-tickets.yml (docs/qa/*.md push 트리거)
    → scripts/qa-followup-extract.py
  ↓ QA 보고서의 "후속 티켓 제안" 표 → GitHub Issue 자동 발행
```

---

## 각 스크립트 역할

### `harness-audit.py`
`harness-rules.json` 의 `forbidden_patterns` + `layer_dependency` 를 코드베이스 전수 또는 diff 파일에만 적용합니다.

- **주석/블록 주석 필터** — `//`, `#`, `/* ... */` 내부 매칭은 false positive 로 제외.
- `--diff-files` — `git diff --name-only` 결과를 넘기면 그 파일만 스캔.
- `--format json|md`, `--out <path>`, `--fail-on error|warning|none`.

**Exit codes:** 0=clean, 1=error severity 발견, 2=스크립트 오류.

### `senior-gate.py`
`pr-reviewer` 에이전트가 반복해서 지적하는 4가지 Critical 유형을 휴리스틱으로 차단:

| 룰 | 감지 조건 |
|---|---|
| `admin_endpoint_without_role` | Controller 메서드 매핑 경로/어노테이션에 `admin` 이 있으면서 `@RoleRequired`/`@PreAuthorize` 부재 |
| `controller_http_servlet_request` | `*Controller.kt` 파라미터에 `HttpServletRequest` 직접 주입 |
| `listener_direct_repository` | `*Listener.kt`/`*Consumer.kt` 필드/생성자에 `*Repository` 주입 |
| `service_usecase_without_transactional` | `*UseCase.kt` 또는 `@Service` 클래스가 Repository 호출하는데 `@Transactional` 부재 |

**단순 regex 로는 잡기 힘든** '클래스 이름 + 어노테이션 조합' 을 파이썬으로 검사.

### `qa-followup-extract.py`
`docs/qa/*.md` 의 `## 후속 티켓 제안` 섹션 표를 파싱해 `gh issue create` 페이로드로 출력합니다. 섹션 마커/표 헤더는 `harness-rules.json` 의 `qa_followup` 섹션으로 프로젝트별 조정 가능.

---

## 로컬 dry-run

```bash
# 1. harness-rules.json JSON 유효성
python3 -c 'import json; json.load(open(".claude/harness-rules.json"))'

# 2. 하네스 훅 스모크 테스트
CLAUDE_TOOL_INPUT='{"file_path":"Foo.kt","content":"val x = LocalDateTime.now()"}' \
  python3 .claude/harness-check.py code-pattern
# → exit 2 + 메시지가 나와야 정상 (훅 살아 있음)

# 3. 전수 감사 dry-run
python3 .claude/scripts/harness-audit.py --format md --out /tmp/audit.md
cat /tmp/audit.md

# 4. Senior Gate dry-run (현재 브랜치 vs origin/main)
CHANGED=$(git diff --name-only origin/main...HEAD -- '*.kt')
python3 .claude/scripts/senior-gate.py --diff-files "$CHANGED" --allow-empty-diff

# 5. QA 후속 티켓 추출
python3 .claude/scripts/qa-followup-extract.py --file docs/qa/latest.md --format md
```

---

## 훅이 silent pass 된 걸 어떻게 감지하는가

`harness-audit.py` 가 **`scanned_files=0`** 또는 **`applied_rules=0`** 을 리턴하면 훅은 동작 안 하고 있었을 가능성이 높습니다.

추가로 권장되는 주기적 진단:
- nightly workflow 에서 `scanned_files` 가 이전 run 대비 급감하면 Issue 재발행
- `feedback-loop-guardian` 에이전트가 JSON 유효성 + `--fail-on warning` dry-run 을 정기 수행

---

## 룰 추가 시 주의

1. 새 룰의 `file_glob` / `exclude_glob` 는 반드시 기존 코드에 대해 **전수 감사로 영향 측정**.
2. 최초 도입은 `severity: warning` 으로 시작 → legacy 정리 후 `error` 로 승격.
3. `harness-audit.py --fail-on warning` 로 CI 에서 warning 까지 강제하는 구간도 고려.

자세한 파이프라인은 `.analysis/feedback-loop/PIPELINE.md` 참조.
