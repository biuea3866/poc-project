package com.closet.inventory.domain

import org.springframework.data.jpa.repository.JpaRepository

interface InventoryRepository : JpaRepository<Inventory, Long> {

    fun findByProductOptionIdAndDeletedAtIsNull(productOptionId: Long): Inventory?

    fun findBySkuAndDeletedAtIsNull(sku: String): Inventory?

    fun findByIdAndDeletedAtIsNull(id: Long): Inventory?

    fun findByProductIdAndDeletedAtIsNull(productId: Long): List<Inventory>

    fun findByAvailableQuantityLessThanEqualAndDeletedAtIsNull(threshold: Int): List<Inventory>
}
