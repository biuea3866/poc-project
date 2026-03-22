package com.closet.review.domain.entity

import com.closet.common.entity.BaseEntity
import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.review.domain.enums.ReviewStatus
import com.closet.review.domain.enums.SizeFeeling
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "review")
class Review(

    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(name = "order_item_id", nullable = false, unique = true)
    val orderItemId: Long,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "rating", nullable = false)
    var rating: Int,

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(name = "height")
    val height: Int? = null,

    @Column(name = "weight")
    val weight: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "size_feeling", length = 20, columnDefinition = "VARCHAR(20)")
    val sizeFeeling: SizeFeeling? = null,

    @Column(name = "helpful_count", nullable = false)
    var helpfulCount: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    var status: ReviewStatus = ReviewStatus.ACTIVE,

    @OneToMany(mappedBy = "review", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val images: MutableList<ReviewImage> = mutableListOf()

) : BaseEntity() {

    init {
        validateRating(rating)
    }

    fun update(content: String, rating: Int) {
        validateRating(rating)
        if (status == ReviewStatus.DELETED) {
            throw BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "삭제된 리뷰는 수정할 수 없습니다")
        }
        this.content = content
        this.rating = rating
    }

    fun hide() {
        status.validateTransitionTo(ReviewStatus.HIDDEN)
        status = ReviewStatus.HIDDEN
    }

    fun delete() {
        status.validateTransitionTo(ReviewStatus.DELETED)
        status = ReviewStatus.DELETED
        softDelete()
    }

    fun markHelpful() {
        if (status != ReviewStatus.ACTIVE) {
            throw BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "활성 상태의 리뷰만 도움돼요를 누를 수 있습니다")
        }
        helpfulCount++
    }

    fun addImage(image: ReviewImage) {
        image.review = this
        images.add(image)
    }

    fun validateOwner(memberId: Long) {
        if (this.memberId != memberId) {
            throw BusinessException(ErrorCode.FORBIDDEN, "본인의 리뷰만 수정/삭제할 수 있습니다")
        }
    }

    companion object {
        fun create(
            productId: Long,
            orderItemId: Long,
            memberId: Long,
            rating: Int,
            content: String,
            height: Int? = null,
            weight: Int? = null,
            sizeFeeling: SizeFeeling? = null,
            imageUrls: List<String> = emptyList()
        ): Review {
            val review = Review(
                productId = productId,
                orderItemId = orderItemId,
                memberId = memberId,
                rating = rating,
                content = content,
                height = height,
                weight = weight,
                sizeFeeling = sizeFeeling
            )
            imageUrls.forEachIndexed { index, url ->
                review.addImage(ReviewImage(imageUrl = url, sortOrder = index))
            }
            return review
        }

        private fun validateRating(rating: Int) {
            require(rating in 1..5) { "별점은 1~5 범위여야 합니다: $rating" }
        }
    }
}
