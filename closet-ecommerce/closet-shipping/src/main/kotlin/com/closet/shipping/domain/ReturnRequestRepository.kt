package com.closet.shipping.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface ReturnRequestRepository : JpaRepository<ReturnRequest, Long> {

    fun findByOrderId(orderId: Long): List<ReturnRequest>

    fun findByOrderIdAndStatusNotIn(orderId: Long, statuses: Collection<ReturnStatus>): List<ReturnRequest>

    fun findByIdAndSellerId(id: Long, sellerId: Long): Optional<ReturnRequest>
}
