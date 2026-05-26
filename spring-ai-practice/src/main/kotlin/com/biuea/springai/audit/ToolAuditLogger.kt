package com.biuea.springai.audit

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * 도구 호출 감사 로그 작성기.
 *
 * AOP `@Around` 가 MCP 도구 리플렉션 호출에서 미동작하므로
 * 도구 진입부에서 ToolGuard 가 명시 호출한다.
 */
@Component
class ToolAuditLogger(private val objectMapper: ObjectMapper) {

    private val log = LoggerFactory.getLogger("audit.tool")

    fun success(subject: String, tool: String, args: Map<String, Any?>, latencyMs: Long) {
        log.info("{}", line(subject, tool, args, "success", null, latencyMs))
    }

    fun denied(subject: String, tool: String, args: Map<String, Any?>, reason: String, latencyMs: Long) {
        log.warn("{}", line(subject, tool, args, "denied", reason, latencyMs))
    }

    fun error(subject: String, tool: String, args: Map<String, Any?>, reason: String, latencyMs: Long) {
        log.warn("{}", line(subject, tool, args, "error", reason, latencyMs))
    }

    private fun line(
        subject: String,
        tool: String,
        args: Map<String, Any?>,
        outcome: String,
        reason: String?,
        latencyMs: Long,
    ): String {
        val payload = mutableMapOf<String, Any?>(
            "timestamp" to Instant.now(),
            "subject" to subject,
            "tool" to tool,
            "args" to args,
            "outcome" to outcome,
            "latencyMs" to latencyMs,
        )
        if (reason != null) payload["reason"] = reason
        return objectMapper.writeValueAsString(payload)
    }
}
