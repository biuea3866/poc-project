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
    title: String,
    content: String? = null,
    status: DocumentStatus = DocumentStatus.DRAFT,
    aiStatus: AiStatus = AiStatus.PENDING,
    parent: Document? = null,
    deletedAt: ZonedDateTime? = null,

    @Column(name = "created_by")
    val createdBy: Long,

    updatedBy: Long,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {

    @Column(name = "title")
    var title: String = title
        private set

    @Column(name = "content")
    var content: String? = content
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    var status: DocumentStatus = status
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_status")
    var aiStatus: AiStatus = aiStatus
        private set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: Document? = parent
        private set

    @Column(name = "deleted_at")
    var deletedAt: ZonedDateTime? = deletedAt
        private set

    @Column(name = "updated_by")
    var updatedBy: Long = updatedBy
        private set

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

    // ── 수정 ──────────────────────────────────────────────────────────────────
    fun update(title: String, content: String?, updatedBy: Long) {
        this.title = title
        this.content = content
        this.updatedBy = updatedBy
        validate()
    }

    // ── 발행 ──────────────────────────────────────────────────────────────────
    fun publish() {
        check(status == DocumentStatus.DRAFT) { "DRAFT 상태의 문서만 발행할 수 있습니다." }
        this.status = DocumentStatus.ACTIVE
        this.aiStatus = AiStatus.PENDING
    }

    // ── AI 상태 전이 ──────────────────────────────────────────────────────────
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

    // ── 삭제 ──────────────────────────────────────────────────────────────────
    fun delete() {
        this.status = DocumentStatus.DELETED
        this.deletedAt = ZonedDateTime.now()
    }

    /** 자신 및 모든 하위 문서를 소프트 삭제하고 변경된 Document 목록을 반환한다. */
    fun deleteWithDescendants(): List<Document> {
        this.delete()
        return buildList {
            add(this@Document)
            children
                .filter { !it.isDeleted() }
                .forEach { addAll(it.deleteWithDescendants()) }
        }
    }

    // ── 복구 ──────────────────────────────────────────────────────────────────
    fun restore(parent: Document?) {
        check(status == DocumentStatus.DELETED) { "DELETED 상태의 문서만 복구할 수 있습니다." }
        this.status = DocumentStatus.ACTIVE
        this.deletedAt = null
        this.parent = parent
    }

    /** 부모가 ACTIVE 상태인 경우에만 반환한다. 삭제된 부모면 null (루트로 복구). */
    fun resolveActiveParent(): Document? = parent?.takeIf { it.isActive() }

    // ── 조회 헬퍼 ─────────────────────────────────────────────────────────────
    fun isDeleted(): Boolean = status == DocumentStatus.DELETED

    fun isDraft(): Boolean = status == DocumentStatus.DRAFT

    fun isActive(): Boolean = status == DocumentStatus.ACTIVE

    /** 이 문서를 userId가 조회할 수 있는지 판단한다. DRAFT는 작성자 본인만 허용. */
    fun isViewableBy(userId: Long): Boolean = !isDraft() || createdBy == userId

    /** 가장 최근 리비전을 반환한다. */
    fun latestRevision(): DocumentRevision? = revisions.lastOrNull()

    // ── 관계 관리 ─────────────────────────────────────────────────────────────
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
