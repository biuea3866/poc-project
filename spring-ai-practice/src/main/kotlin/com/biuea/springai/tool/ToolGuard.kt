package com.biuea.springai.tool

import com.biuea.springai.audit.ToolAuditLogger
import com.biuea.springai.security.ScopeGuard
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/**
 * 모든 도구 호출의 공통 래퍼. PoC 학습용으로 다음 책임을 묶는다.
 *
 *   1) 인가: ScopeGuard 로 필요한 스코프 확인 (없으면 403)
 *   2) 감사: 성공/거부/오류 결과를 ToolAuditLogger 에 기록
 *
 * 입력 검증은 도구 메서드 본문에서 ToolInputValidator 로 별도 수행한다.
 */
@Component
class ToolGuard(
    private val scopeGuard: ScopeGuard,
    private val auditLogger: ToolAuditLogger,
) {

    fun <T> invoke(tool: String, scope: String, args: Map<String, Any?>, block: () -> T): T {
        val start = System.nanoTime()
        val subject = currentSubject()
        return try {
            scopeGuard.requireScope(scope)
            val result = block()
            auditLogger.success(subject, tool, args, elapsedMs(start))
            result
        } catch (e: AccessDeniedException) {
            auditLogger.denied(subject, tool, args, e.message ?: "access denied", elapsedMs(start))
            throw e
        } catch (e: IllegalArgumentException) {
            auditLogger.error(subject, tool, args, e.message ?: "invalid argument", elapsedMs(start))
            throw e
        } catch (e: Exception) {
            auditLogger.error(subject, tool, args, e.message ?: e.javaClass.simpleName, elapsedMs(start))
            throw e
        }
    }

    private fun currentSubject(): String =
        SecurityContextHolder.getContext().authentication?.principal?.toString() ?: "anonymous"

    private fun elapsedMs(start: Long): Long = (System.nanoTime() - start) / 1_000_000
}
