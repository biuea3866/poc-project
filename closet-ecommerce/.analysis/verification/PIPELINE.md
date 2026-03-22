# 검증 파이프라인

> 구현된 코드 → 설계 원칙 검증 → 아키텍처 검증 → 티켓 AC 검증 → 리뷰 리포트

---

## 실행 원칙

- **입력**: 구현된 코드 (PR 또는 브랜치)
- **출력**: 검증 리포트 (통과/위반 목록 + 수정 제안)
- **Phase 1~4 전체를 순서대로 수행**. 위반 없으면 해당 Phase는 "검증 통과"로 간결하게 기록.

---

## 파이프라인 흐름

```
[INPUT] PR URL 또는 브랜치 + 관련 티켓 번호
    │
    ▼
━━ Phase 1: 설계 원칙 검증 ━━━━━━━━━━━━━━━━━━━━━━━━━━━
    │  be-implementation/PIPELINE.md의 "Closet 프로젝트 설계 원칙" 기준으로 검증
    │
    ▼
━━ Phase 2: 아키텍처 검증 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    │  TDD에 정의된 아키텍처와 실제 구현의 일치 여부 확인
    │
    ▼
━━ Phase 3: 티켓 AC 검증 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    │  해당 티켓의 AC 체크리스트를 코드 기반으로 하나씩 확인
    │
    ▼
━━ Phase 4: 검증 리포트 생성 ━━━━━━━━━━━━━━━━━━━━━━━━━
    │  통과/위반 요약 + 수정 제안
    │
    ▼
[OUTPUT] 검증 리포트 (results/)
```

---

## Phase 1: 설계 원칙 검증

be-implementation/PIPELINE.md "Closet 프로젝트 설계 원칙" 9개 카테고리를 자동 검증한다.

### 1.1 DB 설계 검증

| 검증 항목 | 방법 | 위반 판정 |
|----------|------|----------|
| FK 미사용 | DDL에서 `FOREIGN KEY` 검색 | 1건이라도 존재하면 위반 |
| JSON 미사용 | DDL에서 `JSON` 타입 검색 | 존재하면 위반 |
| ENUM 미사용 | DDL에서 `ENUM` 타입 검색 | 존재하면 위반 |
| BOOLEAN 미사용 | DDL에서 `BOOLEAN` 검색 | TINYINT(1) 대신 BOOLEAN이면 위반 |
| COMMENT 필수 | DDL에서 COMMENT 없는 컬럼 검색 | 1개라도 없으면 위반 |
| DATETIME(6) | DDL에서 시간 컬럼 정밀도 확인 | DATETIME(6) 아니면 위반 |

```bash
# 자동 검증 예시
grep -c "FOREIGN KEY" {flyway_file}     # 0이어야 통과
grep -c "JSON" {flyway_file}             # 0이어야 통과
grep -c "ENUM(" {flyway_file}            # 0이어야 통과
```

### 1.2 도메인 모델 검증

| 검증 항목 | 방법 | 위반 판정 |
|----------|------|----------|
| 엔티티 캡슐화 | Service에서 `entity.status =` 직접 할당 검색 | setter 직접 호출이면 위반 (엔티티 메서드 사용해야) |
| enum 캡슐화 | Status enum에 `canTransitionTo` 존재 확인 | 없으면 위반 |
| Service 얇음 | Service 클래스에서 `if`/`when` 분기 수 확인 | 비즈니스 분기가 3개 이상이면 경고 |
| BC 분리 | Service의 DI 확인 | 다른 BC의 Repository 주입이면 위반 |

```bash
# 자동 검증 예시
# Service에서 status 직접 할당하는지 확인
grep -rn "\.status = " {service_files}   # 0이어야 통과 (엔티티 메서드 사용해야)

# enum에 canTransitionTo 있는지
grep -c "canTransitionTo" {status_enum_file}  # 1 이상이어야 통과
```

### 1.3 레이어 아키텍처 검증

| 검증 항목 | 방법 | 위반 판정 |
|----------|------|----------|
| Controller → Facade만 | Controller의 DI 확인 | Service 직접 주입이면 위반 |
| Facade 비즈니스 로직 없음 | Facade에서 `if`/`when`/DB접근 확인 | 존재하면 위반 |

### 1.4 이벤트 검증

| 검증 항목 | 방법 | 위반 판정 |
|----------|------|----------|
| 터미널 상태만 발행 | KafkaTemplate.send 호출 지점의 상태 확인 | 중간 상태에서 발행하면 위반 |
| Facade에서 발행 안 함 | Facade 코드에서 KafkaTemplate 확인 | 존재하면 위반 |

### 1.5 Feature Flag 검증

| 검증 항목 | 방법 | 위반 판정 |
|----------|------|----------|
| SimpleRuntimeConfig 사용 | `@ConfigurationProperties` 검색 | Feature Flag에 사용하면 위반 |
| BooleanFeatureKey 사용 | `FeatureFlagService.getFlag` 검색 | 다른 방식이면 위반 |

### 1.6 스케줄러 검증

| 검증 항목 | 방법 | 위반 판정 |
|----------|------|----------|
| @Scheduled 미사용 | `@Scheduled` 검색 | 존재하면 위반 |
| Tasklet 사용 | `Tasklet` 구현체 확인 | 없으면 위반 |
| Facade 재사용 | Tasklet에서 Facade 호출 확인 | 직접 로직이면 위반 |

---

## Phase 2: 아키텍처 검증

해당 프로젝트의 TDD와 실제 구현을 대조한다.

### 검증 항목

| 항목 | 확인 방법 |
|------|----------|
| 패키지 구조 | TDD 4.2 디렉토리 구조와 실제 파일 경로 비교 |
| 엔티티 필드 | TDD DDL 컬럼과 JPA 엔티티 필드 1:1 매핑 확인 |
| API 엔드포인트 | TDD API 스펙과 실제 Controller 매핑 확인 |
| 의존 관계 | TDD Bounded Context와 실제 DI 의존 확인 |
| 상태 머신 | TDD 상태 전이와 enum 전이 규칙 일치 확인 |

### 검증 코드 예시

```kotlin
// 자동으로 확인 가능한 항목
fun verifyEntityMatchesDdl(entityClass: KClass<*>, ddlFile: File) {
    val entityFields = entityClass.memberProperties.map { it.name }
    val ddlColumns = parseDdlColumns(ddlFile)  // snake_case → camelCase 변환

    val missing = ddlColumns - entityFields.toSet()
    val extra = entityFields.toSet() - ddlColumns

    require(missing.isEmpty()) { "엔티티에 누락된 컬럼: $missing" }
    require(extra.isEmpty()) { "DDL에 없는 필드: $extra" }
}
```

---

## Phase 3: 티켓 AC 검증

해당 티켓의 AC(Acceptance Criteria) 체크리스트를 하나씩 코드 기반으로 확인한다.

### 검증 방법

```
티켓 AC 예시:
  - [ ] OrderService는 OrderRepository, OrderStatusHistoryRepository만 의존
  - [ ] ProductService, PaymentService를 의존하지 않음
  - [ ] 모든 상태 전이가 엔티티 메서드를 통해 수행됨

검증:
  1. OrderService.kt 파일 읽기
  2. constructor DI 확인 → OrderRepository, OrderStatusHistoryRepository만 있는지
  3. import에 ProductService, PaymentService 없는지
  4. ".status = " 직접 할당 없는지
  → AC 충족 여부 판정
```

### AC 유형별 검증 전략

| AC 유형 | 검증 방법 |
|---------|----------|
| "X만 의존" | constructor DI + import 분석 |
| "X에 비즈니스 로직 없음" | if/when 분기 + DB접근 검색 |
| "X 테이블에 COMMENT" | DDL 파싱 |
| "빌드 성공" | `./gradlew compileKotlin` 실행 |
| "테스트 통과" | `./gradlew test --tests "..."` 실행 |
| "Kafka 이벤트 발행" | KafkaTemplate.send 호출 + 토픽명 확인 |
| "@Scheduled 미사용" | grep 검색 |

---

## Phase 4: 검증 리포트

### 리포트 구조

```markdown
# 검증 리포트: {티켓번호} {제목}

> 검증일: {날짜}
> 대상: {PR URL 또는 브랜치}
> 티켓: {티켓 파일 경로}

## 요약

| 카테고리 | 검증 항목 | 통과 | 위반 | 경고 |
|---------|---------|------|------|------|
| 설계 원칙 | 15 | 14 | 1 | 0 |
| 아키텍처 | 8 | 8 | 0 | 0 |
| 티켓 AC | 6 | 5 | 0 | 1 |
| **합계** | **29** | **27** | **1** | **1** |

## 위반 상세

| # | 카테고리 | 항목 | 위반 내용 | 수정 제안 | 심각도 |
|---|---------|------|----------|----------|--------|
| V-1 | DB 설계 | COMMENT 필수 | inventory.version 컬럼에 COMMENT 없음 | COMMENT '낙관적 락 버전' 추가 | Minor |

## 경고 상세

| # | 카테고리 | 항목 | 경고 내용 | 제안 |
|---|---------|------|----------|------|
| W-1 | 도메인 모델 | Service 얇음 | OrderService에 if 분기 4개 (기준: 3개) | 엔티티 메서드로 이동 검토 |

## AC 체크리스트

- [x] OrderService는 OrderRepository만 의존
- [x] 모든 상태 전이가 엔티티 메서드를 통해 수행됨
- [x] Soft Delete 대상에 @SQLRestriction 적용
- [x] 빌드 성공 (BUILD SUCCESSFUL)
- [x] 테스트 통과 (6 tests passed)
- [ ] inventory.version COMMENT 누락
```

### 결과 저장

```
.analysis/verification/results/{날짜}_{티켓번호}/
├── verification_report.md     ← 검증 리포트
├── design_check.md            ← Phase 1 상세
├── architecture_check.md      ← Phase 2 상세
└── ac_check.md                ← Phase 3 상세
```

---

## pr-review 파이프라인과의 관계

| 파이프라인 | 시점 | 관점 | 대상 |
|----------|------|------|------|
| **verification** (이 파이프라인) | PR 생성 전 (구현 직후) | 설계 원칙 + AC 충족 | 단일 티켓의 구현물 |
| **pr-review** (기존) | PR 생성 후 | 코드 품질 + 영향 범위 + 사이드 이펙트 | PR 전체 diff |

```
구현 완료
  → verification 파이프라인 (설계 원칙 + AC 검증)
    → 통과 시 PR 생성
      → pr-review 파이프라인 (코드 리뷰)
        → 통과 시 머지
```

---

## 출력 어조

산출물은 **팀원과 공유하는 문서**입니다.

- **핵심부터** — 통과/위반 요약을 맨 위에.
- **짧게** — 통과 항목은 체크마크만. 위반만 상세히.
- **구체적으로** — "COMMENT 누락" 대신 "inventory.version 컬럼에 COMMENT 없음".
- **수정 제안 포함** — 위반 지적만 하지 말고 구체적 수정 방법 제시.
