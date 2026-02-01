package com.biuea.wiki.domain.document

import com.biuea.wiki.domain.ai.AiAgentLog
import com.biuea.wiki.domain.common.BaseTimeEntity
import jakarta.persistence.CascadeType
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

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var children: MutableList<Document> = mutableListOf()
        protected set

    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var revisions: MutableList<DocumentRevision> = mutableListOf()
        protected set

    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var tagMaps: MutableList<DocumentTagMap> = mutableListOf()
        protected set

    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var summaries: MutableList<DocumentSummary> = mutableListOf()
        protected set

    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var agentLogs: MutableList<AiAgentLog> = mutableListOf()
        protected set

    fun softDelete() {
        this.status = DocumentStatus.DELETED
        this.deletedAt = LocalDateTime.now()
    }

    fun isDeleted(): Boolean = status == DocumentStatus.DELETED

    fun addChild(child: Document) {
        children.add(child)
        child.mergeTo(this)
    }

    fun addRevision(revision: DocumentRevision) {
        revisions.add(revision)
        revision.mappedBy(this)
    }

    fun addTagMaps(tagMaps: List<DocumentTagMap>, revision: DocumentRevision? = null) {
        tagMaps.forEach { it.mappedBy(this, revision) }
        this.tagMaps.addAll(tagMaps)
    }

    fun addSummary(summary: DocumentSummary, revision: DocumentRevision) {
        summaries.add(summary)
        summary.mappedBy(this, revision)
    }

    fun mergeTo(parent: Document) {
        this.parent = parent
    }
}
