package com.biuea.wiki.domain.comment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "comments",
    indexes = [
        Index(name = "idx_comment_document", columnList = "document_id"),
        Index(name = "idx_comment_parent", columnList = "parent_id"),
    ]
)
class Comment(
    @Column(name = "document_id", nullable = false)
    val documentId: Long,

    @Column(name = "author_id", nullable = false)
    val authorId: Long,

    @Column(columnDefinition = "TEXT", nullable = false)
    var content: String,

    @Column(name = "parent_id")
    val parentId: Long? = null,

    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
)
