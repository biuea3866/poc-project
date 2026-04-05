package com.closet.review.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ReviewRepository : JpaRepository<Review, Long> {

    fun findByProductIdAndStatusOrderByCreatedAtDesc(productId: Long, status: ReviewStatus, pageable: Pageable): Page<Review>

    fun findByProductIdAndStatusInOrderByCreatedAtDesc(productId: Long, statuses: Collection<ReviewStatus>, pageable: Pageable): Page<Review>

    fun findByMemberIdAndStatusNotOrderByCreatedAtDesc(memberId: Long, status: ReviewStatus, pageable: Pageable): Page<Review>

    fun existsByOrderItemIdAndMemberIdAndStatusNot(orderItemId: Long, memberId: Long, status: ReviewStatus): Boolean

    fun findByIdAndMemberId(id: Long, memberId: Long): Review?

    fun countByProductIdAndStatus(productId: Long, status: ReviewStatus): Long

    // 정렬: 최신순
    @Query("SELECT r FROM Review r WHERE r.productId = :productId AND r.status = :status ORDER BY r.createdAt DESC")
    fun findByProductIdLatest(productId: Long, status: ReviewStatus, pageable: Pageable): Page<Review>

    // 정렬: 별점순 (높은 별점 우선)
    @Query("SELECT r FROM Review r WHERE r.productId = :productId AND r.status = :status ORDER BY r.rating DESC, r.createdAt DESC")
    fun findByProductIdRating(productId: Long, status: ReviewStatus, pageable: Pageable): Page<Review>

    // 정렬: 도움이 됐어요 순
    @Query("SELECT r FROM Review r WHERE r.productId = :productId AND r.status = :status ORDER BY r.helpfulCount DESC, r.createdAt DESC")
    fun findByProductIdHelpful(productId: Long, status: ReviewStatus, pageable: Pageable): Page<Review>

    // 포토리뷰 필터 + 정렬
    @Query("SELECT r FROM Review r WHERE r.productId = :productId AND r.status = :status AND r.hasImage = true ORDER BY r.createdAt DESC")
    fun findByProductIdPhotoOnlyLatest(productId: Long, status: ReviewStatus, pageable: Pageable): Page<Review>

    @Query("SELECT r FROM Review r WHERE r.productId = :productId AND r.status = :status AND r.hasImage = true ORDER BY r.rating DESC, r.createdAt DESC")
    fun findByProductIdPhotoOnlyRating(productId: Long, status: ReviewStatus, pageable: Pageable): Page<Review>

    @Query("SELECT r FROM Review r WHERE r.productId = :productId AND r.status = :status AND r.hasImage = true ORDER BY r.helpfulCount DESC, r.createdAt DESC")
    fun findByProductIdPhotoOnlyHelpful(productId: Long, status: ReviewStatus, pageable: Pageable): Page<Review>

    // "비슷한 체형" 필터 (키/몸무게 +-5 범위, US-802)
    @Query("""
        SELECT r FROM Review r
        WHERE r.productId = :productId
          AND r.status = :status
          AND r.height IS NOT NULL AND r.weight IS NOT NULL
          AND r.height BETWEEN :minHeight AND :maxHeight
          AND r.weight BETWEEN :minWeight AND :maxWeight
        ORDER BY r.createdAt DESC
    """)
    fun findBySimilarBody(
        productId: Long,
        status: ReviewStatus,
        minHeight: Int,
        maxHeight: Int,
        minWeight: Int,
        maxWeight: Int,
        pageable: Pageable,
    ): Page<Review>
}
