package com.closet.promotion.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.promotion.domain.timesale.TimeSale
import com.closet.promotion.domain.timesale.TimeSaleStatus
import com.closet.promotion.presentation.dto.CreateTimeSaleRequest
import com.closet.promotion.presentation.dto.TimeSaleResponse
import com.closet.promotion.repository.TimeSaleRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class TimeSaleService(
    private val timeSaleRepository: TimeSaleRepository,
) {
    @Transactional
    fun createTimeSale(request: CreateTimeSaleRequest): TimeSaleResponse {
        val timeSale =
            TimeSale.create(
                productId = request.productId,
                salePrice = request.salePrice,
                limitQuantity = request.limitQuantity,
                startAt = request.startAt,
                endAt = request.endAt,
            )

        val saved = timeSaleRepository.save(timeSale)
        logger.info { "타임세일 생성 완료: timeSaleId=${saved.id}, productId=${saved.productId}" }
        return TimeSaleResponse.from(saved)
    }

    fun getActiveTimeSales(): List<TimeSaleResponse> {
        return timeSaleRepository.findByStatus(TimeSaleStatus.ACTIVE)
            .map { TimeSaleResponse.from(it) }
    }

    @Transactional
    fun purchaseTimeSale(timeSaleId: Long): TimeSaleResponse {
        val timeSale =
            timeSaleRepository.findById(timeSaleId)
                .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "타임세일을 찾을 수 없습니다. id=$timeSaleId") }

        timeSale.purchase()

        logger.info { "타임세일 구매 완료: timeSaleId=$timeSaleId, soldCount=${timeSale.soldCount}" }
        return TimeSaleResponse.from(timeSale)
    }
}
