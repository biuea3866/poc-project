package com.closet.inventory.domain

import org.springframework.data.jpa.repository.JpaRepository

interface InventoryHistoryRepository : JpaRepository<InventoryHistory, Long> {
    fun findByInventoryIdOrderByCreatedAtDesc(inventoryId: Long): List<InventoryHistory>

    fun findByReferenceIdAndReferenceType(
        referenceId: String,
        referenceType: String,
    ): List<InventoryHistory>
}
