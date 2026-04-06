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
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

@Entity
@Table(name = "shipment")
@EntityListeners(AuditingEntityListener::class)
class Shipment(
    @Column(name = "order_id", nullable = false, unique = true)
    val orderId: Long,
    @Column(name = "seller_id", nullable = false)
    val sellerId: Long,
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Column(name = "carrier", length = 20)
    var carrier: String? = null,
    @Column(name = "tracking_number", length = 30)
    var trackingNumber: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    var status: ShippingStatus = ShippingStatus.READY,
    @Column(name = "receiver_name", nullable = false, length = 50)
    val receiverName: String,
    @Column(name = "receiver_phone", nullable = false, length = 20)
    val receiverPhone: String,
    @Column(name = "zip_code", nullable = false, length = 10)
    val zipCode: String,
    @Column(name = "address", nullable = false, length = 200)
    val address: String,
    @Column(name = "detail_address", nullable = false, length = 200)
    val detailAddress: String,
    @Column(name = "shipped_at", columnDefinition = "DATETIME(6)")
    var shippedAt: ZonedDateTime? = null,
    @Column(name = "delivered_at", columnDefinition = "DATETIME(6)")
    var deliveredAt: ZonedDateTime? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: ZonedDateTime

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    lateinit var updatedAt: ZonedDateTime

    /**
     * 이미 송장이 등록되어 있는지 확인.
     */
    fun hasTrackingNumber(): Boolean = trackingNumber != null

    /**
     * 송장 등록 (배송 시작).
     * 택배사 API 호출 후 반환된 송장번호를 세팅한다.
     * 중복 등록 시 IllegalStateException을 던진다.
     */
    fun registerTracking(
        carrier: String,
        trackingNumber: String,
    ) {
        check(!hasTrackingNumber()) {
            "이미 송장이 등록된 배송입니다: orderId=$orderId, existing=$this.trackingNumber"
        }
        this.carrier = carrier
        this.trackingNumber = trackingNumber
        this.status = ShippingStatus.READY
        this.shippedAt = ZonedDateTime.now()
    }

    /**
     * 배송 상태 전이.
     */
    fun updateStatus(newStatus: ShippingStatus) {
        status.validateTransitionTo(newStatus)
        status = newStatus
        if (newStatus == ShippingStatus.DELIVERED) {
            deliveredAt = ZonedDateTime.now()
        }
    }

    /**
     * 구매확정 대상 여부 확인.
     * deliveredAt + 168시간 경과 (PD-16)
     */
    fun isAutoConfirmEligible(now: ZonedDateTime): Boolean {
        val da = deliveredAt ?: return false
        return status == ShippingStatus.DELIVERED && da.plusHours(168).isBefore(now)
    }

    companion object {
        fun create(
            orderId: Long,
            sellerId: Long,
            memberId: Long,
            receiverName: String,
            receiverPhone: String,
            zipCode: String,
            address: String,
            detailAddress: String,
        ): Shipment {
            return Shipment(
                orderId = orderId,
                sellerId = sellerId,
                memberId = memberId,
                receiverName = receiverName,
                receiverPhone = receiverPhone,
                zipCode = zipCode,
                address = address,
                detailAddress = detailAddress,
            )
        }
    }
}
