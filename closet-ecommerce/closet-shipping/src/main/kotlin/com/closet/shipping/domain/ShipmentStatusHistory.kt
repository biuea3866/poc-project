package com.closet.shipping.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "shipment_status_history")
@EntityListeners(AuditingEntityListener::class)
class ShipmentStatusHistory(
    @Column(name = "shipment_id", nullable = false)
    val shipmentId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30, columnDefinition = "VARCHAR(30)")
    val fromStatus: ShipmentStatus?,

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val toStatus: ShipmentStatus,

    @Column(name = "reason", length = 500)
    val reason: String? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: LocalDateTime

    companion object {
        fun create(
            shipmentId: Long,
            fromStatus: ShipmentStatus?,
            toStatus: ShipmentStatus,
            reason: String? = null,
        ): ShipmentStatusHistory {
            return ShipmentStatusHistory(
                shipmentId = shipmentId,
                fromStatus = fromStatus,
                toStatus = toStatus,
                reason = reason,
            )
        }
    }
}
