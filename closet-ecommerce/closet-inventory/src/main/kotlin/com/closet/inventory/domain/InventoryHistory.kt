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
import java.time.ZonedDateTime

@Entity
@Table(name = "inventory_history")
@EntityListeners(AuditingEntityListener::class)
class InventoryHistory(
    @Column(name = "inventory_id", nullable = false)
    val inventoryId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val changeType: ChangeType,
    @Column(name = "quantity", nullable = false)
    val quantity: Int,
    @Column(name = "before_total", nullable = false)
    val beforeTotal: Int,
    @Column(name = "after_total", nullable = false)
    val afterTotal: Int,
    @Column(name = "before_available", nullable = false)
    val beforeAvailable: Int,
    @Column(name = "after_available", nullable = false)
    val afterAvailable: Int,
    @Column(name = "before_reserved", nullable = false)
    val beforeReserved: Int,
    @Column(name = "after_reserved", nullable = false)
    val afterReserved: Int,
    @Column(name = "reference_id", length = 100)
    val referenceId: String? = null,
    @Column(name = "reference_type", length = 50)
    val referenceType: String? = null,
    @Column(name = "reason", length = 500)
    val reason: String? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: ZonedDateTime

    companion object {
        fun create(
            inventoryId: Long,
            changeType: ChangeType,
            quantity: Int,
            beforeTotal: Int,
            afterTotal: Int,
            beforeAvailable: Int,
            afterAvailable: Int,
            beforeReserved: Int,
            afterReserved: Int,
            referenceId: String? = null,
            referenceType: String? = null,
            reason: String? = null,
        ): InventoryHistory {
            return InventoryHistory(
                inventoryId = inventoryId,
                changeType = changeType,
                quantity = quantity,
                beforeTotal = beforeTotal,
                afterTotal = afterTotal,
                beforeAvailable = beforeAvailable,
                afterAvailable = afterAvailable,
                beforeReserved = beforeReserved,
                afterReserved = afterReserved,
                referenceId = referenceId,
                referenceType = referenceType,
                reason = reason,
            )
        }
    }
}
