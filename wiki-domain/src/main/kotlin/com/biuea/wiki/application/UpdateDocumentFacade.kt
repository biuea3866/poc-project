package com.biuea.wiki.application

import com.biuea.wiki.domain.document.entity.DocumentRevision
import com.biuea.wiki.infrastructure.document.DocumentRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UpdateDocumentFacade(
    private val documentRepository: DocumentRepository,
) {
    @Transactional
    fun update(input: UpdateDocumentInput): UpdateDocumentOutput {
        val document = documentRepository.findByIdAndStatusNot(input.documentId)
            ?: throw IllegalArgumentException("문서를 찾을 수 없습니다. id=${input.documentId}")

        check(document.createdBy == input.userId) { "문서 수정 권한이 없습니다." }
        check(!document.isDeleted()) { "삭제된 문서는 수정할 수 없습니다." }

        document.title = input.title
        document.content = input.content
        document.updatedBy = input.userId

        val revision = DocumentRevision.create(document)
        document.addRevision(revision)
        documentRepository.save(document)

        return UpdateDocumentOutput(
            documentId = document.id,
            revisionId = document.revisions.last().id,
        )
    }
}

data class UpdateDocumentInput(
    val documentId: Long,
    val userId: Long,
    val title: String,
    val content: String?,
)

data class UpdateDocumentOutput(
    val documentId: Long,
    val revisionId: Long,
)
