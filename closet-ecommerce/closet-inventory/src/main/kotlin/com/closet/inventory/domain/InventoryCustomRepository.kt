package com.closet.inventory.domain

interface InventoryCustomRepository {
    /**
     * 안전재고 이하인 재고 목록 조회.
     * WHERE deleted_at IS NULL AND available_quantity <= safety_threshold
     */
    fun findBelowSafetyThreshold(): List<Inventory>
}
