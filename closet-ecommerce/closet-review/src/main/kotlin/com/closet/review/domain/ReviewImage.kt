package com.closet.review.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

/**
 * 리뷰 이미지 엔티티 (US-801).
 *
 * Presigned URL로 MinIO/S3에 업로드된 이미지의 URL을 저장한다.
 */
@Entity
@Table(name = "review_image")
@EntityListeners(AuditingEntityListener::class)
class ReviewImage(
    @ManyToOne
    @JoinColumn(name = "review_id", nullable = false)
    val review: Review,
    @Column(name = "image_url", nullable = false, length = 500)
    val imageUrl: String,
    @Column(name = "thumbnail_url", nullable = false, length = 500)
    val thumbnailUrl: String,
    @Column(name = "display_order", nullable = false)
    val displayOrder: Int,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: ZonedDateTime
}
