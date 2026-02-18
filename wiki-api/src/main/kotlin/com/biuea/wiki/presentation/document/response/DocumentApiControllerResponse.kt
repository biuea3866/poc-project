package com.biuea.wiki.presentation.document.response

import com.biuea.wiki.application.AiLogItem
import com.biuea.wiki.application.AiStatusOutput
import com.biuea.wiki.application.DocumentDetailOutput
import com.biuea.wiki.application.DocumentListItem
import com.biuea.wiki.application.DocumentRevisionItem
import com.biuea.wiki.application.PublishDocumentOutput
import com.biuea.wiki.application.RestoreDocumentOutput
import com.biuea.wiki.application.SaveDocumentOutput
import com.biuea.wiki.application.SearchDocumentItem
import com.biuea.wiki.application.TagItem
import com.biuea.wiki.application.UpdateDocumentOutput
import com.biuea.wiki.domain.ai.AgentLogStatus
import com.biuea.wiki.domain.ai.AgentType
import com.biuea.wiki.domain.document.entity.AiStatus
import com.biuea.wiki.domain.document.entity.DocumentStatus
import com.biuea.wiki.domain.tag.entity.TagConstant
import java.time.ZonedDateTime

data class CreateDocumentResponse(
    val id: Long,
    val title: String,
    val status: DocumentStatus,
    val aiStatus: AiStatus,
    val revisionId: Long,
) {
    companion object {
        fun from(output: SaveDocumentOutput) = CreateDocumentResponse(
            id = output.id,
            title = output.title,
            status = output.status,
            aiStatus = output.aiStatus,
            revisionId = output.revisionId,
        )
    }
}

data class PublishDocumentResponse(
    val documentId: Long,
    val revisionId: Long,
) {
    companion object {
        fun from(output: PublishDocumentOutput) = PublishDocumentResponse(
            documentId = output.documentId,
            revisionId = output.revisionId,
        )
    }
}

data class UpdateDocumentResponse(
    val documentId: Long,
    val revisionId: Long,
) {
    companion object {
        fun from(output: UpdateDocumentOutput) = UpdateDocumentResponse(
            documentId = output.documentId,
            revisionId = output.revisionId,
        )
    }
}

data class RestoreDocumentResponse(
    val documentId: Long,
    val restoredToRoot: Boolean,
) {
    companion object {
        fun from(output: RestoreDocumentOutput) = RestoreDocumentResponse(
            documentId = output.documentId,
            restoredToRoot = output.restoredToRoot,
        )
    }
}

data class DocumentDetailResponse(
    val id: Long,
    val title: String,
    val content: String?,
    val status: DocumentStatus,
    val aiStatus: AiStatus,
    val parentId: Long?,
    val summary: String?,
    val tags: List<TagResponse>,
    val createdBy: Long,
    val revisionId: Long?,
) {
    companion object {
        fun from(output: DocumentDetailOutput) = DocumentDetailResponse(
            id = output.id,
            title = output.title,
            content = output.content,
            status = output.status,
            aiStatus = output.aiStatus,
            parentId = output.parentId,
            summary = output.summary,
            tags = output.tags.map { TagResponse.from(it) },
            createdBy = output.createdBy,
            revisionId = output.revisionId,
        )
    }
}

data class DocumentListResponse(
    val id: Long,
    val title: String,
    val status: DocumentStatus,
    val aiStatus: AiStatus,
    val parentId: Long?,
    val childCount: Int,
) {
    companion object {
        fun from(item: DocumentListItem) = DocumentListResponse(
            id = item.id,
            title = item.title,
            status = item.status,
            aiStatus = item.aiStatus,
            parentId = item.parentId,
            childCount = item.childCount,
        )
    }
}

data class TagResponse(
    val id: Long,
    val name: String,
    val tagConstant: TagConstant,
) {
    companion object {
        fun from(item: TagItem) = TagResponse(id = item.id, name = item.name, tagConstant = item.tagConstant)
        fun from(item: DocumentDetailOutput.TagItem) = TagResponse(id = item.id, name = item.name, tagConstant = item.tagConstant)
    }
}

data class AiStatusResponse(
    val documentId: Long,
    val aiStatus: AiStatus,
) {
    companion object {
        fun from(output: AiStatusOutput) = AiStatusResponse(
            documentId = output.documentId,
            aiStatus = output.aiStatus,
        )
    }
}

data class DocumentRevisionResponse(
    val id: Long,
    val title: String,
    val content: String?,
    val createdBy: Long,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(item: DocumentRevisionItem) = DocumentRevisionResponse(
            id = item.id,
            title = item.data.title,
            content = item.data.content,
            createdBy = item.createdBy,
            createdAt = item.createdAt,
        )
    }
}

data class SearchDocumentResponse(
    val id: Long,
    val title: String,
    val content: String?,
    val status: DocumentStatus,
    val aiStatus: AiStatus,
    val parentId: Long?,
) {
    companion object {
        fun from(item: SearchDocumentItem) = SearchDocumentResponse(
            id = item.id,
            title = item.title,
            content = item.content,
            status = item.status,
            aiStatus = item.aiStatus,
            parentId = item.parentId,
        )
    }
}

data class AiLogResponse(
    val id: Long,
    val agentType: AgentType,
    val status: AgentLogStatus,
    val actionDetail: String,
    val referenceData: String?,
    val documentId: Long,
    val documentRevisionId: Long,
    val executorId: Long,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(item: AiLogItem) = AiLogResponse(
            id = item.id,
            agentType = item.agentType,
            status = item.status,
            actionDetail = item.actionDetail,
            referenceData = item.referenceData,
            documentId = item.documentId,
            documentRevisionId = item.documentRevisionId,
            executorId = item.executorId,
            createdAt = item.createdAt,
        )
    }
}
