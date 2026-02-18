package com.biuea.wiki.domain.document.entity

import com.biuea.wiki.domain.ai.AiAgentLog
import io.hypersistence.utils.hibernate.type.json.JsonStringType
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import java.time.ZonedDateTime

@Entity
@Table(name = "document_revision")
class DocumentRevision(
    @Type(JsonStringType::class)
    @Column(name = "data", columnDefinition = "TEXT")
    val data: DocumentRevisionData,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    var document: Document,

    @Column(name = "created_by")
    val createdBy: Long,

    @Column(name = "created_at")
    val createdAt: ZonedDateTime = ZonedDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) {
    @OneToMany(mappedBy = "documentRevision", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var summaries: MutableList<DocumentSummary> = mutableListOf()
        protected set

    @OneToMany(mappedBy = "documentRevision", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var agentLogs: MutableList<AiAgentLog> = mutableListOf()
        protected set

    fun mappedBy(document: Document) {
        this.document = document
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
                createdBy = document.updatedBy,
            )
        }
    }
}

data class DocumentRevisionData(
    val title: String,
    val content: String?,
    val status: DocumentStatus,
    val parentId: Long?,   // 엔티티 참조 대신 ID만 저장
) {
    companion object {
        fun from(document: Document): DocumentRevisionData {
            return DocumentRevisionData(
                title = document.title,
                content = document.content,
                status = document.status,
                parentId = document.parent?.id,
            )
        }
    }
}
