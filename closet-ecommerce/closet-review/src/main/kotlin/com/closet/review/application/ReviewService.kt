package com.closet.review.application

import com.closet.common.event.ClosetTopics
import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.common.outbox.OutboxEventPublisher
import com.closet.review.domain.Review
import com.closet.review.domain.ReviewEditHistory
import com.closet.review.domain.ReviewEditHistoryRepository
import com.closet.review.domain.ReviewHelpful
import com.closet.review.domain.ReviewHelpfulRepository
import com.closet.review.domain.ReviewImage
import com.closet.review.domain.ReviewRepository
import com.closet.review.domain.ReviewSortType
import com.closet.review.domain.ReviewStatus
import com.closet.review.domain.ReviewableOrderItem
import com.closet.review.domain.ReviewableOrderItemRepository
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

private val logger = KotlinLogging.logger {}

/**
 * 리뷰 서비스 (US-801 ~ US-803).
 *
 * 리뷰 CRUD + 이미지 URL 관리 + Outbox 이벤트 발행 + "도움이 됐어요".
 * 이미지는 Presigned URL로 MinIO/S3에 업로드된 URL을 저장한다.
 */
@Service
@Transactional(readOnly = true)
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val reviewEditHistoryRepository: ReviewEditHistoryRepository,
    private val reviewHelpfulRepository: ReviewHelpfulRepository,
    private val reviewableOrderItemRepository: ReviewableOrderItemRepository,
    private val reviewSummaryService: ReviewSummaryService,
    private val outboxEventPublisher: OutboxEventPublisher,
    private val objectMapper: ObjectMapper,
) {
    /**
     * 주문 아이템 구매확정 이벤트 처리.
     * order-service에서 OrderItemConfirmed 이벤트를 수신하면 호출된다.
     * 구매확정된 주문 아이템을 기록하여 리뷰 작성 자격을 부여한다.
     */
    @Transactional
    fun onOrderItemConfirmed(
        orderItemId: Long,
        memberId: Long,
        productId: Long,
    ) {
        if (reviewableOrderItemRepository.existsByOrderItemIdAndMemberId(orderItemId, memberId)) {
            logger.info { "이미 구매확정 기록이 존재합니다: orderItemId=$orderItemId, memberId=$memberId" }
            return
        }

        reviewableOrderItemRepository.save(
            ReviewableOrderItem.create(
                orderItemId = orderItemId,
                memberId = memberId,
                productId = productId,
            ),
        )
        logger.info { "구매확정 기록 저장: orderItemId=$orderItemId, memberId=$memberId, productId=$productId" }
    }

    /**
     * 리뷰 작성 (US-801).
     * - 구매확정(CONFIRMED) 상태 주문만 리뷰 가능
     * - 동일 주문상품 1회만 작성
     * - 이미지 URL 최대 5장
     * - review.created Kafka 이벤트 발행
     */
    @Transactional
    fun createReview(
        memberId: Long,
        request: CreateReviewRequest,
    ): ReviewResponse {
        // 구매확정 여부 검증
        val isConfirmed =
            reviewableOrderItemRepository.existsByOrderItemIdAndMemberId(
                orderItemId = request.orderItemId,
                memberId = memberId,
            )
        if (!isConfirmed) {
            throw BusinessException(ErrorCode.INVALID_INPUT, "구매확정된 주문 아이템만 리뷰를 작성할 수 있습니다: orderItemId=${request.orderItemId}")
        }

        // 중복 리뷰 체크
        val exists =
            reviewRepository.existsByOrderItemIdAndMemberIdAndStatusNot(
                orderItemId = request.orderItemId,
                memberId = memberId,
                status = ReviewStatus.DELETED,
            )
        if (exists) {
            throw BusinessException(ErrorCode.DUPLICATE_ENTITY, "이미 리뷰를 작성했습니다: orderItemId=${request.orderItemId}")
        }

        // 이미지 URL 개수 검증
        if (request.imageUrls.size > Review.MAX_IMAGE_COUNT) {
            throw BusinessException(ErrorCode.INVALID_INPUT, "이미지는 최대 ${Review.MAX_IMAGE_COUNT}장까지 등록할 수 있습니다")
        }

        val review =
            Review.create(
                productId = request.productId,
                orderItemId = request.orderItemId,
                memberId = memberId,
                rating = request.rating,
                content = request.content,
                height = request.height,
                weight = request.weight,
                normalSize = request.normalSize,
                purchasedSize = request.purchasedSize,
                fitType = request.fitType,
            )

        // 이미지 URL -> ReviewImage 엔티티 변환
        request.imageUrls.forEachIndexed { index, url ->
            review.addImage(
                imageUrl = url,
                // 썸네일은 클라이언트/CDN에서 리사이즈 또는 별도 key 관리
                thumbnailUrl = url,
                displayOrder = index,
            )
        }

        val saved = reviewRepository.save(review)

        // 리뷰 집계 갱신 (US-804)
        reviewSummaryService.onReviewCreated(saved)

        // review.created 이벤트 발행 (US-803: 포인트 적립용)
        publishReviewCreatedEvent(saved)

        logger.info { "리뷰 작성 완료: id=${saved.id}, productId=${request.productId}, memberId=$memberId" }
        return ReviewResponse.from(saved)
    }

    /**
     * 리뷰 수정 (US-801).
     * - 별점 수정 불가
     * - 최대 3회, 7일 이내
     * - 이미지 교체 가능
     */
    @Transactional
    fun updateReview(
        memberId: Long,
        reviewId: Long,
        request: UpdateReviewRequest,
    ): ReviewResponse {
        val review = getReviewByMember(reviewId, memberId)

        val previousContent = review.content
        review.update(request.content)

        // 이미지 교체 (null이면 변경 없음)
        if (request.imageUrls != null) {
            if (request.imageUrls.size > Review.MAX_IMAGE_COUNT) {
                throw BusinessException(ErrorCode.INVALID_INPUT, "이미지는 최대 ${Review.MAX_IMAGE_COUNT}장까지 등록할 수 있습니다")
            }
            val newImages =
                request.imageUrls.mapIndexed { index, url ->
                    ReviewImage(
                        review = review,
                        imageUrl = url,
                        thumbnailUrl = url,
                        displayOrder = index,
                    )
                }
            review.replaceImages(newImages)
        }

        // 수정 이력 저장
        reviewEditHistoryRepository.save(
            ReviewEditHistory.create(
                reviewId = review.id,
                previousContent = previousContent,
                newContent = request.content,
                editCount = review.editCount,
            ),
        )

        // 집계 갱신 (포토 여부 변경 가능)
        reviewSummaryService.onReviewUpdated(review)

        logger.info { "리뷰 수정 완료: id=$reviewId, editCount=${review.editCount}" }
        return ReviewResponse.from(review)
    }

    /**
     * 리뷰 삭제 (본인만, US-801).
     * 삭제 시 포인트 회수 이벤트 발행 (US-803).
     */
    @Transactional
    fun deleteReview(
        memberId: Long,
        reviewId: Long,
    ) {
        val review = getReviewByMember(reviewId, memberId)
        review.delete()

        // 리뷰 집계 갱신 (US-804)
        reviewSummaryService.onReviewDeleted(review)

        // review.deleted 이벤트 발행 (포인트 회수용, US-803)
        publishReviewDeletedEvent(review)

        logger.info { "리뷰 삭제 완료: id=$reviewId, memberId=$memberId" }
    }

    /**
     * 관리자 블라인드.
     */
    @Transactional
    fun hideReview(reviewId: Long) {
        val review =
            reviewRepository.findById(reviewId)
                .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "리뷰를 찾을 수 없습니다: id=$reviewId") }
        review.hide()

        // 블라인드 시 집계에서 제거
        reviewSummaryService.onReviewDeleted(review)

        logger.info { "리뷰 블라인드 처리: id=$reviewId" }
    }

    /**
     * 관리자 블라인드 해제.
     */
    @Transactional
    fun unhideReview(reviewId: Long) {
        val review =
            reviewRepository.findById(reviewId)
                .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "리뷰를 찾을 수 없습니다: id=$reviewId") }
        review.unhide()

        // 블라인드 해제 시 집계에 다시 추가
        reviewSummaryService.onReviewCreated(review)

        logger.info { "리뷰 블라인드 해제: id=$reviewId" }
    }

    /**
     * 리뷰 상세 조회.
     */
    fun findById(reviewId: Long): ReviewResponse {
        val review =
            reviewRepository.findById(reviewId)
                .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "리뷰를 찾을 수 없습니다: id=$reviewId") }
        return ReviewResponse.from(review)
    }

    /**
     * 상품별 리뷰 목록 조회 (정렬 + 필터 지원).
     */
    fun findByProductId(query: ReviewListQuery): Page<ReviewResponse> {
        val pageable = PageRequest.of(query.page, query.size)
        val status = ReviewStatus.VISIBLE

        // "비슷한 체형" 필터 (US-802)
        if (query.myHeight != null && query.myWeight != null) {
            return reviewRepository.findBySimilarBody(
                productId = query.productId,
                status = status,
                minHeight = query.myHeight - SIMILAR_BODY_RANGE,
                maxHeight = query.myHeight + SIMILAR_BODY_RANGE,
                minWeight = query.myWeight - SIMILAR_BODY_RANGE,
                maxWeight = query.myWeight + SIMILAR_BODY_RANGE,
                pageable = pageable,
            ).map { ReviewResponse.from(it) }
        }

        // 포토리뷰 필터
        val page: Page<Review> =
            if (query.photoOnly) {
                when (query.sort) {
                    ReviewSortType.LATEST -> reviewRepository.findByProductIdPhotoOnlyLatest(query.productId, status, pageable)
                    ReviewSortType.RATING -> reviewRepository.findByProductIdPhotoOnlyRating(query.productId, status, pageable)
                    ReviewSortType.HELPFUL -> reviewRepository.findByProductIdPhotoOnlyHelpful(query.productId, status, pageable)
                }
            } else {
                when (query.sort) {
                    ReviewSortType.LATEST -> reviewRepository.findByProductIdLatest(query.productId, status, pageable)
                    ReviewSortType.RATING -> reviewRepository.findByProductIdRating(query.productId, status, pageable)
                    ReviewSortType.HELPFUL -> reviewRepository.findByProductIdHelpful(query.productId, status, pageable)
                }
            }

        return page.map { ReviewResponse.from(it) }
    }

    /**
     * 회원별 리뷰 목록 조회.
     */
    fun findByMemberId(
        memberId: Long,
        pageable: Pageable,
    ): Page<ReviewResponse> {
        return reviewRepository.findByMemberIdAndStatusNotOrderByCreatedAtDesc(
            memberId = memberId,
            status = ReviewStatus.DELETED,
            pageable = pageable,
        ).map { ReviewResponse.from(it) }
    }

    /**
     * 리뷰 수정 이력 조회.
     */
    fun getEditHistory(reviewId: Long): List<ReviewEditHistoryResponse> {
        return reviewEditHistoryRepository.findByReviewIdOrderByCreatedAtDesc(reviewId)
            .map { ReviewEditHistoryResponse.from(it) }
    }

    /**
     * "도움이 됐어요" (POST /api/v1/reviews/{id}/helpful).
     * 회원당 리뷰 1건당 1회만 가능.
     */
    @Transactional
    fun markHelpful(
        memberId: Long,
        reviewId: Long,
    ) {
        val review =
            reviewRepository.findById(reviewId)
                .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "리뷰를 찾을 수 없습니다: id=$reviewId") }

        if (review.status != ReviewStatus.VISIBLE) {
            throw BusinessException(ErrorCode.INVALID_INPUT, "노출 중인 리뷰에만 '도움이 됐어요'를 할 수 있습니다")
        }

        val alreadyMarked = reviewHelpfulRepository.existsByReviewIdAndMemberId(reviewId, memberId)
        if (alreadyMarked) {
            throw BusinessException(ErrorCode.DUPLICATE_ENTITY, "이미 '도움이 됐어요'를 누른 리뷰입니다")
        }

        reviewHelpfulRepository.save(ReviewHelpful.create(reviewId, memberId))
        review.incrementHelpfulCount()

        logger.info { "리뷰 도움이 됐어요: reviewId=$reviewId, memberId=$memberId, helpfulCount=${review.helpfulCount}" }
    }

    private fun getReviewByMember(
        reviewId: Long,
        memberId: Long,
    ): Review {
        return reviewRepository.findByIdAndMemberId(reviewId, memberId)
            ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "리뷰를 찾을 수 없습니다: id=$reviewId, memberId=$memberId")
    }

    /**
     * review.created 이벤트 발행 (US-803).
     * 포인트 계산: 텍스트 100P, 포토 300P, 사이즈정보 +50P (최대 350P)
     */
    private fun publishReviewCreatedEvent(review: Review) {
        val pointAmount = review.calculatePointAmount()
        val payload =
            objectMapper.writeValueAsString(
                mapOf(
                    "eventId" to "review-created-${review.id}-${System.currentTimeMillis()}",
                    "eventType" to "ReviewCreated",
                    "reviewId" to review.id,
                    "productId" to review.productId,
                    "memberId" to review.memberId,
                    "rating" to review.rating,
                    "isPhotoReview" to review.isPhotoReview(),
                    "hasSizeInfo" to review.hasSizeInfo(),
                    "pointAmount" to pointAmount,
                    "timestamp" to ZonedDateTime.now().toString(),
                ),
            )

        outboxEventPublisher.publish(
            aggregateType = "Review",
            aggregateId = review.id.toString(),
            eventType = "ReviewCreated",
            topic = ClosetTopics.REVIEW,
            partitionKey = review.memberId.toString(),
            payload = payload,
        )

        logger.info { "review.created 이벤트 발행: reviewId=${review.id}, memberId=${review.memberId}, pointAmount=$pointAmount" }
    }

    /**
     * review.deleted 이벤트 발행 (US-803: 포인트 회수).
     */
    private fun publishReviewDeletedEvent(review: Review) {
        val pointAmount = review.calculatePointAmount()
        val payload =
            objectMapper.writeValueAsString(
                mapOf(
                    "eventId" to "review-deleted-${review.id}-${System.currentTimeMillis()}",
                    "eventType" to "ReviewDeleted",
                    "reviewId" to review.id,
                    "productId" to review.productId,
                    "memberId" to review.memberId,
                    "pointAmount" to pointAmount,
                    "timestamp" to ZonedDateTime.now().toString(),
                ),
            )

        outboxEventPublisher.publish(
            aggregateType = "Review",
            aggregateId = review.id.toString(),
            eventType = "ReviewDeleted",
            topic = ClosetTopics.REVIEW,
            partitionKey = review.memberId.toString(),
            payload = payload,
        )

        logger.info { "review.deleted 이벤트 발행: reviewId=${review.id}, memberId=${review.memberId}, pointAmount=$pointAmount" }
    }

    companion object {
        /** "나와 비슷한 체형" 필터의 키/몸무게 허용 범위 (US-802) */
        private const val SIMILAR_BODY_RANGE = 5
    }
}
