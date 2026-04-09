package com.closet.member.domain.repository

import com.closet.member.domain.point.PointEventType
import com.closet.member.domain.point.PointPolicy
import com.closet.member.domain.point.QPointPolicy.pointPolicy
import com.querydsl.jpa.impl.JPAQueryFactory

class PointPolicyCustomRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : PointPolicyCustomRepository {
    override fun findActiveByEventType(eventType: PointEventType): List<PointPolicy> {
        return queryFactory.selectFrom(pointPolicy)
            .where(
                pointPolicy.isActive.isTrue,
                pointPolicy.eventType.eq(eventType),
            )
            .fetch()
    }
}
