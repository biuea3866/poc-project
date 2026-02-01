package com.biuea.wiki.domain.document

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "document_summary")
class DocumentSummary(
    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_revision_id", nullable = false)
    var documentRevision: DocumentRevision,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
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
