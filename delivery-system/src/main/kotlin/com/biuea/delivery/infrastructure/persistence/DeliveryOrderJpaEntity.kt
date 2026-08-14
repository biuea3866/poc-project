package com.biuea.delivery.infrastructure.persistence

import com.biuea.delivery.domain.order.DeliveryOrder
import com.biuea.delivery.domain.order.DeliveryOrderStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.ZonedDateTime

/**
 * 주문 영속화 엔티티. 도메인 모델(DeliveryOrder)과 분리해 domain 레이어가 JPA 를 모르게 한다.
 *
 * version 은 낙관적 락의 판정 근거다. UPDATE 시 `WHERE version = ?` 이 붙고,
 * 영향 행이 0이면 Hibernate 가 충돌로 판단해 예외를 던진다.
 */
@Entity
@Table(name = "delivery_orders")
class DeliveryOrderJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long?,

    @Column(name = "store_id", nullable = false)
    var storeId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: DeliveryOrderStatus,

    @Column(name = "assigned_rider_id")
    var assignedRiderId: Long?,

    @Version
    @Column(name = "version", nullable = false)
    var version: Long,

    @Column(name = "created_at", nullable = false)
    var createdAt: ZonedDateTime,

    @Column(name = "assigned_at")
    var assignedAt: ZonedDateTime?,
) {
    fun toDomain(): DeliveryOrder = DeliveryOrder.reconstitute(
        id = requireNotNull(id) { "영속화되지 않은 주문은 도메인으로 복원할 수 없습니다." },
        storeId = storeId,
        status = status,
        assignedRiderId = assignedRiderId,
        version = version,
        createdAt = createdAt,
        assignedAt = assignedAt,
    )

    companion object {
        fun from(order: DeliveryOrder): DeliveryOrderJpaEntity = DeliveryOrderJpaEntity(
            id = order.id,
            storeId = order.storeId,
            status = order.currentStatus,
            assignedRiderId = order.currentAssignedRiderId,
            version = order.version,
            createdAt = order.createdAt,
            assignedAt = order.currentAssignedAt,
        )
    }
}
