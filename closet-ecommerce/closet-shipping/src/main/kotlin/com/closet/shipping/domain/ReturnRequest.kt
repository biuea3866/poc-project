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
@Table(name = "return_request")
@EntityListeners(AuditingEntityListener::class)
class ReturnRequest(
    @Column(name = "order_id", nullable = false)
    val orderId: Long,

    @Column(name = "order_item_id", nullable = false)
    val orderItemId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val type: ReturnType,

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_type", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val reasonType: ReturnReasonType,

    @Column(name = "reason_detail", length = 500)
    val reasonDetail: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    var status: ReturnStatus = ReturnStatus.REQUESTED,

    @Column(name = "return_tracking_number", length = 50)
    var returnTrackingNumber: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "return_carrier", length = 30, columnDefinition = "VARCHAR(30)")
    var returnCarrier: Carrier? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_fee_bearer", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val shippingFeeBearer: ShippingFeeBearer,

    @Column(name = "return_shipping_fee", nullable = false)
    val returnShippingFee: Long = 0,

    @Column(name = "requested_at", nullable = false, columnDefinition = "DATETIME(6)")
    val requestedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "approved_at", columnDefinition = "DATETIME(6)")
    var approvedAt: LocalDateTime? = null,

    @Column(name = "rejected_at", columnDefinition = "DATETIME(6)")
    var rejectedAt: LocalDateTime? = null,
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

    fun approve() {
        if (status != ReturnStatus.REQUESTED) {
            throw BusinessException(
                ErrorCode.INVALID_STATE_TRANSITION,
                "반품 승인은 REQUESTED 상태에서만 가능합니다. 현재 상태: ${status.name}"
            )
        }
        this.status = ReturnStatus.APPROVED
        this.approvedAt = LocalDateTime.now()
    }

    fun reject() {
        if (status != ReturnStatus.REQUESTED) {
            throw BusinessException(
                ErrorCode.INVALID_STATE_TRANSITION,
                "반품 거절은 REQUESTED 상태에서만 가능합니다. 현재 상태: ${status.name}"
            )
        }
        this.status = ReturnStatus.REJECTED
        this.rejectedAt = LocalDateTime.now()
    }

    companion object {
        fun create(
            orderId: Long,
            orderItemId: Long,
            type: ReturnType,
            reasonType: ReturnReasonType,
            reasonDetail: String? = null,
            shippingFeeBearer: ShippingFeeBearer,
            returnShippingFee: Long = 0,
        ): ReturnRequest {
            return ReturnRequest(
                orderId = orderId,
                orderItemId = orderItemId,
                type = type,
                reasonType = reasonType,
                reasonDetail = reasonDetail,
                shippingFeeBearer = shippingFeeBearer,
                returnShippingFee = returnShippingFee,
            )
        }
    }
}
