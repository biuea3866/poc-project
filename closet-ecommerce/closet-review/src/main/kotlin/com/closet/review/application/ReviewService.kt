package com.closet.review.application

import com.closet.common.event.ClosetTopics
import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.common.outbox.OutboxEventPublisher
import com.closet.review.domain.Review
import com.closet.review.domain.ReviewEditHistory
import com.closet.review.domain.ReviewEditHistoryRepository
import com.closet.review.domain.ReviewImage
import com.closet.review.domain.ReviewRepository
import com.closet.review.domain.ReviewStatus
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import net.coobird.thumbnailator.Thumbnails
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.time.ZonedDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * 리뷰 서비스.
 *
 * CRUD + 이미지 Thumbnailator 리사이즈(PD-33) + Outbox 이벤트 발행.
 */
@Service
@Transactional(readOnly = true)
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val reviewEditHistoryRepository: ReviewEditHistoryRepository,
    private val reviewSummaryService: ReviewSummaryService,
    private val outboxEventPublisher: OutboxEventPublisher,
    private val objectMapper: ObjectMapper,
    @Value("\${review.image.upload-dir:./uploads/reviews}")
    private val uploadDir: String,
) {

    companion object {
        private const val THUMBNAIL_SIZE = 400
        private const val MAX_IMAGE_SIZE = 5 * 1024 * 1024L // 5MB (PD-34)
    }

    /**
     * 리뷰 작성 (CP-24).
     * 주문 아이템당 1개만 작성 가능.
     */
    @Transactional
    fun createReview(
        memberId: Long,
        request: CreateReviewRequest,
        images: List<MultipartFile>? = null,
    ): ReviewResponse {
        // 중복 리뷰 체크
        val exists = reviewRepository.existsByOrderItemIdAndMemberIdAndStatusNot(
            orderItemId = request.orderItemId,
            memberId = memberId,
            status = ReviewStatus.DELETED,
        )
        if (exists) {
            throw BusinessException(ErrorCode.DUPLICATE_ENTITY, "이미 리뷰를 작성했습니다: orderItemId=${request.orderItemId}")
        }

        val review = Review.create(
            productId = request.productId,
            orderItemId = request.orderItemId,
            memberId = memberId,
            rating = request.rating,
            content = request.content,
            height = request.height,
            weight = request.weight,
            purchasedSize = request.purchasedSize,
            fitType = request.fitType,
        )

        // 이미지 처리
        if (!images.isNullOrEmpty()) {
            processAndAddImages(review, images)
        }

        val saved = reviewRepository.save(review)

        // 리뷰 집계 갱신
        reviewSummaryService.onReviewCreated(saved)

        // review.created 이벤트 발행 (포인트 적립용)
        publishReviewCreatedEvent(saved)

        logger.info { "리뷰 작성 완료: id=${saved.id}, productId=${request.productId}, memberId=$memberId, rating=${request.rating}" }
        return ReviewResponse.from(saved)
    }

    /**
     * 리뷰 수정 (CP-26, PD-32).
     * - 별점 수정 불가
     * - 최대 3회, 7일 이내
     */
    @Transactional
    fun updateReview(
        memberId: Long,
        reviewId: Long,
        request: UpdateReviewRequest,
        images: List<MultipartFile>? = null,
    ): ReviewResponse {
        val review = getReviewByMember(reviewId, memberId)

        val previousContent = review.content
        review.update(request.content)

        // 이미지 교체
        if (images != null) {
            val newImages = mutableListOf<ReviewImage>()
            for ((index, file) in images.withIndex()) {
                val (imageUrl, thumbnailUrl) = saveImageFile(file)
                newImages.add(ReviewImage(
                    review = review,
                    imageUrl = imageUrl,
                    thumbnailUrl = thumbnailUrl,
                    displayOrder = index,
                ))
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
            )
        )

        logger.info { "리뷰 수정 완료: id=$reviewId, editCount=${review.editCount}" }
        return ReviewResponse.from(review)
    }

    /**
     * 리뷰 삭제 (BUYER).
     */
    @Transactional
    fun deleteReview(memberId: Long, reviewId: Long) {
        val review = getReviewByMember(reviewId, memberId)
        review.delete()

        // 리뷰 집계 갱신
        reviewSummaryService.onReviewDeleted(review)

        // review.deleted 이벤트 발행 (포인트 회수용)
        publishReviewDeletedEvent(review)

        logger.info { "리뷰 삭제 완료: id=$reviewId, memberId=$memberId" }
    }

    /**
     * 관리자 블라인드 (CP-26, PD-35).
     */
    @Transactional
    fun hideReview(reviewId: Long) {
        val review = reviewRepository.findById(reviewId)
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "리뷰를 찾을 수 없습니다: id=$reviewId") }
        review.hide()

        // 블라인드 시 집계에서 제거
        reviewSummaryService.onReviewDeleted(review)

        logger.info { "리뷰 블라인드 처리: id=$reviewId" }
    }

    /**
     * 관리자 블라인드 해제 (CP-26).
     */
    @Transactional
    fun unhideReview(reviewId: Long) {
        val review = reviewRepository.findById(reviewId)
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
        val review = reviewRepository.findById(reviewId)
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "리뷰를 찾을 수 없습니다: id=$reviewId") }
        return ReviewResponse.from(review)
    }

    /**
     * 상품별 리뷰 목록 조회.
     */
    fun findByProductId(productId: Long, pageable: Pageable): Page<ReviewResponse> {
        return reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(
            productId = productId,
            status = ReviewStatus.VISIBLE,
            pageable = pageable,
        ).map { ReviewResponse.from(it) }
    }

    /**
     * 회원별 리뷰 목록 조회.
     */
    fun findByMemberId(memberId: Long, pageable: Pageable): Page<ReviewResponse> {
        return reviewRepository.findByMemberIdAndStatusNotOrderByCreatedAtDesc(
            memberId = memberId,
            status = ReviewStatus.DELETED,
            pageable = pageable,
        ).map { ReviewResponse.from(it) }
    }

    /**
     * 리뷰 수정 이력 조회 (CP-26).
     */
    fun getEditHistory(reviewId: Long): List<ReviewEditHistoryResponse> {
        return reviewEditHistoryRepository.findByReviewIdOrderByCreatedAtDesc(reviewId)
            .map { ReviewEditHistoryResponse.from(it) }
    }

    private fun getReviewByMember(reviewId: Long, memberId: Long): Review {
        return reviewRepository.findByIdAndMemberId(reviewId, memberId)
            ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "리뷰를 찾을 수 없습니다: id=$reviewId, memberId=$memberId")
    }

    private fun processAndAddImages(review: Review, files: List<MultipartFile>) {
        if (files.size > Review.MAX_IMAGE_COUNT) {
            throw BusinessException(ErrorCode.INVALID_INPUT, "이미지는 최대 ${Review.MAX_IMAGE_COUNT}장까지 등록할 수 있습니다")
        }

        for ((index, file) in files.withIndex()) {
            if (file.size > MAX_IMAGE_SIZE) {
                throw BusinessException(ErrorCode.INVALID_INPUT, "이미지 크기는 5MB 이하여야 합니다")
            }

            val (imageUrl, thumbnailUrl) = saveImageFile(file)
            review.addImage(imageUrl, thumbnailUrl, index)
        }
    }

    /**
     * 이미지 로컬 저장 + Thumbnailator 리사이즈 (PD-33).
     * 원본 + 썸네일(400x400) 모두 저장.
     */
    private fun saveImageFile(file: MultipartFile): Pair<String, String> {
        val dir = File(uploadDir)
        if (!dir.exists()) dir.mkdirs()

        val ext = file.originalFilename?.substringAfterLast('.', "jpg") ?: "jpg"
        val fileName = "${UUID.randomUUID()}.$ext"
        val thumbnailFileName = "${UUID.randomUUID()}_thumb.$ext"

        val originalFile = File(dir, fileName)
        file.transferTo(originalFile)

        // Thumbnailator로 400x400 리사이즈
        val thumbnailFile = File(dir, thumbnailFileName)
        Thumbnails.of(originalFile)
            .size(THUMBNAIL_SIZE, THUMBNAIL_SIZE)
            .keepAspectRatio(true)
            .toFile(thumbnailFile)

        val imageUrl = "/uploads/reviews/$fileName"
        val thumbnailUrl = "/uploads/reviews/$thumbnailFileName"

        logger.debug { "이미지 저장 완료: original=$imageUrl, thumbnail=$thumbnailUrl" }
        return imageUrl to thumbnailUrl
    }

    private fun publishReviewCreatedEvent(review: Review) {
        val pointAmount = if (review.isPhotoReview()) 500 else 200
        val payload = objectMapper.writeValueAsString(
            mapOf(
                "eventId" to "review-created-${review.id}-${System.currentTimeMillis()}",
                "reviewId" to review.id,
                "productId" to review.productId,
                "memberId" to review.memberId,
                "rating" to review.rating,
                "isPhotoReview" to review.isPhotoReview(),
                "pointAmount" to pointAmount,
                "timestamp" to ZonedDateTime.now().toString(),
            )
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

    private fun publishReviewDeletedEvent(review: Review) {
        val pointAmount = if (review.isPhotoReview()) 500 else 200
        val payload = objectMapper.writeValueAsString(
            mapOf(
                "eventId" to "review-deleted-${review.id}-${System.currentTimeMillis()}",
                "reviewId" to review.id,
                "productId" to review.productId,
                "memberId" to review.memberId,
                "pointAmount" to pointAmount,
                "timestamp" to ZonedDateTime.now().toString(),
            )
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
}
