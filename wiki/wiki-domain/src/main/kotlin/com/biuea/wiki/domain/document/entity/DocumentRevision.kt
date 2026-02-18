package com.biuea.wiki.domain.document.entity

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
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

@Entity
@Table(name = "document_revision")
@EntityListeners(AuditingEntityListener::class)
class DocumentRevision(
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data")
    val data: DocumentRevisionData,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    var document: Document,

    @Column(name = "created_by")
    val createdBy: Long,

    @Column(name = "created_at")
    var createdAt: ZonedDateTime = ZonedDateTime.now(),

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
