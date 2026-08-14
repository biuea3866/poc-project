package com.biuea.delivery.domain.order

import java.time.ZonedDateTime

/**
 * 배달 주문. 라이더 수락(배차)의 단일 승자 규칙을 스스로 지킨다.
 *
 * JPA 애노테이션은 이 클래스에 붙이지 않는다. 영속화는 infrastructure 의 DeliveryOrderJpaEntity 가 담당하고,
 * domain 은 프레임워크를 모른 채 상태 전이 규칙만 갖는다(레이어 의존 방향 유지).
 * 낙관적 락에 쓰이는 version 은 도메인이 값으로만 들고 다니며, 충돌 판정은 영속화 계층이 수행한다.
 */
class DeliveryOrder private constructor(
    val id: Long?,
    val storeId: Long,
    private var status: DeliveryOrderStatus,
    private var assignedRiderId: Long?,
    val version: Long,
    val createdAt: ZonedDateTime,
    private var assignedAt: ZonedDateTime?,
) {
    val currentStatus: DeliveryOrderStatus get() = status
    val currentAssignedRiderId: Long? get() = assignedRiderId
    val currentAssignedAt: ZonedDateTime? get() = assignedAt

    /**
     * 라이더에게 배차한다. 여러 라이더가 동시에 수락해도 WAITING_RIDER 인 순간 1명만 통과한다.
     * 시각은 인자로 받지 않고 내부에서 확정한다 — 호출부가 배차 시각을 조작할 여지를 없앤다.
     */
    fun assignTo(riderId: Long) {
        if (!status.canAssignRider()) {
            throw DeliveryOrderNotAssignableException(id, status, assignedRiderId)
        }
        this.status = DeliveryOrderStatus.ASSIGNED
        this.assignedRiderId = riderId
        this.assignedAt = ZonedDateTime.now()
    }

    companion object {
        private const val INITIAL_VERSION = 0L

        /** 가게가 조리를 마쳐 라이더에게 브로드캐스트되는 시점의 주문을 만든다. */
        fun waitingRider(storeId: Long): DeliveryOrder = DeliveryOrder(
            id = null,
            storeId = storeId,
            status = DeliveryOrderStatus.WAITING_RIDER,
            assignedRiderId = null,
            version = INITIAL_VERSION,
            createdAt = ZonedDateTime.now(),
            assignedAt = null,
        )

        /** 영속화된 주문을 복원한다(무검증). */
        fun reconstitute(
            id: Long,
            storeId: Long,
            status: DeliveryOrderStatus,
            assignedRiderId: Long?,
            version: Long,
            createdAt: ZonedDateTime,
            assignedAt: ZonedDateTime?,
        ): DeliveryOrder = DeliveryOrder(
            id = id,
            storeId = storeId,
            status = status,
            assignedRiderId = assignedRiderId,
            version = version,
            createdAt = createdAt,
            assignedAt = assignedAt,
        )
    }
}
