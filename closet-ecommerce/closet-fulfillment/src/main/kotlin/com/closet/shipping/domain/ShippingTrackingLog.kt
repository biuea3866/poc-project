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
import java.time.ZonedDateTime

/**
 * 배송 추적 이력.
 *
 * carrierStatus: Mock 서버 원본 상태 (ACCEPTED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED)
 * mappedStatus: ShippingStatus로 매핑된 상위 상태
 */
@Entity
@Table(name = "shipping_tracking_log")
@EntityListeners(AuditingEntityListener::class)
class ShippingTrackingLog(
    @Column(name = "shipping_id", nullable = false)
    val shippingId: Long,
    @Column(name = "carrier_status", nullable = false, length = 30)
    val carrierStatus: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "mapped_status", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    val mappedStatus: ShippingStatus,
    @Column(name = "location", length = 200)
    val location: String? = null,
    @Column(name = "description", length = 500)
    val description: String? = null,
    @Column(name = "tracked_at", nullable = false, columnDefinition = "DATETIME(6)")
    val trackedAt: ZonedDateTime,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: ZonedDateTime

    companion object {
        fun create(
            shippingId: Long,
            carrierStatus: String,
            mappedStatus: ShippingStatus,
            location: String? = null,
            description: String? = null,
            trackedAt: ZonedDateTime = ZonedDateTime.now(),
        ): ShippingTrackingLog {
            return ShippingTrackingLog(
                shippingId = shippingId,
                carrierStatus = carrierStatus,
                mappedStatus = mappedStatus,
                location = location,
                description = description,
                trackedAt = trackedAt,
            )
        }
    }
}
