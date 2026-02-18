package com.biuea.wiki.application

import com.biuea.wiki.domain.document.entity.Document
import com.biuea.wiki.infrastructure.document.DocumentRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DeleteDocumentFacade(
    private val documentRepository: DocumentRepository,
) {
    @Transactional
    fun delete(input: DeleteDocumentInput) {
        val document = documentRepository.findByIdAndStatusNot(input.documentId)
            ?: throw IllegalArgumentException("문서를 찾을 수 없습니다. id=${input.documentId}")

        check(document.createdBy == input.userId) { "문서 삭제 권한이 없습니다." }

        // 재귀적으로 하위 문서도 소프트 삭제 (cascade)
        softDeleteRecursive(document)
        documentRepository.saveAll(collectAll(document))
    }

    private fun softDeleteRecursive(document: Document) {
        document.delete()
        document.children
            .filter { !it.isDeleted() }
            .forEach { softDeleteRecursive(it) }
    }

    private fun collectAll(document: Document): List<Document> {
        val result = mutableListOf(document)
        document.children.forEach { result.addAll(collectAll(it)) }
        return result
    }
}

data class DeleteDocumentInput(
    val documentId: Long,
    val userId: Long,
)
