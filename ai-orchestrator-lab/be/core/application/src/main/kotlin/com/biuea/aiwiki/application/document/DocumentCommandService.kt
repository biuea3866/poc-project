package com.biuea.aiwiki.application.document

import com.biuea.aiwiki.domain.document.model.Document
import com.biuea.aiwiki.domain.document.model.DocumentRevision
import com.biuea.aiwiki.domain.document.port.DocumentRepository
import com.biuea.aiwiki.domain.document.port.DocumentRevisionRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class DocumentCommandService(
    private val documentRepository: DocumentRepository,
    private val documentRevisionRepository: DocumentRevisionRepository,
) {
    fun createDraft(command: CreateDraftCommand): Document {
        val saved = documentRepository.save(
            Document(
                title = command.title,
                content = command.content,
                tags = command.tags,
                createdBy = command.userId,
                updatedBy = command.userId,
            )
        )
        return saved
    }

    fun requestAnalysis(documentId: Long): Document {
        val current = requireNotNull(documentRepository.findById(documentId)) { "문서를 찾을 수 없습니다." }
        current.requestAnalysis()
        val updated = documentRepository.save(current)
        saveRevision(updated)
        return updated
    }

    fun activate(documentId: Long, userId: Long): Document {
        val current = requireNotNull(documentRepository.findById(documentId)) { "문서를 찾을 수 없습니다." }
        current.activate(userId)
        val updated = documentRepository.save(current)
        saveRevision(updated)
        return updated
    }

    fun deleteDocument(documentId: Long, userId: Long): Document {
        val current = requireNotNull(documentRepository.findById(documentId)) { "문서를 찾을 수 없습니다." }
        current.delete(userId)
        val updated = documentRepository.save(current)
        saveRevision(updated)
        return updated
    }

    fun updateDraft(command: UpdateDraftCommand): Document {
        val current = requireNotNull(documentRepository.findById(command.documentId)) { "문서를 찾을 수 없습니다." }
        if (current.updatedAt != command.expectedUpdatedAt) {
            throw OptimisticLockException("문서가 이미 변경되었습니다. 최신 상태를 다시 조회하세요.")
        }
        current.updateDraft(
            title = command.title,
            content = command.content,
            tags = command.tags,
            userId = command.userId,
        )
        val updated = documentRepository.save(current)
        if (updated.status == com.biuea.aiwiki.domain.document.model.DocumentStatus.ACTIVE) {
            saveRevision(updated)
        }
        return updated
    }

    private fun saveRevision(updated: Document) {
        documentRevisionRepository.save(
            DocumentRevision(
                documentId = requireNotNull(updated.id),
                title = updated.title,
                content = updated.content,
                status = updated.status,
                createdBy = updated.updatedBy,
            )
        )
    }
}

data class CreateDraftCommand(
    val title: String,
    val content: String,
    val tags: List<String>,
    val userId: Long,
)

data class UpdateDraftCommand(
    val documentId: Long,
    val title: String,
    val content: String,
    val tags: List<String>,
    val userId: Long,
    val expectedUpdatedAt: Instant,
)

class OptimisticLockException(message: String) : RuntimeException(message)
