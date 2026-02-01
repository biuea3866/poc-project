package com.biuea.wiki.domain.document

import com.biuea.wiki.domain.ai.AiAgentLog
import com.biuea.wiki.domain.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "document")
class Document(
    @Column(nullable = false)
    var title: String,

    @Column(columnDefinition = "TEXT")
    var content: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: DocumentStatus = DocumentStatus.PENDING,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: Document? = null,

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,

    @Column(name = "created_by", nullable = false, updatable = false)
    val createdBy: Long,

    @Column(name = "updated_by", nullable = false)
    var updatedBy: Long,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    val children: MutableList<Document> = mutableListOf()

    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY)
    val revisions: MutableList<DocumentRevision> = mutableListOf()

    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY)
    val tags: MutableList<DocumentTag> = mutableListOf()

    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY)
    val summaries: MutableList<DocumentSummary> = mutableListOf()

    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY)
    val agentLogs: MutableList<AiAgentLog> = mutableListOf()

    fun softDelete() {
        this.status = DocumentStatus.DELETED
        this.deletedAt = LocalDateTime.now()
    }

    fun isDeleted(): Boolean = status == DocumentStatus.DELETED
}
