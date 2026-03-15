package com.biuea.wiki.domain.document.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(
    name = "document_summary",
    indexes = [
        Index(name = "idx_doc_summary_document_id_revision_id", columnList = "document_id, document_revision_id"),
        Index(name = "idx_doc_summary_document_id", columnList = "document_id"),
    ]
)
class DocumentSummary(
    @Column(name = "content")
    var content: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_revision_id")
    var documentRevision: DocumentRevision,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    var document: Document,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) {
    fun mappedBy(document: Document, documentRevision: DocumentRevision) {
        this.document = document
        this.documentRevision = documentRevision
    }

    companion object {
        fun create(content: String, document: Document, documentRevision: DocumentRevision): DocumentSummary {
            return DocumentSummary(
                content = content,
                documentRevision = documentRevision,
                document = document
            )
        }
    }
}
