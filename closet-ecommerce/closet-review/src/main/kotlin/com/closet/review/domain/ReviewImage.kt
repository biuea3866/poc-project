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
import java.time.LocalDateTime

/**
 * 리뷰 이미지 엔티티.
 *
 * 원본 + 썸네일(400x400) 경로를 저장한다 (PD-33).
 * Phase 2: 로컬 스토리지 저장, Phase 3: S3 전환.
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
    lateinit var createdAt: LocalDateTime
}
