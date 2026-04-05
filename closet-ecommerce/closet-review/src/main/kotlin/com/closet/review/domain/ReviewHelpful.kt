package com.closet.review.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

/**
 * 리뷰 "도움이 됐어요" 엔티티.
 *
 * 회원당 리뷰 1건당 1회만 투표 가능하다.
 */
@Entity
@Table(name = "review_helpful")
@EntityListeners(AuditingEntityListener::class)
class ReviewHelpful(
    @Column(name = "review_id", nullable = false)
    val reviewId: Long,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: ZonedDateTime

    companion object {
        fun create(reviewId: Long, memberId: Long): ReviewHelpful {
            return ReviewHelpful(reviewId = reviewId, memberId = memberId)
        }
    }
}
