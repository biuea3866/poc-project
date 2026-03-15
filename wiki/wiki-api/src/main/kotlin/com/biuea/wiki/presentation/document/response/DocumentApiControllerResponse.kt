package com.biuea.wiki.presentation.document.response

import com.biuea.wiki.domain.ai.AgentLogStatus
import com.biuea.wiki.domain.ai.AgentType
import com.biuea.wiki.domain.ai.AiAgentLog
import com.biuea.wiki.domain.document.entity.Document
import com.biuea.wiki.domain.document.entity.DocumentRevision
import com.biuea.wiki.domain.document.entity.DocumentStatus
import com.biuea.wiki.domain.tag.entity.Tag
import com.biuea.wiki.domain.tag.entity.TagConstant
import com.biuea.wiki.domain.tag.entity.TagDocumentMapping
import java.time.LocalDateTime
import java.time.ZonedDateTime

data class DocumentResponse(
    val id: Long,
    val title: String,
    val content: String?,
    val status: DocumentStatus,
    val parentId: Long?,
    val createdBy: Long,
    val updatedBy: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(document: Document): DocumentResponse {
            return DocumentResponse(
                id = document.id,
                title = document.title,
                content = document.content,
                status = document.status,
                parentId = document.parent?.id,
                createdBy = document.createdBy,
                updatedBy = document.updatedBy,
                createdAt = document.createdAt,
                updatedAt = document.updatedAt,
            )
        }
    }
}

data class DocumentListResponse(
    val documents: List<DocumentResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class RevisionListResponse(
    val revisions: List<RevisionResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class RevisionResponse(
    val id: Long,
    val title: String,
    val content: String?,
    val status: DocumentStatus,
    val createdBy: Long,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(revision: DocumentRevision): RevisionResponse {
            return RevisionResponse(
                id = revision.id,
                title = revision.data.title,
                content = revision.data.content,
                status = revision.data.status,
                createdBy = revision.createdBy,
                createdAt = revision.createdAt,
            )
        }
    }
}

data class TagResponse(
    val id: Long,
    val name: String,
    val tagConstant: TagConstant,
) {
    companion object {
        fun from(mapping: TagDocumentMapping): TagResponse {
            return TagResponse(
                id = mapping.tag.id,
                name = mapping.tag.name,
                tagConstant = mapping.tag.tagType.tagConstant,
            )
        }
    }
}

data class AiStatusResponse(
    val documentId: Long,
    val agents: List<AgentStatusEntry>,
) {
    data class AgentStatusEntry(
        val agentType: AgentType,
        val status: AgentLogStatus,
        val actionDetail: String,
        val createdAt: ZonedDateTime,
    )

    companion object {
        fun from(documentId: Long, logs: List<AiAgentLog>): AiStatusResponse {
            return AiStatusResponse(
                documentId = documentId,
                agents = logs.map {
                    AgentStatusEntry(
                        agentType = it.agentType,
                        status = it.status,
                        actionDetail = it.actionDetail,
                        createdAt = it.createdAt,
                    )
                }
            )
        }
    }
}
