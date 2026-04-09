package com.closet.inventory.domain

import com.closet.inventory.domain.QInventory.inventory
import com.querydsl.jpa.impl.JPAQueryFactory

class InventoryCustomRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : InventoryCustomRepository {
    override fun findBelowSafetyThreshold(): List<Inventory> {
        return queryFactory.selectFrom(inventory)
            .where(
                inventory.deletedAt.isNull,
                inventory.availableQuantity.loe(inventory.safetyThreshold),
            )
            .fetch()
    }
}
