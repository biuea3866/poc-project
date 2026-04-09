package com.closet.review.domain

import com.closet.review.domain.QReview.review
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class ReviewCustomRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : ReviewCustomRepository {
    override fun findByProductIdLatest(
        productId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<Review> = queryWithSort(productId, status, false, pageable, review.createdAt.desc())

    override fun findByProductIdRating(
        productId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<Review> = queryWithSort(productId, status, false, pageable, review.rating.desc(), review.createdAt.desc())

    override fun findByProductIdHelpful(
        productId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<Review> = queryWithSort(productId, status, false, pageable, review.helpfulCount.desc(), review.createdAt.desc())

    override fun findByProductIdPhotoOnlyLatest(
        productId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<Review> = queryWithSort(productId, status, true, pageable, review.createdAt.desc())

    override fun findByProductIdPhotoOnlyRating(
        productId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<Review> = queryWithSort(productId, status, true, pageable, review.rating.desc(), review.createdAt.desc())

    override fun findByProductIdPhotoOnlyHelpful(
        productId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<Review> = queryWithSort(productId, status, true, pageable, review.helpfulCount.desc(), review.createdAt.desc())

    override fun findBySimilarBody(
        productId: Long,
        status: ReviewStatus,
        minHeight: Int,
        maxHeight: Int,
        minWeight: Int,
        maxWeight: Int,
        pageable: Pageable,
    ): Page<Review> {
        val query =
            queryFactory.selectFrom(review)
                .where(
                    review.productId.eq(productId),
                    review.status.eq(status),
                    review.height.isNotNull,
                    review.weight.isNotNull,
                    review.height.between(minHeight, maxHeight),
                    review.weight.between(minWeight, maxWeight),
                )
                .orderBy(review.createdAt.desc())

        val total = query.fetchCount()
        val content =
            query
                .offset(pageable.offset)
                .limit(pageable.pageSize.toLong())
                .fetch()

        return PageImpl(content, pageable, total)
    }

    private fun queryWithSort(
        productId: Long,
        status: ReviewStatus,
        photoOnly: Boolean,
        pageable: Pageable,
        vararg orderSpecifiers: OrderSpecifier<*>,
    ): Page<Review> {
        val baseQuery =
            queryFactory.selectFrom(review)
                .where(
                    review.productId.eq(productId),
                    review.status.eq(status),
                    if (photoOnly) review.hasImage.isTrue else null,
                )

        val total = baseQuery.fetchCount()
        val content =
            queryFactory.selectFrom(review)
                .where(
                    review.productId.eq(productId),
                    review.status.eq(status),
                    if (photoOnly) review.hasImage.isTrue else null,
                )
                .orderBy(*orderSpecifiers)
                .offset(pageable.offset)
                .limit(pageable.pageSize.toLong())
                .fetch()

        return PageImpl(content, pageable, total)
    }
}
