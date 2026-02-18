package com.biuea.wiki.application

import com.biuea.wiki.domain.ai.AgentLogStatus
import com.biuea.wiki.domain.ai.AgentType
import com.biuea.wiki.infrastructure.ai.AiAgentLogRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class GetAiLogsFacade(
    private val aiAgentLogRepository: AiAgentLogRepository,
) {
    @Transactional(readOnly = true)
    fun getLogs(input: GetAiLogsInput): Page<AiLogItem> {
        val logs = when {
            input.documentId != null && input.agentType != null ->
                aiAgentLogRepository.findByDocumentIdAndAgentType(
                    documentId = input.documentId,
                    agentType = input.agentType,
                    pageable = input.pageable,
                )
            input.documentId != null ->
                aiAgentLogRepository.findByDocumentId(input.documentId, input.pageable)
            else ->
                aiAgentLogRepository.findAll(input.pageable)
        }

        return logs.map { log ->
            AiLogItem(
                id = log.id,
                agentType = log.agentType,
                status = log.status,
                actionDetail = log.actionDetail,
                referenceData = log.referenceData,
                documentId = log.documentId.id,
                documentRevisionId = log.documentRevisionId.id,
                executorId = log.executorId,
                createdAt = log.createdAt,
            )
        }
    }
}

data class GetAiLogsInput(
    val documentId: Long?,
    val agentType: AgentType?,
    val pageable: Pageable,
)

data class AiLogItem(
    val id: Long,
    val agentType: AgentType,
    val status: AgentLogStatus,
    val actionDetail: String,
    val referenceData: String?,
    val documentId: Long,
    val documentRevisionId: Long,
    val executorId: Long,
    val createdAt: ZonedDateTime,
)
