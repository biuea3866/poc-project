---
name: harness-audit
description: harness-rules.json의 forbidden_patterns를 기준으로 코드베이스를 전수 감사하고 위반 목록을 보고하는 절차. Glob+Grep으로 매칭, exclude_glob 필터 적용, false positive 판단 근거 명시. harness-auditor 에이전트가 참조한다.
---

# Harness Audit Skill

## 언제 사용하나
- 주기적 코드 품질 점검
- 새 룰 추가 후 기존 코드 영향 파악
- 머지 전 최종 검증

## 원칙
- **수정 금지** — 발견만 하고 수정은 구현 에이전트에게 위임
- **근거 필수** — 모든 발견은 파일:라인:룰ID로 명시
- **false positive 표시** — 예외로 판단되면 이유 기록

## 절차

### Step 1. 룰 로드
`.claude/harness-rules.json` → `forbidden_patterns.rules` 배열 읽기

### Step 2. 룰별 스캔
각 rule에 대해:
1. `file_glob`으로 대상 파일 수집 (Glob)
2. `pattern` 정규식으로 매칭 (Grep -n)
3. `exclude_glob`에 걸리는 파일 제외
4. 매치 결과를 파일:라인 단위로 수집

### Step 3. 집계
```
## Summary
- Total violations: N
- By severity: error=X, warning=Y
- By rule:
  - no-local-datetime: 3
  - no-jpa-query: 1
  - no-db-fk: 2
```

### Step 4. 상세 보고
```
## Findings

### [ERROR] no-local-datetime (3건)
- path/File1.kt:42 — `val now = LocalDateTime.now()`
  → 제안: `ZonedDateTime.now()` 또는 `OffsetDateTime.now()`
- path/File2.kt:88 — ...
- path/File3.kt:15 — ...

### [ERROR] no-jpa-query (1건)
- path/Repository.kt:30 — `@Query("SELECT u FROM ...")`
  → 제안: QueryDSL CustomRepository + Impl 패턴으로 전환

### [WARNING] no-fqcn (2건)
- path/Service.kt:12 — `com.example.foo.BarService()` 호출
  → 제안: import 추가 후 단순 참조
```

### Step 5. 권고
- **Critical(error)는 즉시 수정 티켓** — 대응 에이전트: be-implementer
- **대량 발견**은 별도 리팩토링 티켓화 (refactor-<area> 형식)
- **false positive 의심** 건은 rule의 `exclude_glob` 보강 제안

## false positive 처리
예외로 판단 시 보고서에 다음 형식 포함:
```
### False Positive 후보
- path/migration/V20260418__.sql:10 — no-db-enum 매치되지만 마이그레이션 파일
  근거: exclude_glob에 `**/migration/**` 추가 제안
```

## 실행 도구
- Glob: file_glob에서 파일 수집
- Grep: 패턴 매칭 (`-n` 라인번호 + `output_mode: content`)
- Bash: `python3 .claude/harness-check.py code-pattern` 스팟 검증