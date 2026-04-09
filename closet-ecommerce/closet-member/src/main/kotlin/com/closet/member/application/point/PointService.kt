package com.closet.member.application.point

import com.closet.member.domain.point.PointBalance
import com.closet.member.domain.point.PointReferenceType
import com.closet.member.domain.point.PointTransactionType
import com.closet.member.domain.repository.MemberRepository
import com.closet.member.domain.repository.PointBalanceRepository
import com.closet.member.domain.repository.PointHistoryRepository
import com.closet.member.presentation.dto.CancelPointRequest
import com.closet.member.presentation.dto.EarnPointRequest
import com.closet.member.presentation.dto.PointBalanceResponse
import com.closet.member.presentation.dto.PointHistoryResponse
import com.closet.member.presentation.dto.UsePointRequest
import mu.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

private val logger = KotlinLogging.logger {}

/**
 * 통합 적립금 서비스.
 *
 * PointBalance를 권위있는 원장(ledger)으로 사용하며,
 * 일반 적립/사용/취소 + 리뷰 포인트 적립/회수를 모두 관리한다.
 *
 * 리뷰 포인트:
 * - 텍스트 리뷰: 100P, 포토 리뷰: 300P, 사이즈정보: +50P (최대 350P)
 * - 일일 한도: 5,000P (KST 00:00 리셋)
 * - 리뷰 삭제 시 회수 (마이너스 잔액 허용)
 */
@Service
@Transactional(readOnly = true)
class PointService(
    private val pointBalanceRepository: PointBalanceRepository,
    private val pointHistoryRepository: PointHistoryRepository,
    private val memberRepository: MemberRepository? = null,
    private val redisTemplate: StringRedisTemplate? = null,
) {
    companion object {
        private const val DAILY_LIMIT = 5000
        private const val DAILY_KEY_PREFIX = "review:daily_point:"
        private val KST = ZoneId.of("Asia/Seoul")
    }

    // ========================================
    // 일반 적립금 관리 (PointBalance 기반)
    // ========================================

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

    // ========================================
    // 리뷰 포인트 (Redis 일일 한도 관리)
    // ========================================

    /**
     * 리뷰 포인트 적립.
     * 일일 한도 5,000P 체크 후 PointBalance에 적립한다.
     *
     * @return 실제 적립된 포인트 (일일 한도 초과 시 0)
     */
    @Transactional
    fun earnReviewPoint(
        memberId: Long,
        reviewId: Long,
        amount: Int,
    ): Int {
        requireNotNull(redisTemplate) { "Redis is required for review point operations" }

        val balance = getOrCreateBalance(memberId)

        // 일일 한도 체크
        val dailyKey = getDailyKey(memberId)
        val currentDaily = redisTemplate.opsForValue().get(dailyKey)?.toIntOrNull() ?: 0

        if (currentDaily >= DAILY_LIMIT) {
            logger.info { "리뷰 포인트 일일 한도 초과: memberId=$memberId, currentDaily=$currentDaily" }
            return 0
        }

        val actualAmount = minOf(amount, DAILY_LIMIT - currentDaily)

        val history = balance.earn(actualAmount)
        history.withReference(PointReferenceType.REVIEW, reviewId)

        pointBalanceRepository.save(balance)
        pointHistoryRepository.save(history)

        // Redis 일일 누적 업데이트
        redisTemplate.opsForValue().increment(dailyKey, actualAmount.toLong())
        val ttl = calculateTtlUntilMidnightKST()
        redisTemplate.expire(dailyKey, ttl)

        logger.info { "리뷰 포인트 적립 완료: memberId=$memberId, amount=$actualAmount, balance=${balance.availablePoints}" }
        return actualAmount
    }

    /**
     * 리뷰 포인트 회수.
     * 리뷰 삭제 시 적립 포인트를 회수한다.
     * 잔액 부족 시 마이너스 잔액 허용 (다음 적립에서 상계).
     */
    @Transactional
    fun revokeReviewPoint(
        memberId: Long,
        reviewId: Long,
        amount: Int,
    ) {
        val balance = getOrCreateBalance(memberId)

        // 마이너스 잔액 허용 (cancelEarn은 부족 시 예외 발생하므로 직접 처리)
        balance.totalPoints -= amount
        balance.availablePoints -= amount

        val history =
            com.closet.member.domain.point.PointHistory(
                memberId = memberId,
                amount = -amount,
                balanceAfter = balance.availablePoints,
                transactionType = PointTransactionType.CANCEL_EARN,
            ).withReference(PointReferenceType.REVIEW, reviewId)

        pointBalanceRepository.save(balance)
        pointHistoryRepository.save(history)

        logger.info { "리뷰 포인트 회수 완료: memberId=$memberId, amount=$amount, balance=${balance.availablePoints}" }
    }

    // ========================================
    // Private helpers
    // ========================================

    private fun getOrCreateBalance(memberId: Long): PointBalance {
        return pointBalanceRepository.findByMemberId(memberId)
            .orElseGet {
                pointBalanceRepository.save(PointBalance.create(memberId))
            }
    }

    private fun getDailyKey(memberId: Long): String {
        val today = LocalDate.now(KST)
        return "${DAILY_KEY_PREFIX}$memberId:$today"
    }

    private fun calculateTtlUntilMidnightKST(): Duration {
        val now = ZonedDateTime.now(KST)
        val midnight = now.toLocalDate().plusDays(1).atStartOfDay(KST)
        return Duration.between(now, midnight).let {
            if (it.isNegative || it.isZero) Duration.ofHours(24) else it
        }
    }
}
