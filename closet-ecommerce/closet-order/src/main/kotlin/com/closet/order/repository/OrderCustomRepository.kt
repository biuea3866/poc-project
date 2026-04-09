package com.closet.order.repository

import com.closet.order.domain.order.Order
import java.time.ZonedDateTime

interface OrderCustomRepository {
    fun findAutoConfirmCandidates(cutoff: ZonedDateTime): List<Order>
}
