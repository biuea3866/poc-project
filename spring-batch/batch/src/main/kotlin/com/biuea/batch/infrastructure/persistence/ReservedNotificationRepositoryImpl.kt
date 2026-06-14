package com.biuea.batch.infrastructure.persistence

import com.biuea.batch.domain.ReservedNotificationRepository
import com.biuea.batch.domain.UserIdRange
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * 도메인 ReservedNotificationRepository 의 QueryDSL 구현.
 * 배치 Reader/Writer 의 grouped paging·bulk update 와 분리된, application(UseCase)용 고수준 연산만 담당한다.
 */
@Repository
class ReservedNotificationRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : ReservedNotificationRepository {

    private val notification = QReservedNotificationJpaEntity.reservedNotificationJpaEntity

    @Transactional
    override fun resetSentFlags(): Int =
        queryFactory.update(notification)
            .set(notification.sent, false)
            .where(notification.sent.isTrue)
            .execute()
            .toInt()

    @Transactional(readOnly = true)
    override fun countTotal(): Long =
        queryFactory.select(notification.count())
            .from(notification)
            .fetchOne() ?: 0L

    @Transactional(readOnly = true)
    override fun countSent(): Long =
        queryFactory.select(notification.count())
            .from(notification)
            .where(notification.sent.isTrue)
            .fetchOne() ?: 0L

    @Transactional(readOnly = true)
    override fun userIdRange(): UserIdRange? {
        val tuple = queryFactory.select(notification.userId.min(), notification.userId.max())
            .from(notification)
            .fetchOne()
        val min = tuple?.get(notification.userId.min())
        val max = tuple?.get(notification.userId.max())
        if (min == null || max == null) return null
        return UserIdRange(min = min, max = max)
    }
}
