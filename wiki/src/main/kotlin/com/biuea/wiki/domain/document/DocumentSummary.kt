package com.biuea.wiki.domain.document

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "document_summary")
class DocumentSummary(
    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(name = "document_revision_id", nullable = false)
    val documentRevisionId: Long,

    @Column(name = "document_id", nullable = false)
    val documentId: Long,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
)
