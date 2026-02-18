package com.biuea.wiki.presentation.document

import com.biuea.wiki.application.AnalyzeDocumentFacade
import com.biuea.wiki.application.AnalyzeDocumentInput
import com.biuea.wiki.application.DeleteDocumentFacade
import com.biuea.wiki.application.DeleteDocumentInput
import com.biuea.wiki.application.GetDocumentAiStatusFacade
import com.biuea.wiki.application.GetDocumentFacade
import com.biuea.wiki.application.GetDocumentRevisionsFacade
import com.biuea.wiki.application.GetDocumentTagsFacade
import com.biuea.wiki.application.PublishDocumentFacade
import com.biuea.wiki.application.PublishDocumentInput
import com.biuea.wiki.application.RestoreDocumentFacade
import com.biuea.wiki.application.RestoreDocumentInput
import com.biuea.wiki.application.SaveDocumentFacade
import com.biuea.wiki.application.SaveDocumentInput
import com.biuea.wiki.application.UpdateDocumentFacade
import com.biuea.wiki.application.UpdateDocumentInput
import com.biuea.wiki.domain.auth.AuthenticatedUser
import com.biuea.wiki.domain.document.entity.AiStatus
import com.biuea.wiki.presentation.document.request.CreateDocumentRequest
import com.biuea.wiki.presentation.document.request.UpdateDocumentRequest
import com.biuea.wiki.presentation.document.response.AiStatusResponse
import com.biuea.wiki.presentation.document.response.CreateDocumentResponse
import com.biuea.wiki.presentation.document.response.DocumentDetailResponse
import com.biuea.wiki.presentation.document.response.DocumentListResponse
import com.biuea.wiki.presentation.document.response.DocumentRevisionResponse
import com.biuea.wiki.presentation.document.response.PublishDocumentResponse
import com.biuea.wiki.presentation.document.response.RestoreDocumentResponse
import com.biuea.wiki.presentation.document.response.TagResponse
import com.biuea.wiki.presentation.document.response.UpdateDocumentResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

@RestController
@RequestMapping("/api/v1/documents")
class DocumentApiController(
    private val saveDocumentFacade: SaveDocumentFacade,
    private val publishDocumentFacade: PublishDocumentFacade,
    private val getDocumentFacade: GetDocumentFacade,
    private val updateDocumentFacade: UpdateDocumentFacade,
    private val deleteDocumentFacade: DeleteDocumentFacade,
    private val restoreDocumentFacade: RestoreDocumentFacade,
    private val getDocumentRevisionsFacade: GetDocumentRevisionsFacade,
    private val getDocumentTagsFacade: GetDocumentTagsFacade,
    private val getDocumentAiStatusFacade: GetDocumentAiStatusFacade,
    private val analyzeDocumentFacade: AnalyzeDocumentFacade,
    private val sseEmitterManager: SseEmitterManager,
) {
    // POST /api/v1/documents — DRAFT 상태로 생성
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createDocument(
        @Valid @RequestBody request: CreateDocumentRequest,
        @AuthenticationPrincipal user: AuthenticatedUser,
    ): CreateDocumentResponse {
        val output = saveDocumentFacade.saveDocument(
            SaveDocumentInput(
                title = request.title,
                content = request.content,
                parentId = request.parentId,
                createdBy = user.id,
            )
        )
        return CreateDocumentResponse.from(output)
    }

    // POST /api/v1/documents/{id}/publish — DRAFT → ACTIVE + Kafka 이벤트
    @PostMapping("/{id}/publish")
    fun publishDocument(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: AuthenticatedUser,
    ): PublishDocumentResponse {
        val output = publishDocumentFacade.publish(
            PublishDocumentInput(documentId = id, userId = user.id)
        )
        return PublishDocumentResponse.from(output)
    }

    // GET /api/v1/documents — 목록 (parentId 없으면 루트, 있으면 하위 문서)
    @GetMapping
    fun listDocuments(
        @RequestParam(required = false) parentId: Long?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<DocumentListResponse> {
        return getDocumentFacade.listDocuments(parentId, pageable)
            .map { DocumentListResponse.from(it) }
    }

    // GET /api/v1/documents/trash — 삭제된 내 문서 목록
    @GetMapping("/trash")
    fun listTrash(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<DocumentListResponse> {
        return getDocumentFacade.listTrash(user.id, pageable)
            .map { DocumentListResponse.from(it) }
    }

    // GET /api/v1/documents/{id} — 상세 (요약·태그 포함)
    @GetMapping("/{id}")
    fun getDocument(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: AuthenticatedUser,
    ): DocumentDetailResponse {
        return DocumentDetailResponse.from(getDocumentFacade.getDocument(id, user.id))
    }

    // PUT /api/v1/documents/{id} — 수정 + revision 아카이빙
    @PutMapping("/{id}")
    fun updateDocument(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateDocumentRequest,
        @AuthenticationPrincipal user: AuthenticatedUser,
    ): UpdateDocumentResponse {
        val output = updateDocumentFacade.update(
            UpdateDocumentInput(
                documentId = id,
                userId = user.id,
                title = request.title,
                content = request.content,
            )
        )
        return UpdateDocumentResponse.from(output)
    }

    // DELETE /api/v1/documents/{id} — cascade 소프트 삭제
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteDocument(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: AuthenticatedUser,
    ) {
        deleteDocumentFacade.delete(DeleteDocumentInput(documentId = id, userId = user.id))
    }

    // POST /api/v1/documents/{id}/restore — Trash에서 복구
    @PostMapping("/{id}/restore")
    fun restoreDocument(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: AuthenticatedUser,
    ): RestoreDocumentResponse {
        val output = restoreDocumentFacade.restore(RestoreDocumentInput(documentId = id, userId = user.id))
        return RestoreDocumentResponse.from(output)
    }

    // GET /api/v1/documents/{id}/revisions — 변경 이력 목록
    @GetMapping("/{id}/revisions")
    fun getRevisions(
        @PathVariable id: Long,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<DocumentRevisionResponse> {
        return getDocumentRevisionsFacade.getRevisions(id, pageable)
            .map { DocumentRevisionResponse.from(it) }
    }

    // GET /api/v1/documents/{id}/tags — 태그 목록
    @GetMapping("/{id}/tags")
    fun getTags(@PathVariable id: Long): List<TagResponse> {
        return getDocumentTagsFacade.getTags(id).map { TagResponse.from(it) }
    }

    // GET /api/v1/documents/{id}/ai-status — AI 처리 상태 폴링
    @GetMapping("/{id}/ai-status")
    fun getAiStatus(@PathVariable id: Long): AiStatusResponse {
        return AiStatusResponse.from(getDocumentAiStatusFacade.getAiStatus(id))
    }

    // GET /api/v1/documents/{id}/ai-status/stream — SSE 스트림
    @GetMapping("/{id}/ai-status/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamAiStatus(@PathVariable id: Long): SseEmitter {
        return sseEmitterManager.createEmitter(id)
    }

    // POST /api/v1/documents/{id}/analyze — 수동 재분석
    @PostMapping("/{id}/analyze")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun analyzeDocument(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: AuthenticatedUser,
    ) {
        analyzeDocumentFacade.analyze(AnalyzeDocumentInput(documentId = id, userId = user.id))
    }
}
