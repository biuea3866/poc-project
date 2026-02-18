package com.biuea.wiki.domain.event

/**
 * Kafka External Events — 도메인 간 통신에 사용
 *
 * [토픽 컨벤션]
 *  event.{domain}          → 여러 컨슈머 그룹이 각자의 비즈니스를 처리 (pub/sub)
 *  queue.{domain}.{action} → 단일 컨슈머 그룹이 하나의 비즈니스를 처리 (point-to-point)
 *
 * [ApplicationEvent vs Kafka]
 *  ApplicationEvent → 같은 JVM 내 도메인 internal event
 *  Kafka Event      → 모듈(서비스) 간 external event
 */

// topic: event.document
// 문서 발행 이벤트 — 여러 컨슈머 그룹이 반응 (SUMMARY agent, 미래 analytics 등)
data class DocumentCreatedEvent(
    val documentId: Long,
    val documentRevisionId: Long,
    val title: String,
    val content: String?,
    val userId: Long,
)

// topic: queue.ai.tagging
// TAGGER 에이전트에게 태깅 작업 요청 (SUMMARY 완료 후 단일 컨슈머만 소비)
data class AiTaggingRequestEvent(
    val documentId: Long,
    val documentRevisionId: Long,
    val title: String,
    val summary: String,
)

// topic: queue.ai.embedding
// EMBEDDING 에이전트에게 임베딩 작업 요청 (TAGGER 완료 후 단일 컨슈머만 소비)
data class AiEmbeddingRequestEvent(
    val documentId: Long,
    val documentRevisionId: Long,
    val tags: List<TagItem>,
) {
    data class TagItem(val name: String, val tagConstant: String)
}

// topic: event.ai.failed
// AI 처리 실패 이벤트 — 상태 업데이터, 알림 등 여러 그룹이 처리 가능
data class AiProcessingFailedEvent(
    val documentId: Long,
    val documentRevisionId: Long,
    val agentType: String,
    val reason: String,
)
