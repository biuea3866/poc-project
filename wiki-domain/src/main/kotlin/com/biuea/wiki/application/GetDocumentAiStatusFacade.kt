package com.biuea.wiki.application

import com.biuea.wiki.domain.document.entity.AiStatus
import com.biuea.wiki.infrastructure.document.DocumentRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetDocumentAiStatusFacade(
    private val documentRepository: DocumentRepository,
) {
    @Transactional(readOnly = true)
    fun getAiStatus(documentId: Long): AiStatusOutput {
        val document = documentRepository.findByIdAndStatusNot(documentId)
            ?: throw IllegalArgumentException("문서를 찾을 수 없습니다. id=$documentId")

        return AiStatusOutput(
            documentId = document.id,
            aiStatus = document.aiStatus,
        )
    }
}

data class AiStatusOutput(
    val documentId: Long,
    val aiStatus: AiStatus,
)
