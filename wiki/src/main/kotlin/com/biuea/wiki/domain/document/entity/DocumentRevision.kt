package com.biuea.wiki.domain.document.entity

import com.biuea.wiki.domain.ai.AiAgentLog
import io.hypersistence.utils.hibernate.type.json.JsonStringType
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

@Entity
@Table(name = "document_revision")
@EntityListeners(AuditingEntityListener::class)
class DocumentRevision(
    @Type(JsonStringType::class)
    @Column(nullable = false, columnDefinition = "TEXT")
    val data: DocumentRevisionData,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    var document: Document,

    @Column(name = "created_by", nullable = false, updatable = false)
    val createdBy: Long,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: ZonedDateTime = ZonedDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) {

    @OneToMany(mappedBy = "documentRevision", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var tagMaps: MutableList<DocumentTagMap> = mutableListOf()
        protected set

    @OneToMany(mappedBy = "documentRevision", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var summaries: MutableList<DocumentSummary> = mutableListOf()
        protected set

    @OneToMany(mappedBy = "documentRevision", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var agentLogs: MutableList<AiAgentLog> = mutableListOf()
        protected set

    fun mappedBy(document: Document) {
        this.document = document
    }

    fun addTagMaps(tagMaps: List<DocumentTagMap>) {
        tagMaps.forEach { it.mappedBy(document, this) }
        this.tagMaps.addAll(tagMaps)
    }

    fun addSummary(summary: DocumentSummary) {
        summaries.add(summary)
        summary.mappedBy(document, this)
    }

    companion object {
        fun create(document: Document): DocumentRevision {
            return DocumentRevision(
                data = DocumentRevisionData.from(document),
                document = document,
                createdBy = document.createdBy,
            )
        }
    }
}

data class DocumentRevisionData(
    val title: String,
    val content: String?,
    val status: DocumentStatus,
    val parent: Document?,
    val children: List<Document>,
    val deletedAt: ZonedDateTime?,
    val createdBy: Long,
    val updatedBy: Long,
) {
    companion object {
        fun from(document: Document): DocumentRevisionData {
            return DocumentRevisionData(
                title = document.title,
                content = document.content,
                status = document.status,
                parent = document.parent,
                children = document.children,
                deletedAt = document.deletedAt,
                createdBy = document.createdBy,
                updatedBy = document.updatedBy,
            )
        }
    }
}
