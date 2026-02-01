package com.biuea.wiki.domain.document

import com.biuea.wiki.domain.ai.AiAgentLog
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
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "document_revision")
@EntityListeners(AuditingEntityListener::class)
class DocumentRevision(
    @Column(nullable = false, columnDefinition = "TEXT")
    val data: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    var document: Document,

    @Column(name = "created_by", nullable = false, updatable = false)
    val createdBy: Long,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

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
        fun create(data: String, document: Document, createdBy: Long): DocumentRevision {
            return DocumentRevision(
                data = data,
                document = document,
                createdBy = createdBy
            )
        }
    }
}
