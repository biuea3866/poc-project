# BE 기술 부채 해소 파이프라인

코드 품질, 테스트 부재, 컨벤션 위반 등 소~중 규모 기술 부채를 진단하고 해소한다.

> 서비스 전환, 아키텍처 변경 등 대규모 리팩토링은 `be-refactoring` 파이프라인을 사용한다.

## 공통 가이드 참조
> 아래 공통 규칙을 따릅니다. 파이프라인 특화 규칙은 이 문서에서 정의합니다.

- [문체/용어 규칙](../common/output-style.md)
- [Mermaid 다이어그램](../common/mermaid.md)
- [티켓 작성법](../common/ticket-guide.md)

## be-refactoring과의 차이

| | be-refactoring | be-tech-debt |
|---|---|---|
| **규모** | 중~대 (서비스 전환, 아키텍처) | 소~중 (코드 정리, 품질) |
| **마이그레이션** | 필요 (무중단, FeatureFlag, Dual Write) | 불필요 |
| **Phase** | 3단계 (현황→계획→구현) | 2단계 (진단→수정) |
| **위험도** | 높음 (기존 동작 변경 가능) | 낮음 (동작 변경 없음) |
| **산출물** | analysis/ + plan/ | diagnosis/ + fix/ |

## 실행 원칙

- Phase 1(진단) → Phase 2(수정) 순서
- **동작 변경 없음**: 리팩토링과 달리, 기술 부채 해소는 외부 동작을 바꾸지 않음
- **점진적 해소**: 한 번에 전부가 아니라, 범위를 정해서 해소
- **자동화 우선**: detekt, 린터 등 자동 도구로 검출 가능한 것부터

## 입력 유형

| 유형 | 예시 | 진단 기준 |
|------|------|----------|
| detekt/린터 위반 | MagicNumber, ReturnCount, NoUnusedImports | detekt 리포트 |
| 테스트 부재 | 테스트 0건, 커버리지 < 50% | 테스트 파일 유무, 커버리지 리포트 |
| nullable 남용 | 불필요한 `?` 타입, `!!` 사용 | grep 기반 검출 |
| 컨벤션 불일치 | @Query 사용, @Transactional Repository, FQCN import | CLAUDE.md 컨벤션 대조 |
| deprecated 코드 | @Deprecated 잔존, 사용되지 않는 코드 | grep @Deprecated |
| 코드 중복 | 동일/유사 로직 반복 | 수동 분석 |
| 문서 부채 | Swagger 누락, 주석 부재, COMMENT 누락 | grep 기반 검출 |

## 출력 경로

```
.analysis/be-tech-debt/results/{날짜}_{대상}/
│
├── diagnosis/                              # Phase 1 진단
│   ├── debt_inventory.md                   # 부채 목록 (자동 검출 + 수동 분석)
│   └── priority_matrix.md                  # 우선순위 매트릭스 (영향도 × 해소 비용)
│
├── fix/                                    # Phase 2 수정
│   ├── fix_plan.md                         # 수정 계획 (파일별 변경 내용)
│   └── tickets/                            # 수정 티켓 (선택. 규모가 클 때만)
│       ├── _overview.md
│       └── ticket_{NN}_{이름}.md
│
└── README.md                               # 산출물 인덱스
```

## 에이전트 역할

### 페르소나 에이전트 (선택적 병행 스폰)

| 에이전트 | 관점 | 병행 시점 |
|---------|------|----------|
| [`be-senior`](../agents/be-senior.md) | 코드 품질 기준, "왜 이게 부채인가" 판단 | Phase 1 진단 결과 리뷰 |
| [`be-ic`](../agents/be-ic.md) | 안전한 리팩토링 실행, TDD, 컨벤션 준수 | Phase 2 수정 (debt-fixer 대체 또는 병행) |

### Phase 1: 진단 에이전트

| 에이전트 | 역할 | 산출물 |
|---------|------|--------|
| [`debt-scanner`](../agents/debt-scanner.md) | 부채 자동 검출 | `debt_inventory.md` |
| [`debt-prioritizer`](../agents/debt-prioritizer.md) | 우선순위 판정 | `priority_matrix.md` |

### Phase 1-D.5: 리뷰 게이트

| 체크 항목 | 통과 기준 |
|----------|----------|
| 검출 근거 | 자동 도구 리포트 또는 코드 참조가 있는지 |
| 범위 한정 | 한 번에 해소할 범위가 명확한지 (전체 X) |
| 동작 불변 | 수정 후 외부 동작이 바뀌지 않는지 확인 가능한지 |

### Phase 2: 수정 에이전트

| 에이전트 | 역할 |
|---------|------|
| [`debt-fixer`](../agents/debt-fixer.md) | 부채 수정 개발자 |
| [`debt-reviewer`](../agents/debt-reviewer.md) | 수정 리뷰어 |

### Phase 2-D.5: 리뷰 게이트

| 체크 항목 | 통과 기준 |
|----------|----------|
| detekt 통과 | 수정 범위에서 위반 0건 |
| 테스트 통과 | 기존 + 신규 테스트 전부 통과 |
| 동작 불변 | 외부 API/이벤트 변경 없음 |

---

# Phase 1: 진단

## 1-1. 자동 검출 (에이전트, background)

```bash
# detekt
./gradlew :domain:detekt 2>&1 | grep "weighted issues"

# 테스트 커버리지
./gradlew :domain:test --tests "{패키지}.*" 2>&1 | tail -5

# nullable 검출
grep -rn "?: \|!!\|: .*?" src/main/kotlin/{대상}/ | wc -l

# deprecated
grep -rn "@Deprecated\|@Suppress" src/main/kotlin/{대상}/ | wc -l

# @Query 사용 (컨벤션 위반)
grep -rn "@Query" src/main/kotlin/{대상}/ | wc -l

# Swagger 누락
grep -rL "@Operation" src/main/kotlin/{대상}/controller/ | wc -l
```

## 1-2. 수동 분석

자동 검출로 잡히지 않는 부채:
- 코드 중복 (유사 패턴 반복)
- 과도한 복잡도 (한 메서드 50줄+)
- 잘못된 레이어 의존 (Service에서 다른 Service 직접 호출)
- 네이밍 불일치

## 1-3. 우선순위 매트릭스

```
영향도 ↑
  High │  ★ 즉시   ★ 즉시
       │
  Med  │  ⏭ 다음   ★ 즉시
       │
  Low  │  ⏭ 나중   ⏭ 다음
       └──────────────────→
          Low     High
                해소 비용 (쉬움→어려움)
```

---

# Phase 2: 수정

## 2-1. 수정 계획

`fix_plan.md` 작성:
- 수정 대상 파일 목록
- 파일별 변경 내용 (간결하게)
- 예상 변경 라인 수
- 테스트 추가 여부

## 2-2. 수정 실행

> project-analysis Phase 3-1과 동일한 TDD 사이클.
> 단, 부채 해소는 기존 테스트가 통과하는지가 핵심.

```
1. 기존 테스트 전체 통과 확인 (GREEN 상태에서 시작)
2. 코드 수정 (리팩토링)
3. 기존 테스트 여전히 통과 확인
4. 신규 테스트 추가 (필요 시)
5. detekt 통과
6. 커밋
```

## 2-3. PR 체크리스트

```
□ detekt 통과 (수정 범위 위반 0건)
□ 기존 테스트 전체 통과
□ 신규 테스트 추가 (테스트 부채 해소 시)
□ 외부 동작 변경 없음 확인
□ PR 설명에 부채 해소 내역 기재
```

---

# 기술 성장 포인트

> 기술 부채 해소는 "깨끗한 코드란 무엇인가"를 체득하는 과정.

## Phase 1 (진단) 시 시도할 것

| 시도 | 설명 |
|------|------|
| **정적 분석 도구 깊이 이해** | detekt 규칙이 왜 존재하는지. `ReturnCount`가 왜 문제인지, 어떻게 해소하는지 원리 이해 |
| **코드 스멜 패턴화** | "이 코드가 왜 나쁜지"를 패턴으로 정리. 다음에 같은 실수를 안 하려면 어떤 원칙을 세워야 하는지 |
| **테스트 피라미드 분석** | 현재 테스트가 Unit/Integration/E2E 중 어디에 치우쳐 있는지 분석. 이상적 비율과 비교 |

## Phase 2 (수정) 시 시도할 것

| 시도 | 설명 |
|------|------|
| **안전한 리팩토링 기법** | Extract Method, Replace Conditional with Polymorphism 등 Martin Fowler 리팩토링 카탈로그 실천 |
| **테스트를 먼저 추가하고 리팩토링** | 테스트 없는 코드를 바로 수정하지 않고, 먼저 현재 동작을 검증하는 테스트를 추가한 후 수정 |
| **nullable 제거 사고법** | "이 값이 왜 null일 수 있는가" → 도메인 관점에서 null이 의미하는 것 → non-null로 바꿀 수 있는 설계 |
| **자동화 규칙 추가** | 해소한 부채가 재발하지 않도록 detekt 커스텀 규칙 또는 pre-commit hook 추가 |
