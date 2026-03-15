package com.biuea.wiki.domain.tag.entity

import com.biuea.wiki.domain.document.entity.Document
import com.biuea.wiki.domain.document.entity.DocumentRevision
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

@Entity
@Table(
    name = "tag_document_mapping",
    indexes = [
        Index(name = "idx_tag_doc_mapping_tag_id", columnList = "tag_id"),
        Index(name = "idx_tag_doc_mapping_document_id", columnList = "document_id"),
        Index(name = "idx_tag_doc_mapping_document_revision_id", columnList = "document_revision_id"),
    ]
)
@EntityListeners(AuditingEntityListener::class)
class TagDocumentMapping(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id")
    var tag: Tag,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    var document: Document,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_revision_id")
    var documentRevision: DocumentRevision,

    @Column(name = "created_at")
    val createdAt: ZonedDateTime = ZonedDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) {
    companion object {
        fun create(
            tag: Tag,
            document: Document,
            documentRevision: DocumentRevision
        ): TagDocumentMapping {
            return TagDocumentMapping(
                tag = tag,
                document = document,
                documentRevision = documentRevision
            )
        }
    }
}
