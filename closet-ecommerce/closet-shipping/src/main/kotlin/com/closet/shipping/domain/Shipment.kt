package com.closet.shipping.domain

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
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
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "shipment")
@EntityListeners(AuditingEntityListener::class)
class Shipment(
    @Column(name = "order_id", nullable = false)
    val orderId: Long,

    @Column(name = "seller_id", nullable = false)
    val sellerId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "carrier", length = 30, columnDefinition = "VARCHAR(30)")
    var carrier: Carrier? = null,

    @Column(name = "tracking_number", length = 50)
    var trackingNumber: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    var status: ShipmentStatus = ShipmentStatus.PENDING,

    @Column(name = "receiver_name", nullable = false, length = 50)
    val receiverName: String,

    @Column(name = "receiver_phone", nullable = false, length = 20)
    val receiverPhone: String,

    @Column(name = "address", nullable = false, length = 500)
    val address: String,

    @Column(name = "shipped_at", columnDefinition = "DATETIME(6)")
    var shippedAt: LocalDateTime? = null,

    @Column(name = "delivered_at", columnDefinition = "DATETIME(6)")
    var deliveredAt: LocalDateTime? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: LocalDateTime

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    lateinit var updatedAt: LocalDateTime

    fun registerTracking(carrier: Carrier, trackingNumber: String) {
        if (this.status != ShipmentStatus.PENDING) {
            throw BusinessException(
                ErrorCode.INVALID_STATE_TRANSITION,
                "송장 등록은 PENDING 상태에서만 가능합니다. 현재 상태: ${status.name}"
            )
        }
        this.carrier = carrier
        this.trackingNumber = trackingNumber
        transitionTo(ShipmentStatus.READY)
    }

    fun updateStatus(newStatus: ShipmentStatus) {
        transitionTo(newStatus)
        if (newStatus == ShipmentStatus.PICKED_UP) {
            this.shippedAt = LocalDateTime.now()
        }
    }

    fun completeDelivery() {
        transitionTo(ShipmentStatus.DELIVERED)
        this.deliveredAt = LocalDateTime.now()
    }

    private fun transitionTo(newStatus: ShipmentStatus) {
        status.validateTransitionTo(newStatus)
        status = newStatus
    }

    companion object {
        fun create(
            orderId: Long,
            sellerId: Long,
            receiverName: String,
            receiverPhone: String,
            address: String,
        ): Shipment {
            return Shipment(
                orderId = orderId,
                sellerId = sellerId,
                receiverName = receiverName,
                receiverPhone = receiverPhone,
                address = address,
            )
        }
    }
}
