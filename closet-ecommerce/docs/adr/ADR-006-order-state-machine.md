# ADR-006: 주문 상태 머신 설계

## 상태: 승인

## 컨텍스트
주문은 생성부터 완료까지 여러 상태를 거치며, 잘못된 상태 전이는 비즈니스 오류를 유발한다.
- 주문 상태: PENDING → PAID → PREPARING → SHIPPING → DELIVERED → CONFIRMED
- 취소/환불 상태: CANCEL_REQUESTED → CANCELLED, RETURN_REQUESTED → RETURNED
- 상태 전이 규칙을 안전하게 관리해야 함

## 결정
주문 상태를 Kotlin enum에 캡슐화하고, `canTransitionTo` / `validateTransitionTo` 메서드로 전이 규칙을 관리한다.

```kotlin
enum class OrderStatus {
    PENDING,
    PAID,
    PREPARING,
    SHIPPING,
    DELIVERED,
    CONFIRMED,
    CANCEL_REQUESTED,
    CANCELLED,
    RETURN_REQUESTED,
    RETURNED;

    fun canTransitionTo(target: OrderStatus): Boolean =
        allowedTransitions[this]?.contains(target) ?: false

    fun validateTransitionTo(target: OrderStatus) {
        if (!canTransitionTo(target))
            throw InvalidOrderStateTransitionException(this, target)
    }

    companion object {
        private val allowedTransitions = mapOf(
            PENDING to setOf(PAID, CANCEL_REQUESTED),
            PAID to setOf(PREPARING, CANCEL_REQUESTED),
            PREPARING to setOf(SHIPPING, CANCEL_REQUESTED),
            SHIPPING to setOf(DELIVERED),
            DELIVERED to setOf(CONFIRMED, RETURN_REQUESTED),
            CONFIRMED to emptySet(),
            CANCEL_REQUESTED to setOf(CANCELLED),
            CANCELLED to emptySet(),
            RETURN_REQUESTED to setOf(RETURNED),
            RETURNED to emptySet(),
        )
    }
}
```

## 이유
- 상태 전이 규칙이 enum 내부에 캡슐화되어 단일 진실 공급원(Single Source of Truth)
- `canTransitionTo`로 가능 여부 체크, `validateTransitionTo`로 예외 발생 — 용도에 따라 선택
- Service 계층에 상태 전이 로직이 흩어지는 것을 방지
- 새로운 상태 추가 시 enum과 전이 맵만 수정하면 됨

## 결과
- Order 엔티티의 `changeStatus()` 메서드에서 `validateTransitionTo()` 호출
- 상태 전이 실패 시 `InvalidOrderStateTransitionException` (에러 코드: O003)
- 상태 전이 이벤트 발행 가능 (향후 이벤트 소싱 확장점)
- 같은 패턴을 결제 상태(`PaymentStatus`), 상품 상태(`ProductStatus`)에도 적용

## 대안 (검토했으나 선택하지 않은 것)

### Spring Statemachine
- 장점: 이벤트 기반 상태 머신, 가드/액션 등 풍부한 기능
- 단점: 러닝 커브 높음, 단순 상태 전이에 과도한 추상화
- 기각 사유: 4개 도메인의 상태 전이가 비교적 단순하여 enum으로 충분

### if-else / switch 분기
- 장점: 직관적, 추가 라이브러리 불필요
- 단점: 상태/전이 증가 시 분기문 폭발, 실수 위험
- 기각 사유: 유지보수성 저하, 상태 전이 규칙이 코드 전반에 흩어짐

### 별도 상태 전이 테이블 (DB)
- 장점: 런타임에 전이 규칙 변경 가능
- 단점: DB 조회 오버헤드, 과도한 유연성
- 기각 사유: 상태 전이 규칙은 비즈니스 로직이므로 코드로 관리하는 것이 적절
