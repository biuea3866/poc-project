package com.closet.promotion.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.promotion.domain.timesale.TimeSaleOrder
import com.closet.promotion.presentation.dto.PurchaseTimeSaleRequest
import com.closet.promotion.presentation.dto.TimeSaleOrderResponse
import com.closet.promotion.repository.TimeSaleOrderRepository
import com.closet.promotion.repository.TimeSaleRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class TimeSaleFacade(
    private val timeSaleRepository: TimeSaleRepository,
    private val timeSaleOrderRepository: TimeSaleOrderRepository,
) {
    @Transactional
    fun purchaseWithOrder(
        timeSaleId: Long,
        request: PurchaseTimeSaleRequest,
    ): TimeSaleOrderResponse {
        val timeSale =
            timeSaleRepository.findById(timeSaleId)
                .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "타임세일을 찾을 수 없습니다. id=$timeSaleId") }

        repeat(request.quantity) { timeSale.purchase() }

        val order =
            TimeSaleOrder.create(
                timeSaleId = timeSale.id,
                orderId = request.orderId,
                memberId = request.memberId,
                quantity = request.quantity,
            )

        val saved = timeSaleOrderRepository.save(order)

        logger.info { "타임세일 구매 완료: timeSaleId=$timeSaleId, orderId=${request.orderId}, quantity=${request.quantity}" }
        return TimeSaleOrderResponse.from(saved)
    }

    fun getOrdersByTimeSale(timeSaleId: Long): List<TimeSaleOrderResponse> {
        return timeSaleOrderRepository.findByTimeSaleId(timeSaleId)
            .map { TimeSaleOrderResponse.from(it) }
    }

    fun getOrdersByMember(memberId: Long): List<TimeSaleOrderResponse> {
        return timeSaleOrderRepository.findByMemberId(memberId)
            .map { TimeSaleOrderResponse.from(it) }
    }
}
