package com.biuea.wiki.domain.document.entity

import com.biuea.wiki.domain.ai.AiAgentLog
import com.biuea.wiki.domain.common.BaseTimeEntity
import com.biuea.wiki.domain.document.rule.DefaultDocumentRule
import com.biuea.wiki.domain.document.rule.DocumentRule
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(
    name = "document",
    indexes = [
        Index(name = "idx_document_parent_id_deleted_at", columnList = "parent_id, deleted_at"),
        Index(name = "idx_document_created_by_deleted_at", columnList = "created_by, deleted_at"),
        Index(name = "idx_document_deleted_at", columnList = "deleted_at"),
    ]
)
class Document(
    @Column(name = "title")
    var title: String,

    @Column(name = "content")
    var content: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    var status: DocumentStatus = DocumentStatus.PENDING,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: Document? = null,

    @Column(name = "deleted_at")
    var deletedAt: ZonedDateTime? = null,

    @Column(name = "created_by")
    val createdBy: Long,

    @Column(name = "updated_by")
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
    var summaries: MutableList<DocumentSummary> = mutableListOf()
        protected set

    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    var agentLogs: MutableList<AiAgentLog> = mutableListOf()
        protected set

    @Transient
    private var documentRule: DocumentRule = DefaultDocumentRule()

    val latestRevisionId get() = this.revisions.last().id

    fun softDelete() {
        this.status = DocumentStatus.DELETED
        this.deletedAt = ZonedDateTime.now()
        this.children.forEach { it.softDelete() }
    }

    fun restore() {
        check(isDeleted()) { "Document is not deleted: $id" }
        this.status = DocumentStatus.PENDING
        this.deletedAt = null
        if (this.parent != null && this.parent!!.isDeleted()) {
            this.parent = null
        }
    }

    fun publish() {
        this.status = DocumentStatus.COMPLETED
    }

    fun startAiProcessing() {
        // AI 처리 시작 — 상태는 PENDING 유지 (로그는 AiAgentLog로 추적)
    }

    fun completeAiProcessing() {
        this.status = DocumentStatus.COMPLETED
    }

    fun failAiProcessing() {
        this.status = DocumentStatus.FAILED
    }

    fun update(title: String, content: String?, updatedBy: Long) {
        this.title = title
        this.content = content
        this.updatedBy = updatedBy
    }

    fun isDeleted(): Boolean = status == DocumentStatus.DELETED

    fun applyDocumentRule(documentRule: DocumentRule) {
        this.documentRule = documentRule
    }

    fun addChild(child: Document) {
        children.add(child)
        child.mergeTo(this)
    }

    fun addRevision(revision: DocumentRevision) {
        revisions.add(revision)
        revision.mappedBy(this)
    }

    fun addSummary(summary: DocumentSummary, revision: DocumentRevision) {
        summaries.add(summary)
        summary.mappedBy(this, revision)
    }

    fun mergeTo(parent: Document) {
        this.parent = parent
    }

    fun validate() {
        this.documentRule.validate(this)
    }
}
