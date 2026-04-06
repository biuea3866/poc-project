package com.closet.promotion.application

import com.closet.promotion.domain.point.PointBalance
import com.closet.promotion.domain.point.PointTransactionType
import com.closet.promotion.presentation.dto.CancelPointRequest
import com.closet.promotion.presentation.dto.EarnPointRequest
import com.closet.promotion.presentation.dto.PointBalanceResponse
import com.closet.promotion.presentation.dto.PointHistoryResponse
import com.closet.promotion.presentation.dto.UsePointRequest
import com.closet.promotion.repository.PointBalanceRepository
import com.closet.promotion.repository.PointHistoryRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class PointService(
    private val pointBalanceRepository: PointBalanceRepository,
    private val pointHistoryRepository: PointHistoryRepository,
) {
    fun getBalance(memberId: Long): PointBalanceResponse {
        val balance = getOrCreateBalance(memberId)
        return PointBalanceResponse.from(balance)
    }

    fun getHistory(memberId: Long): List<PointHistoryResponse> {
        return pointHistoryRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
            .map { PointHistoryResponse.from(it) }
    }

    @Transactional
    fun earn(request: EarnPointRequest): PointHistoryResponse {
        val balance = getOrCreateBalance(request.memberId)
        val history = balance.earn(request.amount)

        if (request.referenceType != null && request.referenceId != null) {
            history.withReference(request.referenceType, request.referenceId)
        }

        pointBalanceRepository.save(balance)
        val savedHistory = pointHistoryRepository.save(history)

        logger.info { "적립금 적립: memberId=${request.memberId}, amount=${request.amount}, balance=${balance.availablePoints}" }
        return PointHistoryResponse.from(savedHistory)
    }

    @Transactional
    fun use(request: UsePointRequest): PointHistoryResponse {
        val balance = getOrCreateBalance(request.memberId)
        val history = balance.use(request.amount)

        if (request.referenceType != null && request.referenceId != null) {
            history.withReference(request.referenceType, request.referenceId)
        }

        pointBalanceRepository.save(balance)
        val savedHistory = pointHistoryRepository.save(history)

        logger.info { "적립금 사용: memberId=${request.memberId}, amount=${request.amount}, balance=${balance.availablePoints}" }
        return PointHistoryResponse.from(savedHistory)
    }

    @Transactional
    fun cancel(request: CancelPointRequest): PointHistoryResponse {
        val balance = getOrCreateBalance(request.memberId)

        val history =
            when (request.transactionType) {
                PointTransactionType.CANCEL_EARN -> balance.cancelEarn(request.amount)
                PointTransactionType.CANCEL_USE -> balance.cancelUse(request.amount)
                else -> throw IllegalArgumentException("취소 요청에는 CANCEL_EARN 또는 CANCEL_USE만 사용할 수 있습니다")
            }

        if (request.referenceType != null && request.referenceId != null) {
            history.withReference(request.referenceType, request.referenceId)
        }

        pointBalanceRepository.save(balance)
        val savedHistory = pointHistoryRepository.save(history)

        logger.info { "적립금 취소: memberId=${request.memberId}, type=${request.transactionType}, amount=${request.amount}" }
        return PointHistoryResponse.from(savedHistory)
    }

    private fun getOrCreateBalance(memberId: Long): PointBalance {
        return pointBalanceRepository.findByMemberId(memberId)
            .orElseGet {
                pointBalanceRepository.save(PointBalance.create(memberId))
            }
    }
}
