package com.closet.member.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.member.domain.Member
import com.closet.member.domain.PointHistory
import com.closet.member.domain.PointType
import com.closet.member.domain.repository.MemberRepository
import com.closet.member.domain.repository.PointHistoryRepository
import mu.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

private val logger = KotlinLogging.logger {}

/**
 * 포인트 적립/회수 서비스 (CP-27).
 *
 * 리뷰 포인트 적립 (US-803):
 * - 텍스트 리뷰: 100P, 포토 리뷰: 300P, 사이즈정보: +50P (최대 350P)
 * - 일일 한도: 5,000P (KST 00:00 리셋)
 * - 리뷰 삭제 시 회수 (마이너스 잔액 허용)
 */
@Service
@Transactional
class PointService(
    private val memberRepository: MemberRepository,
    private val pointHistoryRepository: PointHistoryRepository,
    private val redisTemplate: StringRedisTemplate,
) {
    companion object {
        private const val DAILY_LIMIT = 5000
        private const val DAILY_KEY_PREFIX = "review:daily_point:"
        private val KST = ZoneId.of("Asia/Seoul")
    }

    /**
     * 리뷰 포인트 적립.
     * 일일 한도 5,000P 체크 후 적립한다.
     *
     * @return 실제 적립된 포인트 (일일 한도 초과 시 0)
     */
    fun earnReviewPoint(
        memberId: Long,
        reviewId: Long,
        amount: Int,
    ): Int {
        val member = getMemberOrThrow(memberId)

        // 일일 한도 체크 (PD-37)
        val dailyKey = getDailyKey(memberId)
        val currentDaily = redisTemplate.opsForValue().get(dailyKey)?.toIntOrNull() ?: 0

        if (currentDaily >= DAILY_LIMIT) {
            logger.info { "리뷰 포인트 일일 한도 초과: memberId=$memberId, currentDaily=$currentDaily" }
            return 0
        }

        val actualAmount = minOf(amount, DAILY_LIMIT - currentDaily)

        member.earnPoints(actualAmount)

        pointHistoryRepository.save(
            PointHistory.earn(
                memberId = memberId,
                amount = actualAmount,
                balanceAfter = member.pointBalance,
                reason = "리뷰 작성 포인트 적립",
                referenceId = "review:$reviewId",
            ),
        )

        // Redis 일일 누적 업데이트
        redisTemplate.opsForValue().increment(dailyKey, actualAmount.toLong())
        // TTL: KST 자정까지
        val ttl = calculateTtlUntilMidnightKST()
        redisTemplate.expire(dailyKey, ttl)

        logger.info { "리뷰 포인트 적립 완료: memberId=$memberId, amount=$actualAmount, balance=${member.pointBalance}" }
        return actualAmount
    }

    /**
     * 리뷰 포인트 회수 (PD-36).
     * 리뷰 삭제 시 적립 포인트를 회수한다.
     * 잔액 부족 시 마이너스 잔액 허용 (다음 적립에서 상계).
     */
    fun revokeReviewPoint(
        memberId: Long,
        reviewId: Long,
        amount: Int,
    ) {
        val member = getMemberOrThrow(memberId)

        // 마이너스 잔액 허용 (PD-36)
        member.pointBalance -= amount

        pointHistoryRepository.save(
            PointHistory(
                memberId = memberId,
                type = PointType.CANCEL,
                amount = -amount,
                balanceAfter = member.pointBalance,
                reason = "리뷰 삭제 포인트 회수",
                referenceId = "review:$reviewId",
            ),
        )

        logger.info { "리뷰 포인트 회수 완료: memberId=$memberId, amount=$amount, balance=${member.pointBalance}" }
    }

    private fun getMemberOrThrow(memberId: Long): Member {
        return memberRepository.findByIdAndDeletedAtIsNull(memberId)
            ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "회원을 찾을 수 없습니다: id=$memberId")
    }

    private fun getDailyKey(memberId: Long): String {
        val today = LocalDate.now(KST)
        return "${DAILY_KEY_PREFIX}$memberId:$today"
    }

    private fun calculateTtlUntilMidnightKST(): Duration {
        val now = java.time.ZonedDateTime.now(KST)
        val midnight = now.toLocalDate().plusDays(1).atStartOfDay(KST)
        return Duration.between(now, midnight).let {
            if (it.isNegative || it.isZero) Duration.ofHours(24) else it
        }
    }
}
