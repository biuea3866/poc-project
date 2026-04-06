package com.closet.promotion.domain.coupon

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
import java.time.ZonedDateTime

@Entity
@Table(name = "member_coupon")
@EntityListeners(AuditingEntityListener::class)
class MemberCoupon(
    @Column(name = "coupon_id", nullable = false)
    val couponId: Long,
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    var status: MemberCouponStatus = MemberCouponStatus.AVAILABLE,
    @Column(name = "used_order_id")
    var usedOrderId: Long? = null,
    @Column(name = "issued_at", nullable = false, columnDefinition = "DATETIME(6)")
    val issuedAt: ZonedDateTime,
    @Column(name = "used_at", columnDefinition = "DATETIME(6)")
    var usedAt: ZonedDateTime? = null,
    @Column(name = "expired_at", nullable = false, columnDefinition = "DATETIME(6)")
    val expiredAt: ZonedDateTime,
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

    fun isUsable(): Boolean = status == MemberCouponStatus.AVAILABLE && ZonedDateTime.now().isBefore(expiredAt)

    fun use(orderId: Long) {
        if (!isUsable()) {
            throw BusinessException(
                ErrorCode.INVALID_STATE_TRANSITION,
                "사용할 수 없는 쿠폰입니다. status=${status.name}",
            )
        }
        status = MemberCouponStatus.USED
        usedOrderId = orderId
        usedAt = ZonedDateTime.now()
    }

    fun expire() {
        if (status != MemberCouponStatus.AVAILABLE) {
            throw BusinessException(
                ErrorCode.INVALID_STATE_TRANSITION,
                "만료할 수 없는 쿠폰입니다. status=${status.name}",
            )
        }
        status = MemberCouponStatus.EXPIRED
    }
}
