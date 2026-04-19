---
name: codebase-convention-scan
description: 기존 코드베이스에서 네이밍/구조/테스트/안티패턴을 추출해 harness-rules.json 델타를 만드는 절차. 샘플링 기반, 최소 3회 반복 패턴만 룰화, 기존 위반은 severity 다운그레이드. convention-detective 에이전트가 참조.
---

# Codebase Convention Scan Skill

## 언제 사용하나
- `/init --scan`으로 기존 프로젝트에 claude_framework 이식 시
- 주기 감사 (`/audit-harness` 전 컨벤션 최신화)
- 신규 도메인 추가 후 룰 재조정

## 핵심 원칙
1. **샘플링** — 전수 조사 대신 카테고리별 5-10개만 읽음
2. **빈도 임계치** — 최소 3회 반복되는 패턴만 룰화
3. **하위 호환성** — 기존 위반 N건이 있으면 `severity: warning` 또는 `exclude_glob`로 grace period
4. **사용자 승인** — 자동 주입 금지, diff 제시 후 `y/n`

## 6단계 절차

### Step 1. 스택/빌드 도구 감지

```bash
# 빌드 파일로 1차 판별
[ -f build.gradle.kts ] && echo "kotlin"
[ -f pom.xml ]          && echo "java"
[ -f package.json ]     && echo "node (세부: tsconfig.json 존재 여부로 TS 판별)"
[ -f pyproject.toml ]   && echo "python"
[ -f go.mod ]           && echo "go"
[ -f Cargo.toml ]       && echo "rust"
```

2차: 파일 확장자 카운트 (`find . -name "*.kt" | wc -l` 등)로 주 언어 확정.

### Step 2. 파일 카테고리 수집

카테고리별 Glob → 많으면 **최신 변경 순 5-10개** 샘플:

| 카테고리 | 글롭 예시 | 목적 |
|---|---|---|
| Entity | `**/domain/**/*.kt`, `**/*Entity*.kt` | Rich Domain 여부, 필드 네이밍 |
| UseCase | `**/*UseCase*.kt`, `**/application/**/*.kt` | 의존 주입, 메서드 길이 |
| Controller | `**/*Controller*.kt`, `**/*Handler*.ts` | API 네이밍, 인증 패턴 |
| Repository | `**/*Repository*.kt`, `**/*Dao*.kt` | QueryDSL/JPA 사용 |
| Test | `**/*Test*.kt`, `**/*Spec.kt` | 프레임워크, 구조(BehaviorSpec vs JUnit) |
| SQL | `**/migration/**/*.sql` | FK/JSON/ENUM 사용 |

### Step 3. 패턴 추출

#### 3-1. 네이밍 빈도
```bash
# 클래스 접미사 집계
grep -rhoE "class \w+(Controller|Service|UseCase|Facade|Gateway|Repository|Impl)" src/ \
  | sed 's/.*class \w\+//' | sort | uniq -c | sort -rn
```

→ 최다 빈도 접미사를 `naming_convention`에 확정.

#### 3-2. 패키지 구조 판별
```bash
find src/main -type d | head -30
```
- `domain/ application/ infrastructure/ presentation/` 모두 존재 → **Hexagonal**
- `controller/ service/ repository/` → **Layered**
- 기타 → **Flat** (layer_dependency 룰 적용 불가)

#### 3-3. 테스트 프레임워크
```bash
# import 문 Grep
grep -rhoE "import (io\.kotest|org\.junit|io\.mockk|org\.mockito|vitest|jest)" --include='*Test*.kt' --include='*.test.ts' | sort -u
```

#### 3-4. 기존 안티패턴 카운트

각 잠재 룰에 대해 Grep으로 건수 측정:

| 룰 ID | Grep 패턴 | 결과에 따른 제안 |
|---|---|---|
| no-jpa-query | `@Query\(` | 0건 → error / 1-5건 → exclude / 5+건 → warning |
| no-local-datetime | `LocalDateTime` | 동일 |
| no-double-bang | `\w+!!` | 동일 |
| no-db-fk | `FOREIGN KEY` (in `*.sql`) | 동일 |
| no-console-log | `console\.log` (TS) | 동일 |

결정 트리:
```
위반 수 N:
├─ N = 0     → severity: error (즉시 적용 가능)
├─ N = 1-5   → severity: error + exclude_glob에 해당 파일 등록
├─ N = 6-20  → severity: warning + 마이그레이션 티켓 제안
└─ N > 20    → severity: warning + 단계적 도입 ADR 제안
```

#### 3-5. 도메인 경계 자동 생성
```bash
# domain/ 하위 1레벨 디렉토리 수집
ls src/main/kotlin/**/domain/ 2>/dev/null
```
발견된 도메인 목록: `[rental, product, user, notification]`
→ 각 도메인에 대해 `no-cross-domain-import-<domain>` 룰 자동 생성:

```json
{
  "id": "no-cross-domain-import-rental",
  "pattern": "import .*\\.domain\\.(product|user|notification)\\.",
  "file_glob": "*/domain/rental/**/*.kt",
  "message": "domain.rental은 다른 도메인 import 금지. common만 허용.",
  "severity": "error"
}
```

#### 3-6. Git/PR 컨벤션 추출
```bash
# 최근 커밋 메시지 50건
git log --oneline -50

# 브랜치 이름 패턴
git branch -a --sort=-committerdate | head -20
```

- `feat:`, `fix:`, `chore:` 접두사 비율 → Conventional Commits 여부
- `feature/{ticket-id}`, `fix/{ticket-id}` 패턴 → branch_convention 값

### Step 4. 룰 델타 생성

최종 JSON 패치 형식:

```json
{
  "_scan_metadata": {
    "scan_date": "2026-04-19",
    "stack": "kotlin/spring-boot",
    "build_tool": "gradle-kts",
    "test_framework": "kotest + mockk",
    "architecture": "hexagonal",
    "samples_read": 47,
    "domains_detected": ["rental", "product", "user", "notification"]
  },
  "proposed_additions": [
    {
      "section": "forbidden_patterns.rules",
      "rule": { "id": "...", "pattern": "...", ... },
      "confidence": "high",
      "reason": "0 violations in current codebase"
    }
  ],
  "proposed_modifications": [ ... ],
  "proposed_downgrades": [
    {
      "rule_id": "no-double-bang",
      "from_severity": "error",
      "to_severity": "warning",
      "existing_violations": 7,
      "migration_plan": "신규 코드는 error, 기존 7곳은 별도 티켓으로 정리"
    }
  ],
  "auto_exclude_globs": {
    "no-jpa-query": ["src/main/kotlin/legacy/**"]
  },
  "naming_convention_update": {
    "controller": "~ApiController",
    "usecase": "~UseCase"
  },
  "workflow_update": {
    "branch_format": "feature/{ticket-id}",
    "protected_branches": ["main", "dev"]
  }
}
```

### Step 5. 사용자 승인

표 형식 요약 + 항목별 선택 가능하게 제시:

```
## 제안 요약 (12 추가 / 2 강화 / 1 다운그레이드)

[1] 추가: no-any-type-dto (0건 위반)              [y/n/skip]
[2] 추가: no-local-datetime (3건, exclude 적용)    [y/n/skip]
...
[A] 전체 승인  [N] 전체 거절  [Q] 종료
```

### Step 6. 주입 + 검증

1. 백업: `cp harness-rules.json harness-rules.json.bak-$(date +%Y%m%d-%H%M%S)`
2. JSON 병합 (기존 룰과 ID 중복 시 우선순위 질의)
3. 스모크 테스트:
   ```bash
   python3 .claude/harness-check.py code-pattern < test-input.json
   ```
4. 의도치 않은 차단 발견 시 자동 롤백 + 보고

## 완료 체크
- [ ] 모든 카테고리 샘플링 완료 (최소 5건/카테고리)
- [ ] 빈도 < 3 패턴은 룰화 제외 확인
- [ ] 기존 위반 있는 룰은 downgrade 또는 exclude 적용
- [ ] 사용자 승인 획득 후 주입
- [ ] 백업 파일 생성
- [ ] 스모크 테스트 통과