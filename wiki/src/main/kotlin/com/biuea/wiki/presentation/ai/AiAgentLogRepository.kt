package com.biuea.wiki.presentation.ai

import com.biuea.wiki.domain.ai.AgentType
import com.biuea.wiki.domain.ai.AiAgentLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface AiAgentLogRepository : JpaRepository<AiAgentLog, Long> {

    fun findByDocumentId(documentId: Long, pageable: Pageable): Page<AiAgentLog>

    fun findByDocumentIdAndAgentType(documentId: Long, agentType: AgentType, pageable: Pageable): Page<AiAgentLog>

    fun findByExecutorId(executorId: Long, pageable: Pageable): Page<AiAgentLog>
}