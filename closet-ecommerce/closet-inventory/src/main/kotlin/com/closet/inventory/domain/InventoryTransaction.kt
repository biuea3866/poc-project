package com.closet.inventory.domain

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
@Table(name = "inventory_transaction")
@EntityListeners(AuditingEntityListener::class)
class InventoryTransaction(
    @Column(name = "inventory_item_id", nullable = false)
    val inventoryItemId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    val type: TransactionType,

    @Column(name = "quantity", nullable = false)
    val quantity: Int,

    @Column(name = "reason", length = 500)
    val reason: String? = null,

    @Column(name = "reference_id", length = 100)
    val referenceId: String? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: LocalDateTime

    companion object {
        fun create(
            inventoryItemId: Long,
            type: TransactionType,
            quantity: Int,
            reason: String? = null,
            referenceId: String? = null,
        ): InventoryTransaction {
            return InventoryTransaction(
                inventoryItemId = inventoryItemId,
                type = type,
                quantity = quantity,
                reason = reason,
                referenceId = referenceId,
            )
        }
    }
}
