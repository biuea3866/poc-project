package com.closet.inventory.repository

import com.closet.inventory.domain.InventoryTransaction
import org.springframework.data.jpa.repository.JpaRepository

interface InventoryTransactionRepository : JpaRepository<InventoryTransaction, Long> {
    fun findByInventoryItemId(inventoryItemId: Long): List<InventoryTransaction>
    fun findByReferenceId(referenceId: String): List<InventoryTransaction>
}
