# Closet E-Commerce 테스트 전략

## 1. 테스트 피라미드

```
         ┌─────────┐
         │  E2E    │  10% — Full API flow (REST Assured / MockMvc)
         │  Test   │
        ─┼─────────┼─
        │Integration│  20% — Repository + DB, Kafka, Redis (Testcontainers)
        │   Test    │
      ──┼───────────┼──
      │  Unit Test  │  70% — Domain model, Service logic (Kotest + MockK)
      │             │
      └─────────────┘
```

### Unit Test (70%)
- **대상**: Domain Entity, Value Object, Service, Enum 상태 전이
- **도구**: Kotest (BehaviorSpec), MockK
- **원칙**:
  - 외부 의존성은 MockK으로 격리
  - 도메인 로직(상태 전이, 금액 계산, 유효성 검증)은 반드시 단위 테스트
  - 하나의 테스트는 하나의 행위만 검증

### Integration Test (20%)
- **대상**: Repository, DB 쿼리, Kafka Producer/Consumer, Redis 캐시
- **도구**: Testcontainers (MySQL, Redis, Kafka), SpringBootTest
- **원칙**:
  - `BaseIntegrationTest` 싱글턴 컨테이너 패턴 사용
  - 테스트 간 데이터 격리 (`@Transactional` 롤백 또는 truncate)
  - 실제 DB 쿼리 실행으로 QueryDSL/JPA 동작 검증

### E2E Test (10%)
- **대상**: 전체 API 플로우 (멀티 서비스 시나리오)
- **도구**: REST Assured 또는 MockMvc
- **원칙**:
  - Happy Path + 주요 실패 시나리오만
  - 테스트 데이터는 Fixture로 사전 준비
  - CI 파이프라인에서 별도 stage로 실행

---

## 2. 서비스별 테스트 케이스 매트릭스

### 2.1 Member Service

| ID | 유형 | 테스트명 | Given | When | Then | 우선순위 |
|----|------|---------|-------|------|------|---------|
| TC-M001 | Unit | 회원가입 성공 | 유효한 이메일/비밀번호 | 회원가입 API 호출 | 201 + JWT 반환 | P0 |
| TC-M002 | Unit | 중복 이메일 회원가입 | 이미 존재하는 이메일 | 회원가입 | 409 Conflict | P0 |
| TC-M003 | Unit | 로그인 성공 | 등록된 회원 | 로그인 | 200 + Access/Refresh Token | P0 |
| TC-M004 | Unit | 잘못된 비밀번호 로그인 | 등록된 회원 | 틀린 비밀번호로 로그인 | 401 Unauthorized | P0 |
| TC-M005 | Integration | 배송지 추가 | 로그인된 회원 | 배송지 등록 | 201 + 배송지 ID | P1 |
| TC-M006 | Integration | 기본 배송지 설정 | 배송지 2개 | 두번째를 기본으로 설정 | 기존 기본 해제 + 새 기본 설정 | P1 |
| TC-M007 | Integration | 회원 탈퇴 | 로그인된 회원 | 탈퇴 API | soft delete + 로그인 불가 | P1 |

### 2.2 Product Service

| ID | 유형 | 테스트명 | Given | When | Then | 우선순위 |
|----|------|---------|-------|------|------|---------|
| TC-P001 | Unit | 상품 등록 성공 | 유효한 상품 정보 | 상품 등록 API | 201 + 상품 ID (DRAFT 상태) | P0 |
| TC-P002 | Unit | 옵션 추가 | DRAFT 상품 | 사이즈/색상 옵션 추가 | 옵션 목록에 포함 | P0 |
| TC-P003 | Unit | 상태 전이 DRAFT->ACTIVE | DRAFT 상품 | 활성화 요청 | ACTIVE 상태 | P0 |
| TC-P004 | Unit | 상태 전이 ACTIVE->SOLD_OUT | ACTIVE 상품, 재고 0 | 품절 처리 | SOLD_OUT 상태 | P0 |
| TC-P005 | Unit | 상태 전이 SOLD_OUT->ACTIVE | SOLD_OUT 상품, 재고 보충 | 재활성화 | ACTIVE 상태 | P1 |
| TC-P006 | Unit | 상태 전이 ACTIVE->INACTIVE | ACTIVE 상품 | 비활성화 | INACTIVE 상태 | P1 |
| TC-P007 | Unit | 잘못된 상태 전이 거부 | INACTIVE 상품 | SOLD_OUT 전환 시도 | 예외 발생 | P0 |
| TC-P008 | Integration | 카테고리 필터 조회 | 여러 카테고리 상품 | 특정 카테고리 필터 | 해당 카테고리만 반환 | P1 |
| TC-P009 | Integration | 브랜드 필터 조회 | 여러 브랜드 상품 | 특정 브랜드 필터 | 해당 브랜드만 반환 | P1 |
| TC-P010 | Integration | 가격 범위 필터 + 페이지네이션 | 다양한 가격 상품 | 가격 범위 + 페이지 요청 | 범위 내 상품 + 페이지 메타 | P1 |

### 2.3 Order Service

| ID | 유형 | 테스트명 | Given | When | Then | 우선순위 |
|----|------|---------|-------|------|------|---------|
| TC-O001 | Unit | 장바구니 상품 추가 | 로그인된 회원 | 상품 장바구니 추가 | 장바구니에 상품 포함 | P0 |
| TC-O002 | Unit | 장바구니 수량 변경 | 장바구니에 상품 존재 | 수량 변경 | 변경된 수량 반영 | P1 |
| TC-O003 | Unit | 장바구니 상품 삭제 | 장바구니에 상품 존재 | 상품 삭제 | 장바구니에서 제거 | P1 |
| TC-O004 | Unit | 장바구니 전체 조회 | 장바구니에 상품 3개 | 조회 | 3개 상품 + 총 금액 | P0 |
| TC-O005 | Unit | 주문 생성 | 장바구니 상품 선택 | 주문 생성 | PENDING 상태 주문 + 주문번호 | P0 |
| TC-O006 | Unit | 주문번호 유니크 | 동시 주문 생성 | 주문 생성 2건 | 서로 다른 주문번호 | P0 |
| TC-O007 | Unit | 주문 취소 (PAID 상태) | PAID 주문 | 취소 요청 | CANCELLED 상태 + 재고 복원 | P0 |
| TC-O008 | Unit | 주문 취소 거부 (SHIPPED) | SHIPPED 주문 | 취소 요청 | 예외 발생 (배송중 취소 불가) | P0 |
| TC-O009 | Unit | 잘못된 상태 전이 거부 | CANCELLED 주문 | PAID 전환 시도 | 예외 발생 | P0 |
| TC-O010 | Unit | 부분 취소 | PAID 주문, 상품 3개 | 1개 부분 취소 | 해당 아이템 CANCELLED + 나머지 유지 | P1 |
| TC-O011 | Integration | 주문 저장 및 조회 | 주문 생성 | DB 저장 후 조회 | 저장된 주문 일치 | P1 |
| TC-O012 | Integration | 주문 목록 조회 | 회원의 주문 5건 | 주문 목록 조회 | 5건 반환 + 최신순 정렬 | P1 |

### 2.4 Payment Service

| ID | 유형 | 테스트명 | Given | When | Then | 우선순위 |
|----|------|---------|-------|------|------|---------|
| TC-PAY001 | Unit | 결제 승인 성공 | PENDING 주문 | 결제 승인 요청 | APPROVED 상태 + 결제 ID | P0 |
| TC-PAY002 | Unit | 중복 승인 멱등성 | 이미 APPROVED 결제 | 동일 결제 재승인 | 기존 결제 반환 (중복 처리 X) | P0 |
| TC-PAY003 | Unit | 결제 취소 (APPROVED만) | APPROVED 결제 | 취소 요청 | CANCELLED 상태 | P0 |
| TC-PAY004 | Unit | 결제 취소 거부 (PENDING) | PENDING 결제 | 취소 요청 | 예외 발생 | P0 |
| TC-PAY005 | Unit | 환불 처리 | APPROVED 결제 | 환불 요청 | REFUNDED 상태 + 환불 금액 | P0 |
| TC-PAY006 | Unit | 잘못된 상태 전이 거부 | REFUNDED 결제 | 승인 시도 | 예외 발생 | P0 |
| TC-PAY007 | Integration | 결제 정보 저장 및 조회 | 결제 승인 | DB 저장 후 조회 | 저장된 결제 일치 | P1 |
| TC-PAY008 | Integration | 주문별 결제 조회 | 주문 3건의 결제 | 특정 주문 결제 조회 | 해당 주문 결제만 반환 | P1 |

---

## 3. E2E 시나리오

| ID | 시나리오명 | 플로우 | 검증 포인트 | 우선순위 |
|----|-----------|--------|------------|---------|
| E2E-001 | Happy Path 전체 구매 | 회원가입 -> 상품 조회 -> 장바구니 추가 -> 주문 생성 -> 결제 승인 | 각 단계 정상 응답 + 최종 주문 PAID 상태 | P0 |
| E2E-002 | 결제 실패 시 재고 복원 | 주문 생성 -> 결제 실패 -> 재고 확인 | 주문 FAILED + 재고 원복 | P0 |
| E2E-003 | 주문 취소 및 환불 | 주문 -> 결제 승인 -> 주문 취소 -> 환불 | 주문 CANCELLED + 결제 REFUNDED | P0 |
| E2E-004 | 품절 상품 주문 시도 | 품절(SOLD_OUT) 상품 -> 주문 시도 | 주문 거부 + 적절한 에러 메시지 | P1 |

---

## 4. 부하 테스트 시나리오

| ID | 시나리오명 | 조건 | 성공 기준 | 도구 |
|----|-----------|------|----------|------|
| LOAD-001 | 상품 조회 동시 부하 | 동시 100명 상품 목록 조회 | p99 < 200ms, 에러율 0% | k6 / Gatling |
| LOAD-002 | 동시 주문 재고 정합성 | 동시 10명 같은 SKU 주문 (재고 5개) | 정확히 5건 성공, 5건 실패, 재고 0 | k6 / Gatling |
| LOAD-003 | 타임세일 동시 쿠폰 발급 | 동시 100명 선착순 쿠폰 50장 발급 | 정확히 50건 발급, 50건 실패, 중복 발급 없음 | k6 / Gatling |

---

## 5. 커버리지 목표

| 지표 | 목표 | 비고 |
|------|------|------|
| Line Coverage | 80%+ | JaCoCo 기준 |
| Branch Coverage | 70%+ | 상태 전이 분기 포함 |
| P0 테스트 | 100% 통과 필수 | **배포 차단** — CI에서 P0 실패 시 머지 블록 |
| P1 테스트 | 경고 | 가급적 통과, 실패 시 리뷰어 판단 |

---

## 6. CI 파이프라인 통합

```
┌──────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────┐
│  Build   │ -> │  Unit Test   │ -> │ Integration  │ -> │  E2E     │
│  (Gradle)│    │  (Kotest)    │    │  Test (TC)   │    │  Test    │
└──────────┘    └──────────────┘    └──────────────┘    └──────────┘
                      │                    │                   │
                 P0 실패 시             P0 실패 시          P0 실패 시
                 빌드 중단             빌드 중단           빌드 중단
```

### 실행 방법
```bash
# 전체 테스트
./gradlew test

# 단위 테스트만
./gradlew test --tests "*.unit.*"

# 통합 테스트만
./gradlew test --tests "*.integration.*"

# 커버리지 리포트
./gradlew jacocoTestReport
```

---

## 7. 테스트 작성 가이드

### Kotest BehaviorSpec 템플릿
```kotlin
class SomeServiceTest : BehaviorSpec({
    val mockRepository = mockk<SomeRepository>()
    val service = SomeService(mockRepository)

    Given("어떤 상태에서") {
        // 사전 조건 설정

        When("어떤 행위를 하면") {
            // 행위 실행

            Then("어떤 결과가 나와야 한다") {
                // 검증
            }
        }
    }
})
```

### Fixture 사용 가이드
```kotlin
// 기본 Fixture 사용
val member = MemberFixture.createMember()

// 커스텀 값으로 오버라이드
val member = MemberFixture.createMember(
    email = "custom@test.com",
    nickname = "custom"
)
```

### Testcontainers 사용 가이드
```kotlin
// BaseIntegrationTest 상속으로 자동 설정
class MemberRepositoryTest : BaseIntegrationTest() {
    // MySQL, Redis 컨테이너 자동 시작
    // application.yml 설정 자동 주입
}
```
