package com.biuea.wiki.presentation.ai

import com.biuea.wiki.application.GetAiLogsFacade
import com.biuea.wiki.application.GetAiLogsInput
import com.biuea.wiki.domain.ai.AgentType
import com.biuea.wiki.presentation.document.response.AiLogResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/ai")
class AiApiController(
    private val getAiLogsFacade: GetAiLogsFacade,
) {
    // GET /api/v1/ai/logs — 에이전트 로그 조회
    @GetMapping("/logs")
    fun getLogs(
        @RequestParam(required = false) documentId: Long?,
        @RequestParam(required = false) agentType: AgentType?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<AiLogResponse> {
        return getAiLogsFacade.getLogs(
            GetAiLogsInput(documentId = documentId, agentType = agentType, pageable = pageable)
        ).map { AiLogResponse.from(it) }
    }
}
