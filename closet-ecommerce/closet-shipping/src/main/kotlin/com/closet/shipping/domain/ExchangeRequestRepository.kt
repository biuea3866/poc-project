package com.closet.shipping.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface ExchangeRequestRepository : JpaRepository<ExchangeRequest, Long> {

    fun findByOrderId(orderId: Long): List<ExchangeRequest>

    fun findByOrderIdAndStatusNotIn(orderId: Long, statuses: Collection<ExchangeStatus>): List<ExchangeRequest>

    fun findByIdAndSellerId(id: Long, sellerId: Long): Optional<ExchangeRequest>
}
