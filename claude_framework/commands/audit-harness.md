---
description: 하네스 룰 전수 감사 — JSON 유효성 → 정적 룰 + 시니어 게이트 Critical 4유형
argument-hint: [optional path, defaults to be-repos/ + fe-repos/]
---

코드베이스를 감사해줘. **JSON 유효성 검증을 가장 먼저 수행 (Sprint 4 silent-pass 사건 재발 방지)**.

**감사 범위**: ${ARGUMENTS:-be-repos/ + fe-repos/ + devops-repos/}

## 단계

### 1. harness-rules.json JSON 유효성
```bash
python3 -c "import json; json.load(open('.claude/harness-rules.json'))"
```
실패 시: 즉시 중단 + 콤마/괄호 누락 위치 보고. 이게 silent-pass 의 흔한 원인.

### 2. 전수 감사 (정적 룰)
```bash
python3 .claude/scripts/harness-audit.py --root <범위> --fail-on error
```
출력: 룰별 위반 목록 (파일:라인:매치 — 최대 20건씩).

### 3. Senior Gate (LLM-free Critical 4유형)
변경 파일이 아닌 **전수 모드** 가 필요하면 sample 단위로:
```bash
find <범위> -name '*Controller.kt' -o -name '*UseCase.kt' -o -name '*Listener.kt' \
  | xargs python3 .claude/scripts/senior-gate.py --diff-files
```
- 관리자 엔드포인트 `@RoleRequired` 누락
- HttpServletRequest 직접 주입
- Listener 의 Repository 직접 호출
- UseCase `@Transactional` 누락

### 4. harness-auditor 에이전트로 정성 검토
정적 룰로 못 잡는 cross-cutting 패턴 (도메인 경계 위반, 네이밍 일관성) 을 `harness-auditor` 에 위임.

### 5. 보고서

```
## 하네스 감사 결과

### JSON 유효성
✅ harness-rules.json 유효 (또는 ❌ + 위치)

### 정적 룰 위반
- 룰별 집계 (severity / rule_id / 카운트)
- 상위 위반 파일 Top 10

### Senior Gate Critical
- 4유형별 위반 카운트
- 파일:라인 + 권장 수정

### Cross-cutting (harness-auditor)
- 도메인 경계 위반
- 네이밍 일관성

### 권고
- harness-rules 보강 후보 (반복되는 패턴 → 신규 룰)
- 메타-피드백 트리거 후보
```

## 운영 규칙

- **수정 금지** — 발견만 보고. 수정은 별도 티켓.
- false positive 판단 시 이유 명시
- silent-pass 의심 (어제 통과 → 오늘 위반) 발견 시 high 위험도로 표기

## 메타-피드백 연동

- 룰로 막을 수 있었는데 못 막은 패턴 발견 → `process-reviewer` 트리거 후보
- 결과를 `pipelines/feedback-loop/<YYYY-MM-DD>-audit.md` 에 저장하면 nightly guardian 이 참조
