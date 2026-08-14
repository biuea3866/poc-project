package com.biuea.delivery.domain.order

/**
 * 라이더 수락 시도의 결과.
 *
 * 경합에서 진 것은 장애가 아니라 정상 시나리오다. 그래서 실패를 예외로 던지지 않고 결과 타입으로 표현한다.
 * 전략(비관적·낙관적·분산락)이 달라도 호출부가 받는 결과의 종류는 같아야 한다.
 */
sealed class AcceptOutcome {

    /** 단일 승자. retryCount 는 이 라이더가 성공하기까지 겪은 버전 충돌 횟수다. */
    data class Accepted(val orderId: Long, val riderId: Long, val retryCount: Int) : AcceptOutcome()

    /** 다른 라이더가 먼저 가져갔다. 패자에게 승자를 알려줘야 앱이 "이미 배차된 요청" 을 정확히 안내한다. */
    data class AlreadyAssigned(val orderId: Long, val assignedRiderId: Long) : AcceptOutcome()

    /** 취소·조리 중처럼 애초에 수락 대상이 아닌 상태였다. */
    data class NotAcceptableStatus(val orderId: Long, val currentStatus: DeliveryOrderStatus) : AcceptOutcome()

    /** 락을 잡지 못해 수락 시도 자체를 포기했다. */
    data class LockAcquisitionFailed(val orderId: Long) : AcceptOutcome()

    /** 낙관적 락에서 재시도 상한까지 충돌이 이어졌다. */
    data class RetryExhausted(val orderId: Long, val attemptCount: Int) : AcceptOutcome()

    data class OrderNotFound(val orderId: Long) : AcceptOutcome()

    /** 성공 결과에만 재시도 횟수를 채운다. 실패 결과는 재시도 횟수로 달라질 게 없다. */
    fun withRetryCount(retryCount: Int): AcceptOutcome =
        if (this is Accepted) copy(retryCount = retryCount) else this

    companion object {
        fun fromRejection(orderId: Long, exception: DeliveryOrderNotAssignableException): AcceptOutcome =
            exception.assignedRiderId
                ?.let { AlreadyAssigned(orderId, it) }
                ?: NotAcceptableStatus(orderId, exception.currentStatus)
    }
}
