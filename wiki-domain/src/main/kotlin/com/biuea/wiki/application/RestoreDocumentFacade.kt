package com.biuea.wiki.application

import com.biuea.wiki.domain.document.entity.DocumentStatus
import com.biuea.wiki.infrastructure.document.DocumentRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RestoreDocumentFacade(
    private val documentRepository: DocumentRepository,
) {
    @Transactional
    fun restore(input: RestoreDocumentInput): RestoreDocumentOutput {
        val document = documentRepository.findById(input.documentId).orElseThrow {
            IllegalArgumentException("문서를 찾을 수 없습니다. id=${input.documentId}")
        }

        check(document.isDeleted()) { "삭제된 문서만 복구할 수 있습니다." }
        check(document.createdBy == input.userId) { "문서 복구 권한이 없습니다." }

        // 부모가 삭제된 경우 루트 레벨로 복구
        val restoredParent = document.parent?.takeIf { it.status == DocumentStatus.ACTIVE }
        document.restore(restoredParent)
        documentRepository.save(document)

        return RestoreDocumentOutput(
            documentId = document.id,
            restoredToRoot = restoredParent == null && document.parent != null,
        )
    }
}

data class RestoreDocumentInput(
    val documentId: Long,
    val userId: Long,
)

data class RestoreDocumentOutput(
    val documentId: Long,
    val restoredToRoot: Boolean,
)
