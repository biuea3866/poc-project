package com.closet.settlement.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.settlement.domain.commission.CommissionRate
import com.closet.settlement.presentation.dto.CommissionRateResponse
import com.closet.settlement.repository.CommissionRateRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class CommissionRateService(
    private val commissionRateRepository: CommissionRateRepository,
) {

    fun getRate(categoryId: Long): CommissionRateResponse {
        val rate = commissionRateRepository
            .findTopByCategoryIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(categoryId, LocalDateTime.now())
            ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "수수료율을 찾을 수 없습니다. categoryId=$categoryId")
        return CommissionRateResponse.from(rate)
    }

    fun getRateValue(categoryId: Long): BigDecimal {
        return getRate(categoryId).rate
    }

    fun getAllRates(): List<CommissionRateResponse> {
        return commissionRateRepository.findAll().map { CommissionRateResponse.from(it) }
    }

    @Transactional
    fun setRate(categoryId: Long, rate: BigDecimal, effectiveFrom: LocalDateTime?): CommissionRateResponse {
        val commissionRate = CommissionRate.create(
            categoryId = categoryId,
            rate = rate,
            effectiveFrom = effectiveFrom ?: LocalDateTime.now(),
        )
        val saved = commissionRateRepository.save(commissionRate)
        logger.info { "수수료율 설정 완료: categoryId=$categoryId, rate=$rate" }
        return CommissionRateResponse.from(saved)
    }
}
