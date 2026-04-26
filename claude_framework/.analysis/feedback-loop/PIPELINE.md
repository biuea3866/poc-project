# Feedback Loop 운영 파이프라인

## 목적
`.claude/harness-check.py` 훅 + PR Senior Review Gate + Nightly Harness Audit + QA Follow-up 으로 구성된 **3-layer 피드백 루프**의 건강 상태를 주기적으로 점검하고, 사건 발생 시 빠르게 복구한다.

이 파이프라인은 rental-commerce Sprint 4 에서 실제로 쓴 순서 그대로다. harness-rules.json 의 콤마 1개 누락이 훅을 silent pass 시키면서 보안 Critical(@RoleRequired 누락) 이 잠입한 사건을 계기로 만들어졌다.

## 언제 실행하는가

- **주기 점검** — 주 1회 이상 (feedback-loop-guardian 에이전트가 수행)
- **Nightly Harness Audit fail** — Issue 발행됐을 때 사람이 수동 트리거
- **PR Senior Review Gate fail 반복** — 같은 Critical 유형이 2회 이상 보이면 휴리스틱 갭
- **QA 스프린트 종료 직후** — 후속 티켓 자동 발행 결과 재검증

## 담당 에이전트 & 스킬

| 역할 | 에이전트 | 도구 |
|---|---|---|
| 메타 진단 | `feedback-loop-guardian` | Read, Grep, Glob, Bash |
| 위반 감사 | `harness-auditor` | harness-audit skill |
| PR 리뷰 레퍼런스 | `pr-reviewer` | pr-review-checklist skill |
| 교정 구현 | `be-implementer` | tdd-loop skill |

## 절차 (Sprint 4 에서 실제로 쓴 순서)

### 1. `harness-rules.json` JSON 유효성 검증 — 훅 사망 상태 감지

가장 먼저 이걸 검증한다. **훅은 silent pass 하기 쉽다**.

```bash
python3 -c 'import json; json.load(open(".claude/harness-rules.json"))' \
  && echo OK \
  || echo "LAYER_1_DEAD — JSON 파싱 실패"
```

실패 시 즉시 hotfix PR. 나머지 단계는 의미 없음.

### 2. `harness-audit.py` 전수 감사

```bash
python3 .claude/scripts/harness-audit.py --format json --fail-on none --out /tmp/audit.json
```

확인 포인트:
- `scanned_files > 0` — 0 이면 글롭/경로 문제
- `applied_rules > 0` — 0 이면 룰 파일 손상
- `by_rule` 에서 특정 룰이 지나치게 많으면 false positive 의심 → exclude_glob 보강 대상

### 3. `pr-reviewer` 에이전트 수동 스폰 (스냅샷 수집)

최근 머지된 PR 중 Critical 지적이 있었던 3~5건에 대해 `pr-reviewer` 를 재실행한다. 목적은 두 가지:
- senior-gate.py 가 **선제 차단할 수 있었는지** 확인 (휴리스틱 갭 식별)
- 새로 생긴 Critical 유형을 목록화

```
Agent(subagent_type="pr-reviewer", prompt="리뷰 대상: <PR URL>")
```

### 4. `senior-gate.py` dry-run

```bash
BASE=main  # 프로젝트 기본 브랜치
CHANGED=$(git diff --name-only origin/$BASE...HEAD -- '*.kt')
python3 .claude/scripts/senior-gate.py --diff-files "$CHANGED" --allow-empty-diff --format md
```

3번의 pr-reviewer 지적과 대조해 **놓친 Critical 유형** 을 식별. 놓친 유형은 senior-gate.py 에 신규 휴리스틱 추가 후보.

### 5. 발견된 Critical → hotfix PR

- Layer 1 사망 (JSON 오류, 훅 로직 버그) → 최우선 PR
- Layer 2 커버리지 갭 → `senior-gate.py` 휴리스틱 1~2개 추가 PR
- Layer 3 false positive 과다 → `harness-rules.json` 의 `exclude_glob` 보강 PR

모든 hotfix 는 TDD 로 진행하며 senior-gate.py 변경은 스크립트 단위 테스트 포함.

### 6. 룰 커버리지 갭 → `harness-rules.json` 업데이트

pr-reviewer 가 반복 지적하는 패턴 중 regex 로 표현 가능한 것은 `forbidden_patterns` 에 추가. 새 룰은 **반드시 `severity: warning` 으로 도입** 하고, 전수 감사로 legacy 위반 수를 측정한 뒤 별도 PR 에서 `error` 로 승격.

### 7. QA 보고서 업데이트 + follow-up tickets 자동 발행

QA 팀이 `docs/qa/YYYY-MM-sprint-N.md` 에 `## 후속 티켓 제안` 표를 채워 push 하면 GitHub Actions 가 자동으로 Issue 생성. 파이프라인 마지막 단계에서 Issue 생성 여부/중복 스킵 동작을 스폿 체크.

```bash
python3 .claude/scripts/qa-followup-extract.py --file docs/qa/latest.md --format md
```

## 산출물

파일 경로: `.analysis/feedback-loop/YYYY-MM-DD-health.md`

구조는 `agents/feedback-loop-guardian.md` 의 "출력 형식" 섹션을 따른다.

## Exit Criteria

- [ ] harness-rules.json JSON 유효성 OK
- [ ] harness-check.py 로컬 dry-run 에서 exit 2 재현
- [ ] harness-audit.py `scanned_files > 0`, `applied_rules > 0`
- [ ] senior-gate.py dry-run 결과 ↔ 최근 pr-reviewer Critical 대조
- [ ] 발견된 Critical 은 전부 hotfix PR 또는 교정 제안 목록으로 기록
- [ ] 보고서 파일 저장

## 주의

- **PR 수정은 파이프라인 밖에서.** 이 파이프라인은 진단과 제안까지. 구현은 be-implementer 에게 위임.
- **exclude_glob 남발 금지.** false positive 제거를 위해 글롭을 넓히면 Critical 이 구멍으로 다시 새어 나간다.
- **silent pass 의심은 건강하지 않은 것.** violations=0 이 갑자기 나오면 개선이 아니라 버그다.
- **새 Layer 를 찍 추가하지 말 것.** 4개 Layer 가 이미 중복 방어 중. 문제는 각 Layer 내부 룰 커버리지.

## 실패 패턴 (Sprint 4 실제 회고)

- **콤마 1개 누락** → harness-rules.json JSON 파싱 실패 → harness-check.py 가 silent exit 0 → 훅 있는데 아무도 모르는 상태.
- **PR 리뷰 사람 기반** → 같은 Critical(@RoleRequired 누락) 이 반복 유입 → senior-gate.py 휴리스틱으로 기계화.
- **QA 후속 티켓을 슬랙으로 공유** → 추적 불가능 → Issue 자동 발행으로 전환.

## 다음 단계
피드백 루프 건강 OK → 일상 개발 재개. 발견 시 → hotfix PR → 재점검 후 Exit Criteria 재통과.
