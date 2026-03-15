package com.biuea.aiwiki.application.document

import com.biuea.aiwiki.domain.document.model.Document
import com.biuea.aiwiki.domain.document.model.DocumentStatus
import com.biuea.aiwiki.domain.document.port.DocumentRepository
import org.springframework.stereotype.Service

@Service
class DocumentQueryService(
    private val documentRepository: DocumentRepository,
) {
    fun getDocument(documentId: Long): Document =
        requireNotNull(documentRepository.findById(documentId)) { "문서를 찾을 수 없습니다." }

    fun searchActiveDocuments(userId: Long, query: String): List<Document> =
        documentRepository.searchActiveOwnedByUser(query, userId)
            .filter { it.status == DocumentStatus.ACTIVE }
}
