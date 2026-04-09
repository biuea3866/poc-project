package com.closet.promotion.repository

import com.closet.promotion.domain.discount.ConditionType
import com.closet.promotion.domain.discount.DiscountPolicy
import com.closet.promotion.domain.discount.QDiscountPolicy.discountPolicy
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import java.math.BigDecimal
import java.time.ZonedDateTime

class DiscountPolicyCustomRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : DiscountPolicyCustomRepository {
    override fun findActiveByConditions(
        categoryId: Long?,
        brandId: Long?,
        orderAmount: BigDecimal?,
    ): List<DiscountPolicy> {
        val now = ZonedDateTime.now()
        val conditionFilter = BooleanBuilder()

        // ALL 조건은 항상 포함
        conditionFilter.or(discountPolicy.conditionType.eq(ConditionType.ALL))

        // 카테고리 조건
        if (categoryId != null) {
            conditionFilter.or(
                discountPolicy.conditionType.eq(ConditionType.CATEGORY)
                    .and(discountPolicy.conditionValue.eq(categoryId.toString())),
            )
        }

        // 브랜드 조건
        if (brandId != null) {
            conditionFilter.or(
                discountPolicy.conditionType.eq(ConditionType.BRAND)
                    .and(discountPolicy.conditionValue.eq(brandId.toString())),
            )
        }

        // 금액 범위 조건은 서비스 레이어에서 추가 필터링
        conditionFilter.or(discountPolicy.conditionType.eq(ConditionType.AMOUNT_RANGE))

        return queryFactory.selectFrom(discountPolicy)
            .where(
                discountPolicy.isActive.isTrue,
                discountPolicy.startedAt.loe(now),
                discountPolicy.endedAt.goe(now),
                discountPolicy.deletedAt.isNull,
                conditionFilter,
            )
            .orderBy(discountPolicy.priority.asc())
            .fetch()
    }
}
