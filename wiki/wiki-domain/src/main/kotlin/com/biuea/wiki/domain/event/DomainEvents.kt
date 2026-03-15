package com.biuea.wiki.domain.event

data class DocumentCreatedEvent(
    val documentId: Long,
    val documentRevisionId: Long,
    val title: String,
    val content: String?,
)

data class AiTaggingRequestEvent(
    val documentId: Long,
    val documentRevisionId: Long,
    val title: String,
    val summary: String,
) {
    data class TagItem(
        val name: String,
        val tagConstant: String,
    )
}

data class AiEmbeddingRequestEvent(
    val documentId: Long,
    val documentRevisionId: Long,
    val tags: List<TagItem>,
) {
    data class TagItem(
        val name: String,
        val tagConstant: String,
    )
}

data class AiProcessingFailedEvent(
    val documentId: Long,
    val documentRevisionId: Long,
    val agentType: String,
    val reason: String,
)
