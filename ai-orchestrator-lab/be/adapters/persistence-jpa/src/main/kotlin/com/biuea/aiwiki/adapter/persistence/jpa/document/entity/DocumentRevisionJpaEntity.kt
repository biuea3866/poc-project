package com.biuea.aiwiki.adapter.persistence.jpa.document.entity

import com.biuea.aiwiki.domain.document.model.DocumentRevision
import com.biuea.aiwiki.domain.document.model.DocumentStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "document_revisions")
class DocumentRevisionJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "document_id", nullable = false)
    var documentId: Long,
    @Column(nullable = false)
    var title: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: DocumentStatus,
    @Column(name = "created_by", nullable = false)
    var createdBy: Long,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
) {
    fun toDomain(): DocumentRevision =
        DocumentRevision(
            id = id,
            documentId = documentId,
            title = title,
            content = content,
            status = status,
            createdBy = createdBy,
            createdAt = createdAt,
        )

    companion object {
        fun from(revision: DocumentRevision): DocumentRevisionJpaEntity =
            DocumentRevisionJpaEntity(
                id = revision.id,
                documentId = revision.documentId,
                title = revision.title,
                content = revision.content,
                status = revision.status,
                createdBy = revision.createdBy,
                createdAt = revision.createdAt ?: Instant.now(),
            )
    }
}
