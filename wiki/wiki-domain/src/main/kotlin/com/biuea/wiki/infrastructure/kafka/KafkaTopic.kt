package com.biuea.wiki.infrastructure.kafka

/**
 * Kafka 토픽 네이밍 컨벤션
 *
 * event.{domain}          — 이벤트성. 여러 컨슈머 그룹이 각자의 비즈니스를 처리 (pub/sub)
 * queue.{domain}.{action} — 목적성. 단일 컨슈머 그룹이 하나의 비즈니스를 처리 (point-to-point)
 */
object KafkaTopic {
    const val EVENT_DOCUMENT = "event.document"
    const val EVENT_AI_FAILED = "event.ai.failed"
    const val QUEUE_AI_TAGGING = "queue.ai.tagging"
    const val QUEUE_AI_EMBEDDING = "queue.ai.embedding"
}
