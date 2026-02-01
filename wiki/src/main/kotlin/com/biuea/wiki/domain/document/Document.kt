package com.biuea.wiki.domain.document

import com.biuea.wiki.domain.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "document")
class Document(
    @Column(nullable = false)
    var title: String,

    @Column(columnDefinition = "TEXT")
    var content: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: DocumentStatus = DocumentStatus.PENDING,

    @Column(name = "parent_id")
    var parentId: Long? = null,

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,

    @Column(name = "created_by", nullable = false, updatable = false)
    val createdBy: Long,

    @Column(name = "updated_by", nullable = false)
    var updatedBy: Long,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {

    fun softDelete() {
        this.status = DocumentStatus.DELETED
        this.deletedAt = LocalDateTime.now()
    }

    fun isDeleted(): Boolean = status == DocumentStatus.DELETED
}
