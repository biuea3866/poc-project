package com.biuea.springdata.domain

import jakarta.persistence.*
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "comment")
data class Comment(
    @EmbeddedId
    val id: CommentId,

    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(name = "user_name", nullable = false, length = 100)
    val userName: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Column(nullable = false)
    val rating: Int,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Embeddable
data class CommentId(
    @Column(name = "id")
    val commentId: Long,

    @Column(name = "created_date", nullable = false)
    val createdDate: LocalDate
) : Serializable
