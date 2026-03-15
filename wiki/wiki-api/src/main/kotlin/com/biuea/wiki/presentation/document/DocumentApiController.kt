package com.biuea.wiki.presentation.document

import com.biuea.wiki.application.PublishDocumentFacade
import com.biuea.wiki.application.SaveDocumentFacade
import com.biuea.wiki.application.SaveDocumentInput
import com.biuea.wiki.domain.auth.AuthenticatedUser
import com.biuea.wiki.domain.document.DocumentService
import com.biuea.wiki.domain.document.UpdateDocumentCommand
import com.biuea.wiki.domain.document.entity.DocumentStatus
import com.biuea.wiki.presentation.document.request.CreateDocumentRequest
import com.biuea.wiki.presentation.document.request.UpdateDocumentRequest
import com.biuea.wiki.presentation.document.response.AiStatusResponse
import com.biuea.wiki.presentation.document.response.DocumentListResponse
import com.biuea.wiki.presentation.document.response.DocumentResponse
import com.biuea.wiki.presentation.document.response.RevisionListResponse
import com.biuea.wiki.presentation.document.response.RevisionResponse
import com.biuea.wiki.presentation.document.response.TagResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/documents")
class DocumentApiController(
    private val documentService: DocumentService,
    private val saveDocumentFacade: SaveDocumentFacade,
    private val publishDocumentFacade: PublishDocumentFacade,
) {
    @PostMapping
    fun createDocument(
        @RequestBody @Valid request: CreateDocumentRequest,
        authentication: Authentication,
    ): ResponseEntity<DocumentResponse> {
        val user = authentication.principal as AuthenticatedUser
        val output = saveDocumentFacade.saveDocument(
            SaveDocumentInput(
                title = request.title,
                content = request.content,
                status = DocumentStatus.PENDING,
                parentId = request.parentId,
                createdBy = user.id,
                tags = request.tags.orEmpty().map {
                    SaveDocumentInput.TagInput(name = it.name, tagConstant = it.tagConstant)
                }
            )
        )
        val document = documentService.getDocument(output.document.id)
        return ResponseEntity.ok(DocumentResponse.from(document))
    }

    @GetMapping
    fun listDocuments(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) parentId: Long?,
    ): ResponseEntity<DocumentListResponse> {
        val result = if (parentId != null) {
            documentService.listDocumentsByParent(parentId, page, size)
        } else {
            documentService.listRootDocuments(page, size)
        }
        return ResponseEntity.ok(
            DocumentListResponse(
                documents = result.content.map { DocumentResponse.from(it) },
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            )
        )
    }

    @GetMapping("/{id}")
    fun getDocument(@PathVariable id: Long): ResponseEntity<DocumentResponse> {
        val document = documentService.getDocument(id)
        return ResponseEntity.ok(DocumentResponse.from(document))
    }

    @PutMapping("/{id}")
    fun updateDocument(
        @PathVariable id: Long,
        @RequestBody @Valid request: UpdateDocumentRequest,
        authentication: Authentication,
    ): ResponseEntity<DocumentResponse> {
        val user = authentication.principal as AuthenticatedUser
        val document = documentService.updateDocument(
            UpdateDocumentCommand(
                id = id,
                title = request.title,
                content = request.content,
                updatedBy = user.id,
            )
        )
        return ResponseEntity.ok(DocumentResponse.from(document))
    }

    @DeleteMapping("/{id}")
    fun deleteDocument(@PathVariable id: Long): ResponseEntity<Void> {
        documentService.deleteDocument(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/restore")
    fun restoreDocument(@PathVariable id: Long): ResponseEntity<DocumentResponse> {
        val document = documentService.restoreDocument(id)
        return ResponseEntity.ok(DocumentResponse.from(document))
    }

    @GetMapping("/trash")
    fun listTrash(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<DocumentListResponse> {
        val result = documentService.listTrash(page, size)
        return ResponseEntity.ok(
            DocumentListResponse(
                documents = result.content.map { DocumentResponse.from(it) },
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            )
        )
    }

    @GetMapping("/{id}/revisions")
    fun getRevisions(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<RevisionListResponse> {
        val result = documentService.getRevisions(id, page, size)
        return ResponseEntity.ok(
            RevisionListResponse(
                revisions = result.content.map { RevisionResponse.from(it) },
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            )
        )
    }

    @GetMapping("/{id}/tags")
    fun getDocumentTags(@PathVariable id: Long): ResponseEntity<List<TagResponse>> {
        val mappings = documentService.getDocumentTags(id)
        return ResponseEntity.ok(mappings.map { TagResponse.from(it) })
    }

    @GetMapping("/{id}/ai-status")
    fun getAiStatus(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<AiStatusResponse> {
        val result = documentService.getAiStatus(id, page, size)
        return ResponseEntity.ok(AiStatusResponse.from(id, result.content))
    }

    @PostMapping("/{id}/publish")
    fun publishDocument(
        @PathVariable id: Long,
        authentication: Authentication,
    ): ResponseEntity<DocumentResponse> {
        val principal = authentication.principal as AuthenticatedUser
<<<<<<< HEAD
        val document = publishDocumentFacade.publish(id, principal.id)
=======
        val document = documentService.publishDocument(id, principal.id)
>>>>>>> origin/main
        return ResponseEntity.ok(DocumentResponse.from(document))
    }

    @PostMapping("/{id}/analyze")
    fun analyzeDocument(@PathVariable id: Long): ResponseEntity<DocumentResponse> {
        val document = publishDocumentFacade.reanalyze(id)
        return ResponseEntity.ok(DocumentResponse.from(document))
    }
}
