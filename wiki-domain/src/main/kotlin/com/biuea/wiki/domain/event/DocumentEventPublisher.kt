package com.biuea.wiki.domain.event

interface DocumentEventPublisher {
    fun publishDocumentCreated(event: DocumentCreatedEvent)
    fun publishAiProcessingFailed(event: AiProcessingFailedEvent)
}
