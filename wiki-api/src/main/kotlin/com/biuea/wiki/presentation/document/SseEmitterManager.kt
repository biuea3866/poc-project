package com.biuea.wiki.presentation.document

import com.biuea.wiki.domain.document.entity.AiStatus
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * SSE 연결 관리자.
 *
 * 요구사항:
 * - event: ai-status-update + data: JSON 포맷
 * - 30초 Heartbeat
 * - 서버가 COMPLETED/FAILED 전송 시 스트림 종료
 */
@Component
class SseEmitterManager(
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val emitters = ConcurrentHashMap<Long, MutableList<SseEmitter>>()
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    init {
        // 30초 Heartbeat
        scheduler.scheduleAtFixedRate(::sendHeartbeats, 30, 30, TimeUnit.SECONDS)
    }

    fun createEmitter(documentId: Long): SseEmitter {
        val emitter = SseEmitter(0L) // 타임아웃 없음 (클라이언트가 제어)
        emitters.getOrPut(documentId) { mutableListOf() }.add(emitter)

        emitter.onCompletion { removeEmitter(documentId, emitter) }
        emitter.onTimeout { removeEmitter(documentId, emitter) }
        emitter.onError { removeEmitter(documentId, emitter) }

        log.info("[SSE] 연결 생성 documentId=$documentId")
        return emitter
    }

    fun broadcast(documentId: Long, aiStatus: AiStatus) {
        val payload = objectMapper.writeValueAsString(
            mapOf("documentId" to documentId, "status" to aiStatus.name)
        )
        val deadEmitters = mutableListOf<SseEmitter>()

        emitters[documentId]?.forEach { emitter ->
            try {
                emitter.send(
                    SseEmitter.event()
                        .name("ai-status-update")
                        .data(payload)
                )
                if (aiStatus == AiStatus.COMPLETED || aiStatus == AiStatus.FAILED) {
                    emitter.complete()
                    deadEmitters.add(emitter)
                }
            } catch (e: Exception) {
                deadEmitters.add(emitter)
            }
        }
        deadEmitters.forEach { removeEmitter(documentId, it) }
    }

    private fun sendHeartbeats() {
        emitters.forEach { (documentId, list) ->
            val dead = mutableListOf<SseEmitter>()
            list.forEach { emitter ->
                try {
                    emitter.send(SseEmitter.event().name("heartbeat").data("{}"))
                } catch (e: Exception) {
                    dead.add(emitter)
                }
            }
            dead.forEach { removeEmitter(documentId, it) }
        }
    }

    private fun removeEmitter(documentId: Long, emitter: SseEmitter) {
        emitters[documentId]?.remove(emitter)
        if (emitters[documentId]?.isEmpty() == true) {
            emitters.remove(documentId)
        }
    }
}
