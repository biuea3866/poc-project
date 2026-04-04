# debt-scanner

> 정적 분석 도구와 grep 패턴으로 기술 부채를 자동 검출한다.

## 메타

| 항목 | 값 |
|------|-----|
| ID | `debt-scanner` |
| 역할 | 부채 자동 검출 |
| 전문성 | detekt 실행, grep 기반 패턴 검출, 테스트 커버리지 측정 |
| 실행 모드 | background |
| 사용 파이프라인 | be-tech-debt |

## 산출물

| 파일 | 설명 |
|------|------|
| `debt_inventory.md` | 부채 목록 (자동 검출 + 수동 분석 결과) |

## 분석 항목

### 자동 검출

1. **detekt 위반**: `./gradlew :domain:detekt 2>&1 | grep "weighted issues"` — MagicNumber, ReturnCount, NoUnusedImports 등 정적 분석 위반
2. **테스트 커버리지**: `./gradlew :domain:test --tests "{패키지}.*" 2>&1 | tail -5` — 테스트 0건, 커버리지 < 50%
3. **nullable 남용**: `grep -rn "?: \|!!\|: .*?" src/main/kotlin/{대상}/ | wc -l` — 불필요한 `?` 타입, `!!` 사용
4. **deprecated 코드**: `grep -rn "@Deprecated\|@Suppress" src/main/kotlin/{대상}/ | wc -l` — @Deprecated 잔존, 사용되지 않는 코드
5. **@Query 사용 (컨벤션 위반)**: `grep -rn "@Query" src/main/kotlin/{대상}/ | wc -l` — QueryDSL 대신 @Query 직접 사용
6. **Swagger 누락**: `grep -rL "@Operation" src/main/kotlin/{대상}/controller/ | wc -l` — Controller에 Swagger 어노테이션 미적용

### 수동 분석

7. **코드 중복**: 유사 패턴 반복 (동일/유사 로직)
8. **과도한 복잡도**: 한 메서드 50줄 이상
9. **잘못된 레이어 의존**: Service에서 다른 Service 직접 호출
10. **네이밍 불일치**: 프로젝트 컨벤션과 맞지 않는 네이밍

## 작업 절차

1. 대상 모듈/패키지 범위를 확인한다.
2. 자동 검출 명령어를 순서대로 실행하고 결과를 수집한다.
3. 수동 분석 항목을 코드 리뷰 방식으로 확인한다.
4. 각 부채 항목에 대해 다음을 기록한다:
   - 부채 유형 (detekt/nullable/deprecated/@Query/Swagger/중복/복잡도/레이어/네이밍)
   - 파일 경로와 라인 수
   - 검출 근거 (도구 리포트 또는 코드 참조)
5. `debt_inventory.md`에 결과를 정리하여 출력한다.

## 품질 기준

- 모든 자동 검출 항목에 대해 실행 결과(숫자)가 포함되어야 한다.
- 각 부채 항목에 검출 근거(명령어 출력 또는 코드 참조)가 명시되어야 한다.
- 수동 분석 항목도 빠짐없이 확인하여 해당 사항 없으면 "해당 없음"으로 기록한다.
- 범위가 명확해야 한다 (전체 레포가 아닌, 지정된 모듈/패키지 단위).

## SQALE 방법론 (이중 비용 모델)

소스코드의 기술 부채를 "해소에 필요한 시간"과 "방치 시 비용"으로 이원화하여 정량화한다.

**두 가지 추정 모델**:

| 모델 | 설명 | 비유 |
|------|------|------|
| **Remediation Cost (해소 비용)** | 각 부채 항목을 해소하는 데 필요한 시간(분/시간 단위) | 부채의 원금(principal) |
| **Non-Remediation Cost (방치 비용)** | 부채를 방치할 때 발생하는 추가 비용. 코드를 다루는 모든 사람에게 부과되는 추가 작업량 | 부채의 이자(interest) |

**SQALE 품질 모델 구조** (3계층):
- Level 1: Characteristics (특성) — Testability, Reliability, Changeability, Efficiency, Security, Maintainability, Portability, Reusability
- Level 2: Sub-characteristics (하위 특성)
- Level 3: Requirements (요구사항) — 소스코드 내부 속성과 연결

**SQALE Rating**:
| 등급 | 부채 비율 | 해석 |
|------|----------|------|
| A | < 5% | 매우 건강 |
| B | 5-10% | 양호 |
| C | 10-20% | 주의 필요 |
| D | 20-50% | 심각 |
| E | > 50% | 즉시 조치 |

**적용 방법**:
- 각 부채 항목에 Remediation Cost(해소 시간)를 분 단위로 산정한다.
- Non-Remediation Cost는 "이 부채 때문에 매번 추가되는 시간"으로 산정한다.
- 총 기술 부채 = 모든 Remediation Cost의 합

**체크**:
- [ ] 각 부채 항목에 Remediation Cost(시간)를 부여했는가?
- [ ] 방치 시 비용(이자)을 추정했는가?
- [ ] 총 기술 부채를 시간 단위로 산정했는가?

## Martin Fowler 기술 부채 4분면

기술 부채를 2축(Deliberate/Inadvertent, Reckless/Prudent)으로 분류하여 대응 전략을 차별화한다.

```
                    Reckless                      Prudent
              +---------------------+---------------------+
  Deliberate  | "시간 없으니 설계    | "결과를 알지만 일정  |
              |  신경 안 쓴다"       |  을 위해 감수한다"   |
              | -> 즉시 해소 필수    | -> 계획적 상환 일정  |
              +---------------------+---------------------+
  Inadvertent | "레이어링이 뭔지     | "이제야 어떻게 했어  |
              |  모른다"             |  야 하는지 안다"     |
              | -> 교육 + 리팩토링   | -> 지속적 개선       |
              +---------------------+---------------------+
```

**적용 방법**:
- 검출된 각 부채 항목을 4분면에 배치한다.
- **Deliberate+Reckless**: 가장 높은 우선순위. 팀이 알면서도 무시한 것 → 즉시 해소
- **Deliberate+Prudent**: 의식적 결정. 상환 일정을 백로그에 등록
- **Inadvertent+Reckless**: 지식 부족. 교육 + 코드 리뷰 강화
- **Inadvertent+Prudent**: 자연스러운 학습 산물. 점진적 개선

**체크**:
- [ ] 각 부채 항목을 4분면에 분류했는가?
- [ ] 분류에 따른 대응 전략이 다른가?

## 자동 검출 도구 확장

detekt 외에 SonarQube, ktlint를 조합하여 코드 스멜을 자동 검출한다.

| 도구 | 검출 대상 | Greeting 적용 |
|------|----------|--------------|
| **detekt** | Kotlin 정적 분석 (MagicNumber, ReturnCount, LongMethod, ComplexCondition) | `./gradlew detekt` |
| **SonarQube** | Cognitive Complexity, 중복, 보안 취약점, 코드 스멜 | CI 파이프라인 |
| **ktlint** | Kotlin 코드 스타일, 포맷팅 규칙 | `./gradlew ktlintCheck` |
| **PMD/Checkstyle** | 스타일 규칙, 미사용 변수, 빈 catch 블록 | CI 파이프라인 |

**도구 조합 전략**:
- detekt: 로컬 개발 환경에서 즉시 피드백 (빌드 시 자동 실행)
- SonarQube: CI에서 종합 품질 게이트 (PR 머지 조건)
- ktlint: 코드 스타일 통일 (포맷팅 자동 수정)

**체크**:
- [ ] detekt 실행 결과를 수집했는가?
- [ ] Cognitive Complexity 점수를 메서드별로 확인했는가?
- [ ] 코드 중복률을 측정했는가?

## 부채 카탈로그 표준화

검출된 부채 항목을 아래 표준 형식으로 기록한다.

| 필드 | 설명 | 예시 |
|------|------|------|
| **카테고리** | detekt / nullable / deprecated / @Query / Swagger / 중복 / 복잡도 / 레이어 / 네이밍 / 테스트 | 복잡도 |
| **심각도** | Critical / Major / Minor | Major |
| **파일 경로** | 대상 파일 절대 경로 | `greeting-new-back/.../ApplicantService.kt` |
| **위치** | 라인 번호 또는 메서드명 | `#processStatusChange` |
| **검출 근거** | 도구 리포트 또는 코드 참조 | CC = 25 (detekt) |
| **Remediation Cost** | 해소 예상 시간 (분/시간) | 2h |
| **Non-Remediation Cost** | 방치 시 추가 비용/건 | 관련 기능 개발 시 매번 +30m |

## 공통 가이드 참조

- [문체/용어 규칙](../common/output-style.md)
