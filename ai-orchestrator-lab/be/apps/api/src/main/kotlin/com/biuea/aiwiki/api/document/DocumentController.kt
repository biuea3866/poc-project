package com.biuea.aiwiki.api.document

import com.biuea.aiwiki.application.document.AiPipelineService
import com.biuea.aiwiki.application.document.AiStatusChangedEvent
import com.biuea.aiwiki.application.document.CreateDraftCommand
import com.biuea.aiwiki.application.document.DocumentCommandService
import com.biuea.aiwiki.application.document.DocumentQueryService
import com.biuea.aiwiki.application.document.UpdateDraftCommand
import jakarta.validation.constraints.NotBlank
import org.springframework.context.event.EventListener
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Validated
@RestController
@RequestMapping("/api/v1/documents")
class DocumentController(
    private val documentCommandService: DocumentCommandService,
    private val documentQueryService: DocumentQueryService,
    private val aiPipelineService: AiPipelineService,
) {
    private val emitters = ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>>()

    @PostMapping
    fun createDraft(@RequestBody request: CreateDraftRequest): ResponseEntity<DocumentResponse> {
        val document = documentCommandService.createDraft(
            CreateDraftCommand(
                title = request.title,
                content = request.content,
                tags = request.tags,
                userId = request.userId,
            )
        )
        return ResponseEntity.ok(DocumentResponse.from(document))
    }

    @PostMapping("/{documentId}/analyze")
    fun analyze(@PathVariable documentId: Long): ResponseEntity<DocumentResponse> {
        val document = documentCommandService.requestAnalysis(documentId)
        aiPipelineService.processDocument(documentId)
        return ResponseEntity.ok(DocumentResponse.from(document))
    }

    @PostMapping("/{documentId}/activate")
    fun activate(
        @PathVariable documentId: Long,
        @RequestParam(defaultValue = "1") userId: Long,
    ): ResponseEntity<DocumentResponse> {
        val document = documentCommandService.activate(documentId, userId)
        return ResponseEntity.ok(DocumentResponse.from(document))
    }

    @PatchMapping("/{documentId}")
    fun updateDraft(
        @PathVariable documentId: Long,
        @RequestBody request: UpdateDraftRequest,
    ): ResponseEntity<DocumentResponse> {
        val document = documentCommandService.updateDraft(
            UpdateDraftCommand(
                documentId = documentId,
                title = request.title,
                content = request.content,
                tags = request.tags,
                userId = request.userId,
                expectedUpdatedAt = request.expectedUpdatedAt,
            )
        )
        return ResponseEntity.ok(DocumentResponse.from(document))
    }

    @DeleteMapping("/{documentId}")
    fun deleteDocument(
        @PathVariable documentId: Long,
        @RequestParam(defaultValue = "1") userId: Long,
    ): ResponseEntity<DocumentResponse> {
        val document = documentCommandService.deleteDocument(documentId, userId)
        return ResponseEntity.ok(DocumentResponse.from(document))
    }

    @GetMapping("/health")
    fun health(@RequestParam(required = false) q: String?): ResponseEntity<Map<String, String?>> =
        ResponseEntity.ok(mapOf("service" to "ai-wiki-api", "query" to q))

    @GetMapping
    fun search(
        @RequestParam(defaultValue = "1") userId: Long,
        @RequestParam(defaultValue = "") q: String,
    ): ResponseEntity<List<DocumentResponse>> =
        ResponseEntity.ok(documentQueryService.searchActiveDocuments(userId, q).map(DocumentResponse::from))

    @GetMapping("/{documentId}")
    fun getDocument(@PathVariable documentId: Long): ResponseEntity<DocumentResponse> =
        ResponseEntity.ok(DocumentResponse.from(documentQueryService.getDocument(documentId)))

    @GetMapping("/{documentId}/ai-status/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamAiStatus(@PathVariable documentId: Long): SseEmitter {
        val emitter = SseEmitter(60_000L)
        val document = documentQueryService.getDocument(documentId)

        // Send current status immediately
        emitter.send(SseEmitter.event().name("ai-status").data(mapOf("status" to document.aiStatus.name)))

        // Register emitter for future updates
        emitters.computeIfAbsent(documentId) { CopyOnWriteArrayList() }.add(emitter)

        emitter.onCompletion { removeEmitter(documentId, emitter) }
        emitter.onTimeout { removeEmitter(documentId, emitter) }
        emitter.onError { removeEmitter(documentId, emitter) }

        return emitter
    }

    @EventListener
    fun onAiStatusChanged(event: AiStatusChangedEvent) {
        val documentEmitters = emitters[event.documentId] ?: return
        val deadEmitters = mutableListOf<SseEmitter>()

        for (emitter in documentEmitters) {
            try {
                emitter.send(
                    SseEmitter.event()
                        .name("ai-status")
                        .data(mapOf("status" to event.status.name))
                )
                if (event.status.name == "COMPLETED" || event.status.name == "FAILED") {
                    emitter.complete()
                    deadEmitters.add(emitter)
                }
            } catch (_: Exception) {
                deadEmitters.add(emitter)
            }
        }

        documentEmitters.removeAll(deadEmitters.toSet())
        if (documentEmitters.isEmpty()) {
            emitters.remove(event.documentId)
        }
    }

    private fun removeEmitter(documentId: Long, emitter: SseEmitter) {
        emitters[documentId]?.remove(emitter)
        if (emitters[documentId]?.isEmpty() == true) {
            emitters.remove(documentId)
        }
    }
}

data class CreateDraftRequest(
    @field:NotBlank
    val title: String,
    @field:NotBlank
    val content: String,
    val tags: List<String> = emptyList(),
    val userId: Long = 1L,
)

data class UpdateDraftRequest(
    @field:NotBlank
    val title: String,
    @field:NotBlank
    val content: String,
    val tags: List<String> = emptyList(),
    val userId: Long = 1L,
    val expectedUpdatedAt: Instant,
)

data class DocumentResponse(
    val id: Long?,
    val title: String,
    val status: String,
    val aiStatus: String,
    val excerpt: String,
    val tags: List<String>,
    val updatedAt: String,
) {
    companion object {
        fun from(document: com.biuea.aiwiki.domain.document.model.Document): DocumentResponse =
            DocumentResponse(
                id = document.id,
                title = document.title,
                status = document.status.name,
                aiStatus = document.aiStatus.name,
                excerpt = document.content.take(100),
                tags = document.tags,
                updatedAt = (document.updatedAt ?: document.createdAt)?.toString() ?: "",
            )
    }
}
