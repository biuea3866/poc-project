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
import java.time.LocalDateTime

@Entity
@Table(name = "inventory_item")
@EntityListeners(AuditingEntityListener::class)
class InventoryItem(
    @Column(name = "product_option_id", nullable = false, unique = true)
    val productOptionId: Long,

    @Column(name = "total_quantity", nullable = false)
    var totalQuantity: Int = 0,

    @Column(name = "available_quantity", nullable = false)
    var availableQuantity: Int = 0,

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
    lateinit var createdAt: LocalDateTime

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    lateinit var updatedAt: LocalDateTime

    /**
     * 재고 예약: 주문 시 가용 재고에서 예약 재고로 이동
     * 불변 조건: availableQuantity >= 0
     */
    fun reserve(quantity: Int, referenceId: String): InventoryTransaction {
        require(quantity > 0) { "예약 수량은 0보다 커야 합니다" }

        if (!isAvailable(quantity)) {
            throw BusinessException(
                ErrorCode.INVALID_INPUT,
                "재고가 부족합니다. 요청: $quantity, 가용: $availableQuantity (productOptionId=$productOptionId)"
            )
        }

        availableQuantity -= quantity
        reservedQuantity += quantity

        return InventoryTransaction.create(
            inventoryItemId = this.id,
            type = TransactionType.RESERVE,
            quantity = quantity,
            reason = "주문 재고 예약",
            referenceId = referenceId,
        )
    }

    /**
     * 예약 해제: 취소/반품 시 예약 재고를 가용 재고로 복원
     */
    fun release(quantity: Int, referenceId: String): InventoryTransaction {
        require(quantity > 0) { "해제 수량은 0보다 커야 합니다" }

        if (reservedQuantity < quantity) {
            throw BusinessException(
                ErrorCode.INVALID_INPUT,
                "예약된 재고보다 많은 수량을 해제할 수 없습니다. 요청: $quantity, 예약: $reservedQuantity (productOptionId=$productOptionId)"
            )
        }

        reservedQuantity -= quantity
        availableQuantity += quantity

        return InventoryTransaction.create(
            inventoryItemId = this.id,
            type = TransactionType.RELEASE,
            quantity = quantity,
            reason = "주문 취소/반품에 의한 재고 해제",
            referenceId = referenceId,
        )
    }

    /**
     * 재고 차감: 결제 확정 시 예약 재고에서 실물 재고 차감
     */
    fun deduct(quantity: Int, referenceId: String): InventoryTransaction {
        require(quantity > 0) { "차감 수량은 0보다 커야 합니다" }

        if (reservedQuantity < quantity) {
            throw BusinessException(
                ErrorCode.INVALID_INPUT,
                "예약된 재고보다 많은 수량을 차감할 수 없습니다. 요청: $quantity, 예약: $reservedQuantity (productOptionId=$productOptionId)"
            )
        }

        reservedQuantity -= quantity
        totalQuantity -= quantity

        return InventoryTransaction.create(
            inventoryItemId = this.id,
            type = TransactionType.OUTBOUND,
            quantity = quantity,
            reason = "결제 확정에 의한 재고 차감",
            referenceId = referenceId,
        )
    }

    /**
     * 입고: 실물 재고 및 가용 재고 증가
     */
    fun restock(quantity: Int, reason: String): InventoryTransaction {
        require(quantity > 0) { "입고 수량은 0보다 커야 합니다" }

        totalQuantity += quantity
        availableQuantity += quantity

        return InventoryTransaction.create(
            inventoryItemId = this.id,
            type = TransactionType.INBOUND,
            quantity = quantity,
            reason = reason,
        )
    }

    /**
     * 가용 재고가 요청 수량 이상인지 확인
     */
    fun isAvailable(quantity: Int): Boolean {
        return availableQuantity >= quantity
    }

    /**
     * 안전재고 임계값 이하인지 확인
     */
    fun isBelowSafetyThreshold(): Boolean {
        return availableQuantity <= safetyThreshold
    }

    companion object {
        fun create(
            productOptionId: Long,
            totalQuantity: Int = 0,
            safetyThreshold: Int = 10,
        ): InventoryItem {
            return InventoryItem(
                productOptionId = productOptionId,
                totalQuantity = totalQuantity,
                availableQuantity = totalQuantity,
                reservedQuantity = 0,
                safetyThreshold = safetyThreshold,
            )
        }
    }
}
