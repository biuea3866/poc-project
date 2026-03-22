package com.closet.settlement.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.settlement.domain.settlement.Settlement
import com.closet.settlement.domain.settlement.SettlementItem
import com.closet.settlement.domain.settlement.SettlementStatus
import com.closet.settlement.presentation.dto.CalculateSettlementRequest
import com.closet.settlement.presentation.dto.SettlementResponse
import com.closet.settlement.repository.SettlementItemRepository
import com.closet.settlement.repository.SettlementRepository
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class SettlementService(
    private val settlementRepository: SettlementRepository,
    private val settlementItemRepository: SettlementItemRepository,
    private val commissionRateService: CommissionRateService,
) {

    @Transactional
    fun calculate(request: CalculateSettlementRequest): SettlementResponse {
        val settlement = Settlement.create(
            sellerId = request.sellerId,
            periodFrom = request.periodFrom,
            periodTo = request.periodTo,
        )
        val savedSettlement = settlementRepository.save(settlement)

        val items = request.items.map { itemReq ->
            val commissionRate = commissionRateService.getRateValue(itemReq.categoryId)
            SettlementItem.create(
                settlementId = savedSettlement.id,
                orderId = itemReq.orderId,
                orderItemId = itemReq.orderItemId,
                saleAmount = itemReq.saleAmount,
                commissionRate = commissionRate,
            )
        }

        val savedItems = items.map { settlementItemRepository.save(it) }

        savedSettlement.calculate(savedItems, request.totalRefund)

        logger.info { "정산 계산 완료: settlementId=${savedSettlement.id}, sellerId=${request.sellerId}, netAmount=${savedSettlement.netAmount}" }
        return SettlementResponse.from(savedSettlement, savedItems)
    }

    fun findById(id: Long): SettlementResponse {
        val settlement = settlementRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "정산을 찾을 수 없습니다. id=$id") }
        val items = settlementItemRepository.findBySettlementId(settlement.id)
        return SettlementResponse.from(settlement, items)
    }

    fun findBySeller(sellerId: Long, status: SettlementStatus?, pageable: Pageable): Page<SettlementResponse> {
        return if (status != null) {
            settlementRepository.findBySellerIdAndStatus(sellerId, status, pageable)
        } else {
            settlementRepository.findBySellerId(sellerId, pageable)
        }.map { settlement ->
            val items = settlementItemRepository.findBySettlementId(settlement.id)
            SettlementResponse.from(settlement, items)
        }
    }

    fun findByPeriod(periodFrom: LocalDateTime, periodTo: LocalDateTime, pageable: Pageable): Page<SettlementResponse> {
        return settlementRepository
            .findByPeriodFromGreaterThanEqualAndPeriodToLessThanEqual(periodFrom, periodTo, pageable)
            .map { settlement ->
                val items = settlementItemRepository.findBySettlementId(settlement.id)
                SettlementResponse.from(settlement, items)
            }
    }

    @Transactional
    fun confirm(id: Long): SettlementResponse {
        val settlement = settlementRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "정산을 찾을 수 없습니다. id=$id") }
        settlement.confirm()
        val items = settlementItemRepository.findBySettlementId(settlement.id)
        logger.info { "정산 확정 완료: settlementId=${settlement.id}" }
        return SettlementResponse.from(settlement, items)
    }

    @Transactional
    fun pay(id: Long): SettlementResponse {
        val settlement = settlementRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "정산을 찾을 수 없습니다. id=$id") }
        settlement.pay()
        val items = settlementItemRepository.findBySettlementId(settlement.id)
        logger.info { "정산 지급 완료: settlementId=${settlement.id}" }
        return SettlementResponse.from(settlement, items)
    }
}
