package com.biuea.delivery.domain.order

/**
 * 배달 요청 수락의 동시성 제어 전략. 구현체(비관적 락·낙관적 락·분산락)는 infrastructure 에 둔다.
 * 도메인이 "단일 승자여야 한다"는 계약만 정의하고, 무엇으로 보장할지는 기술 선택으로 남긴다.
 */
interface DeliveryAcceptStrategy {
    fun accept(orderId: Long, riderId: Long): AcceptOutcome
}
