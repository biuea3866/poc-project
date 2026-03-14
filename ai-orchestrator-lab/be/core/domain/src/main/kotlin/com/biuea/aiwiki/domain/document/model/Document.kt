package com.biuea.aiwiki.domain.document.model

import java.time.Instant

class Document(
    val id: Long? = null,
    title: String,
    content: String,
    status: DocumentStatus = DocumentStatus.DRAFT,
    aiStatus: AiStatus = AiStatus.NOT_STARTED,
    tags: List<String> = emptyList(),
    val createdBy: Long,
    updatedBy: Long,
    val createdAt: Instant? = null,
    updatedAt: Instant? = null,
) {
    var title: String = title
        private set

    var content: String = content
        private set

    var status: DocumentStatus = status
        private set

    var aiStatus: AiStatus = aiStatus
        private set

    private val _tags: MutableList<String> = tags.toMutableList()
    val tags: List<String>
        get() = _tags.toList()

    var updatedBy: Long = updatedBy
        private set

    var updatedAt: Instant? = updatedAt
        private set

    fun activate(userId: Long) {
        status = DocumentStatus.ACTIVE
        updatedBy = userId
    }

    fun delete(userId: Long) {
        require(status != DocumentStatus.DELETED) { "이미 삭제된 문서입니다." }
        status = DocumentStatus.DELETED
        updatedBy = userId
    }

    fun updateDraft(title: String, content: String, tags: List<String>, userId: Long) {
        this.title = title
        this.content = content
        _tags.clear()
        _tags.addAll(tags)
        updatedBy = userId
    }

    var summary: String? = null
        private set

    fun requestAnalysis(): Document {
        require(status == DocumentStatus.ACTIVE) { "ACTIVE 문서만 분석할 수 있습니다." }
        require(aiStatus != AiStatus.PROCESSING) { "PROCESSING 중에는 재요청할 수 없습니다." }
        aiStatus = AiStatus.PENDING
        return this
    }

    fun startProcessing() {
        require(aiStatus == AiStatus.PENDING) { "PENDING 상태에서만 처리를 시작할 수 있습니다." }
        aiStatus = AiStatus.PROCESSING
    }

    fun completeAnalysis(summary: String, tags: List<String>) {
        require(aiStatus == AiStatus.PROCESSING) { "PROCESSING 상태에서만 완료할 수 있습니다." }
        this.summary = summary
        _tags.clear()
        _tags.addAll(tags)
        aiStatus = AiStatus.COMPLETED
    }

    fun failAnalysis() {
        aiStatus = AiStatus.FAILED
    }

    fun applySummary(value: String) {
        summary = value
    }

    fun markUpdated(at: Instant) {
        updatedAt = at
    }
}
