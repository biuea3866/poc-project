package com.closet.order.repository

import com.closet.order.domain.order.Order
import com.closet.order.domain.order.OrderStatus
import com.closet.order.domain.order.QOrder.order
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.ZonedDateTime

class OrderCustomRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : OrderCustomRepository {
    /**
     * 자동 구매확정 대상 조회 (PD-16, PD-17).
     * DELIVERED 상태 + deliveredAt이 cutoff(168시간) 이전 + 반품/교환 미진행.
     */
    override fun findAutoConfirmCandidates(cutoff: ZonedDateTime): List<Order> =
        queryFactory.selectFrom(order)
            .where(
                order.status.eq(OrderStatus.DELIVERED),
                order.deletedAt.isNull,
                order.deliveredAt.isNotNull,
                order.deliveredAt.loe(cutoff),
            )
            .fetch()
}
