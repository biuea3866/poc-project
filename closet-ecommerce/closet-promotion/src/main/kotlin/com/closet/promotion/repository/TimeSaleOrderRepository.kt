package com.closet.promotion.repository

import com.closet.promotion.domain.timesale.TimeSaleOrder
import org.springframework.data.jpa.repository.JpaRepository

interface TimeSaleOrderRepository : JpaRepository<TimeSaleOrder, Long> {
    fun findByTimeSaleId(timeSaleId: Long): List<TimeSaleOrder>

    fun findByMemberId(memberId: Long): List<TimeSaleOrder>

    fun existsByTimeSaleIdAndMemberIdAndOrderId(
        timeSaleId: Long,
        memberId: Long,
        orderId: Long,
    ): Boolean
}
