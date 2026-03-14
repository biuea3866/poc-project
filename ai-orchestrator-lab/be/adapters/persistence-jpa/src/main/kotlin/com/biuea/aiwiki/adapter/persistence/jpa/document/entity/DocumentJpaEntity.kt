package com.biuea.aiwiki.adapter.persistence.jpa.document.entity

import com.biuea.aiwiki.domain.document.model.AiStatus
import com.biuea.aiwiki.domain.document.model.Document
import com.biuea.aiwiki.domain.document.model.DocumentStatus
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant

@Entity
@Table(name = "documents")
class DocumentJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    var title: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: DocumentStatus,
    @Enumerated(EnumType.STRING)
    @Column(name = "ai_status", nullable = false)
    var aiStatus: AiStatus,
    @ElementCollection
    @CollectionTable(name = "document_tags", joinColumns = [JoinColumn(name = "document_id")])
    @Column(name = "tag_name")
    var tags: MutableList<String> = mutableListOf(),
    @Column(name = "summary", columnDefinition = "TEXT")
    var summary: String? = null,
    @Column(name = "created_by", nullable = false)
    var createdBy: Long,
    @Column(name = "updated_by", nullable = false)
    var updatedBy: Long,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Version
    @Column(name = "entity_version", nullable = false)
    var version: Long = 0L,
) {
    fun toDomain(): Document {
        val doc = Document(
            id = id,
            title = title,
            content = content,
            status = status,
            aiStatus = aiStatus,
            tags = tags.toList(),
            createdBy = createdBy,
            updatedBy = updatedBy,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
        summary?.let { doc.applySummary(it) }
        return doc
    }

    companion object {
        fun from(document: Document): DocumentJpaEntity =
            DocumentJpaEntity(
                id = document.id,
                title = document.title,
                content = document.content,
                status = document.status,
                aiStatus = document.aiStatus,
                tags = document.tags.toMutableList(),
                createdBy = document.createdBy,
                updatedBy = document.updatedBy,
                createdAt = document.createdAt ?: Instant.now(),
                summary = document.summary,
                updatedAt = document.updatedAt ?: Instant.now(),
            )
    }
}
