package com.closet.inventory.repository

import com.closet.inventory.domain.InventoryItem
import org.springframework.data.jpa.repository.JpaRepository

interface InventoryItemRepository : JpaRepository<InventoryItem, Long> {
    fun findByProductOptionId(productOptionId: Long): InventoryItem?
    fun findByProductOptionIdIn(productOptionIds: List<Long>): List<InventoryItem>
}
