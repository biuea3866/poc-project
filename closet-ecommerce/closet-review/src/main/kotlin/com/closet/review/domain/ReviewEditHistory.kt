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
 * 리뷰 수정 이력 엔티티 (US-801).
 *
 * CS 대응을 위해 수정 전 내용을 보존한다.
 */
@Entity
@Table(name = "review_edit_history")
@EntityListeners(AuditingEntityListener::class)
class ReviewEditHistory(
    @Column(name = "review_id", nullable = false)
    val reviewId: Long,

    @Column(name = "previous_content", nullable = false, length = 2000)
    val previousContent: String,

    @Column(name = "new_content", nullable = false, length = 2000)
    val newContent: String,

    @Column(name = "edit_count", nullable = false)
    val editCount: Int,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: ZonedDateTime

    companion object {
        fun create(reviewId: Long, previousContent: String, newContent: String, editCount: Int): ReviewEditHistory {
            return ReviewEditHistory(
                reviewId = reviewId,
                previousContent = previousContent,
                newContent = newContent,
                editCount = editCount,
            )
        }
    }
}
