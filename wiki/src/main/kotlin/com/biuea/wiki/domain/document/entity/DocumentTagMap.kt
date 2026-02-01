package com.biuea.wiki.domain.document.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

@Entity
@Table(name = "document_tag_map")
@EntityListeners(AuditingEntityListener::class)
class DocumentTagMap(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    val tag: Tag,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    var document: Document,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_revision_id")
    var documentRevision: DocumentRevision? = null,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: ZonedDateTime = ZonedDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) {
    fun mappedBy(document: Document, documentRevision: DocumentRevision? = null) {
        this.document = document
        this.documentRevision = documentRevision
    }

    companion object {
        fun create(tag: Tag, document: Document, documentRevision: DocumentRevision? = null): DocumentTagMap {
            return DocumentTagMap(
                tag = tag,
                document = document,
                documentRevision = documentRevision
            )
        }
    }
}
