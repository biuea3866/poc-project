package com.closet.review.domain

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

/**
 * 리뷰 엔티티 (US-801 ~ US-803).
 *
 * - 별점 1-5
 * - 텍스트 20-1000자
 * - 이미지 최대 5장 (Presigned URL)
 * - 수정 최대 3회, 작성 후 7일 이내
 * - 별점 수정 불가
 * - 상태: VISIBLE / HIDDEN / DELETED
 * - 사이즈 후기: 키, 몸무게, 평소 사이즈, 구매 사이즈, 핏 평가
 */
@Entity
@Table(name = "review")
@EntityListeners(AuditingEntityListener::class)
class Review(
    @Column(name = "product_id", nullable = false)
    val productId: Long,
    @Column(name = "order_item_id", nullable = false)
    val orderItemId: Long,
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Column(name = "rating", nullable = false)
    val rating: Int,
    @Column(name = "content", nullable = false, length = 2000)
    var content: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    var status: ReviewStatus = ReviewStatus.VISIBLE,
    @Column(name = "edit_count", nullable = false)
    var editCount: Int = 0,
    @Column(name = "has_image", nullable = false, columnDefinition = "TINYINT(1)")
    var hasImage: Boolean = false,
    // 사이즈 후기 (US-802)
    @Column(name = "height")
    var height: Int? = null,
    @Column(name = "weight")
    var weight: Int? = null,
    @Column(name = "normal_size", length = 20)
    var normalSize: String? = null,
    @Column(name = "purchased_size", length = 20)
    var purchasedSize: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "fit_type", length = 20, columnDefinition = "VARCHAR(20)")
    var fitType: SizeFit? = null,
    @Column(name = "helpful_count", nullable = false)
    var helpfulCount: Int = 0,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @OneToMany(mappedBy = "review", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    val images: MutableList<ReviewImage> = mutableListOf()

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: ZonedDateTime

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    lateinit var updatedAt: ZonedDateTime

    @Column(name = "deleted_at", columnDefinition = "DATETIME(6)")
    var deletedAt: ZonedDateTime? = null

    init {
        require(rating in 1..5) { "별점은 1~5 사이여야 합니다: $rating" }
        require(content.length in MIN_CONTENT_LENGTH..MAX_CONTENT_LENGTH) {
            "리뷰 내용은 ${MIN_CONTENT_LENGTH}~${MAX_CONTENT_LENGTH}자 사이여야 합니다"
        }
    }

    /**
     * 리뷰 수정 (US-801).
     * - 별점 수정 불가
     * - 최대 3회 수정
     * - 작성 후 7일 이내
     */
    fun update(
        newContent: String,
        now: ZonedDateTime = ZonedDateTime.now(),
    ) {
        if (status == ReviewStatus.DELETED) {
            throw BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "삭제된 리뷰는 수정할 수 없습니다")
        }
        if (editCount >= MAX_EDIT_COUNT) {
            throw BusinessException(ErrorCode.INVALID_INPUT, "수정 횟수 제한(${MAX_EDIT_COUNT}회)을 초과했습니다")
        }
        if (now.isAfter(createdAt.plusDays(EDIT_DEADLINE_DAYS))) {
            throw BusinessException(ErrorCode.INVALID_INPUT, "수정 가능 기한(${EDIT_DEADLINE_DAYS}일)이 지났습니다")
        }
        require(newContent.length in MIN_CONTENT_LENGTH..MAX_CONTENT_LENGTH) {
            "리뷰 내용은 ${MIN_CONTENT_LENGTH}~${MAX_CONTENT_LENGTH}자 사이여야 합니다"
        }

        this.content = newContent
        this.editCount++
    }

    /**
     * 이미지 추가.
     */
    fun addImage(
        imageUrl: String,
        thumbnailUrl: String,
        displayOrder: Int,
    ) {
        if (images.size >= MAX_IMAGE_COUNT) {
            throw BusinessException(ErrorCode.INVALID_INPUT, "이미지는 최대 ${MAX_IMAGE_COUNT}장까지 등록할 수 있습니다")
        }
        val image =
            ReviewImage(
                review = this,
                imageUrl = imageUrl,
                thumbnailUrl = thumbnailUrl,
                displayOrder = displayOrder,
            )
        images.add(image)
        hasImage = true
    }

    /**
     * 이미지 전체 교체 (수정 시).
     */
    fun replaceImages(newImages: List<ReviewImage>) {
        if (newImages.size > MAX_IMAGE_COUNT) {
            throw BusinessException(ErrorCode.INVALID_INPUT, "이미지는 최대 ${MAX_IMAGE_COUNT}장까지 등록할 수 있습니다")
        }
        images.clear()
        images.addAll(newImages)
        hasImage = images.isNotEmpty()
    }

    /**
     * 리뷰 삭제 (소프트 삭제).
     */
    fun delete() {
        status.validateTransitionTo(ReviewStatus.DELETED)
        status = ReviewStatus.DELETED
        deletedAt = ZonedDateTime.now()
    }

    /**
     * 관리자 블라인드.
     */
    fun hide() {
        status.validateTransitionTo(ReviewStatus.HIDDEN)
        status = ReviewStatus.HIDDEN
    }

    /**
     * 관리자 블라인드 해제.
     */
    fun unhide() {
        status.validateTransitionTo(ReviewStatus.VISIBLE)
        status = ReviewStatus.VISIBLE
    }

    /**
     * 포토 리뷰 여부.
     */
    fun isPhotoReview(): Boolean = hasImage && images.isNotEmpty()

    /**
     * 사이즈 정보 포함 여부 (US-802, US-803).
     * 키, 몸무게, 핏 타입 중 하나라도 입력했으면 사이즈 정보 포함으로 간주.
     */
    fun hasSizeInfo(): Boolean = height != null && weight != null && fitType != null

    /**
     * "도움이 됐어요" 카운트 증가.
     */
    fun incrementHelpfulCount() {
        helpfulCount++
    }

    /**
     * 포인트 계산 (US-803).
     * 텍스트 100P, 포토 300P, 사이즈정보 +50P
     * 최대 조합: 350P (300 + 50)
     */
    fun calculatePointAmount(): Int {
        var amount = TEXT_REVIEW_POINT
        if (isPhotoReview()) {
            amount = PHOTO_REVIEW_POINT
        }
        if (hasSizeInfo()) {
            amount += SIZE_INFO_BONUS_POINT
        }
        return amount
    }

    companion object {
        const val MAX_EDIT_COUNT = 3
        const val MAX_IMAGE_COUNT = 5
        const val EDIT_DEADLINE_DAYS = 7L
        const val MIN_CONTENT_LENGTH = 20
        const val MAX_CONTENT_LENGTH = 1000
        const val TEXT_REVIEW_POINT = 100
        const val PHOTO_REVIEW_POINT = 300
        const val SIZE_INFO_BONUS_POINT = 50

        fun create(
            productId: Long,
            orderItemId: Long,
            memberId: Long,
            rating: Int,
            content: String,
            height: Int? = null,
            weight: Int? = null,
            normalSize: String? = null,
            purchasedSize: String? = null,
            fitType: SizeFit? = null,
        ): Review {
            return Review(
                productId = productId,
                orderItemId = orderItemId,
                memberId = memberId,
                rating = rating,
                content = content,
                height = height,
                weight = weight,
                normalSize = normalSize,
                purchasedSize = purchasedSize,
                fitType = fitType,
            )
        }
    }
}
