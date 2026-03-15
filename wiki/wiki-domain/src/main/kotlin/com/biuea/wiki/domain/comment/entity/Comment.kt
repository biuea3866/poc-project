package com.biuea.wiki.domain.comment.entity

import com.biuea.wiki.domain.common.BaseTimeEntity
import com.biuea.wiki.domain.document.entity.Document
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "comment",
    indexes = [
        Index(name = "idx_comment_document_id", columnList = "document_id"),
        Index(name = "idx_comment_parent_id", columnList = "parent_id"),
    ]
)
class Comment(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    val document: Document,

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    var content: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    val parent: Comment? = null,

    @Column(name = "created_by", nullable = false)
    val createdBy: Long,

    @Column(name = "updated_by", nullable = false)
    var updatedBy: Long,

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    var replies: MutableList<Comment> = mutableListOf()
        protected set

    val isDeleted: Boolean get() = deletedAt != null

    fun update(content: String, updatedBy: Long) {
        this.content = content
        this.updatedBy = updatedBy
    }

    fun softDelete(deletedBy: Long) {
        this.deletedAt = LocalDateTime.now()
        this.updatedBy = deletedBy
    }

    fun isOwnedBy(userId: Long): Boolean = createdBy == userId
}
