package com.closet.inventory.domain

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

@Entity
@Table(name = "inventory")
@EntityListeners(AuditingEntityListener::class)
class Inventory(
    @Column(name = "product_id", nullable = false)
    val productId: Long,
    @Column(name = "product_option_id", nullable = false)
    val productOptionId: Long,
    @Column(name = "sku", nullable = false, length = 50)
    val sku: String,
    @Column(name = "total_quantity", nullable = false)
    var totalQuantity: Int,
    @Column(name = "available_quantity", nullable = false)
    var availableQuantity: Int,
    @Column(name = "reserved_quantity", nullable = false)
    var reservedQuantity: Int = 0,
    @Column(name = "safety_threshold", nullable = false)
    var safetyThreshold: Int = 10,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: ZonedDateTime

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    lateinit var updatedAt: ZonedDateTime

    @Column(name = "deleted_at", columnDefinition = "DATETIME(6)")
    var deletedAt: ZonedDateTime? = null

    /**
     * 재고 예약 (주문 생성 시).
     * available -= quantity, reserved += quantity
     */
    fun reserve(quantity: Int) {
        require(quantity > 0) { "예약 수량은 0보다 커야 합니다" }
        if (availableQuantity < quantity) {
            throw InsufficientStockException(
                productOptionId = productOptionId,
                sku = sku,
                requested = quantity,
                available = availableQuantity,
            )
        }
        availableQuantity -= quantity
        reservedQuantity += quantity
        validateInvariant()
    }

    /**
     * 재고 차감 (결제 완료 시).
     * reserved -= quantity, total -= quantity
     */
    fun deduct(quantity: Int) {
        require(quantity > 0) { "차감 수량은 0보다 커야 합니다" }
        if (reservedQuantity < quantity) {
            throw BusinessException(
                ErrorCode.INVALID_STATE_TRANSITION,
                "예약된 재고가 부족합니다. reserved=$reservedQuantity, requested=$quantity",
            )
        }
        reservedQuantity -= quantity
        totalQuantity -= quantity
        validateInvariant()
    }

    /**
     * 재고 해제 (주문 취소 시).
     * reserved -= quantity, available += quantity
     */
    fun release(quantity: Int) {
        require(quantity > 0) { "해제 수량은 0보다 커야 합니다" }
        if (reservedQuantity < quantity) {
            throw BusinessException(
                ErrorCode.INVALID_STATE_TRANSITION,
                "예약된 재고가 부족합니다. reserved=$reservedQuantity, requested=$quantity",
            )
        }
        reservedQuantity -= quantity
        availableQuantity += quantity
        validateInvariant()
    }

    /**
     * 입고.
     * total += quantity, available += quantity
     * @return 이전 available (0이었는지 체크를 위해)
     */
    fun inbound(quantity: Int): Int {
        require(quantity > 0) { "입고 수량은 0보다 커야 합니다" }
        val previousAvailable = availableQuantity
        totalQuantity += quantity
        availableQuantity += quantity
        validateInvariant()
        return previousAvailable
    }

    /**
     * 반품 양품 복구.
     * total += quantity, available += quantity
     * @return 이전 available (0이었는지 체크를 위해)
     */
    fun returnRestore(quantity: Int): Int {
        require(quantity > 0) { "복구 수량은 0보다 커야 합니다" }
        val previousAvailable = availableQuantity
        totalQuantity += quantity
        availableQuantity += quantity
        validateInvariant()
        return previousAvailable
    }

    /**
     * 안전재고 이하 여부 확인.
     */
    fun isBelowSafetyThreshold(): Boolean {
        return availableQuantity <= safetyThreshold
    }

    /**
     * 품절 여부 확인.
     */
    fun isOutOfStock(): Boolean {
        return availableQuantity == 0
    }

    /**
     * 불변 조건: totalQuantity == availableQuantity + reservedQuantity
     */
    private fun validateInvariant() {
        check(totalQuantity == availableQuantity + reservedQuantity) {
            "재고 불변 조건 위반: total=$totalQuantity, available=$availableQuantity, reserved=$reservedQuantity"
        }
        check(availableQuantity >= 0) { "가용 재고는 0 이상이어야 합니다: $availableQuantity" }
        check(reservedQuantity >= 0) { "예약 재고는 0 이상이어야 합니다: $reservedQuantity" }
    }

    companion object {
        fun create(
            productId: Long,
            productOptionId: Long,
            sku: String,
            totalQuantity: Int,
            safetyThreshold: Int = 10,
        ): Inventory {
            require(totalQuantity >= 0) { "초기 수량은 0 이상이어야 합니다" }
            return Inventory(
                productId = productId,
                productOptionId = productOptionId,
                sku = sku,
                totalQuantity = totalQuantity,
                availableQuantity = totalQuantity,
                reservedQuantity = 0,
                safetyThreshold = safetyThreshold,
            )
        }
    }
}
