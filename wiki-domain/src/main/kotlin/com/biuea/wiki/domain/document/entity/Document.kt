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
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "document")
class Document(
    @Column(name = "title")
    var title: String,

    @Column(name = "content")
    var content: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    var status: DocumentStatus = DocumentStatus.DRAFT,

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_status")
    var aiStatus: AiStatus = AiStatus.PENDING,

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

    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var agentLogs: MutableList<AiAgentLog> = mutableListOf()
        protected set

    @jakarta.persistence.Transient
    private var documentRule: DocumentRule = DefaultDocumentRule()

    fun publish() {
        check(status == DocumentStatus.DRAFT) { "DRAFT 상태의 문서만 발행할 수 있습니다." }
        this.status = DocumentStatus.ACTIVE
        this.aiStatus = AiStatus.PENDING
    }

    fun startAiProcessing() {
        this.aiStatus = AiStatus.PROCESSING
    }

    fun completeAiProcessing() {
        this.aiStatus = AiStatus.COMPLETED
    }

    fun failAiProcessing() {
        this.aiStatus = AiStatus.FAILED
    }

    fun resetAiProcessing() {
        this.aiStatus = AiStatus.PENDING
    }

    fun delete() {
        this.status = DocumentStatus.DELETED
        this.deletedAt = ZonedDateTime.now()
    }

    fun restore(parent: Document?) {
        check(status == DocumentStatus.DELETED) { "DELETED 상태의 문서만 복구할 수 있습니다." }
        this.status = DocumentStatus.ACTIVE
        this.deletedAt = null
        this.parent = parent
    }

    fun isDeleted(): Boolean = status == DocumentStatus.DELETED

    fun isDraft(): Boolean = status == DocumentStatus.DRAFT

    fun isActive(): Boolean = status == DocumentStatus.ACTIVE

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
