package com.biuea.springai.security

import com.biuea.springai.audit.ToolAuditLogger
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springaicommunity.mcp.annotation.McpTool
import org.springframework.ai.tool.annotation.Tool
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.lang.reflect.Method

/**
 * `@RequireScope` 어노테이션을 Spring AOP `@Around` advice 로 처리.
 *
 * 동작 흐름:
 * 1. 도구 빈은 `@Component` 이고 `@RequireScope` 메서드를 가지므로 Spring 이 자동으로 CGLIB 프록시 생성
 * 2. spring-ai 의 `MethodToolCallback` 가 빈을 `toolObject` 로 받고 메서드를 `Method.invoke(target, args)` 호출
 *    → 프록시의 메서드가 invoke 되어 advice 가 가로챔
 * 3. advice 가 SecurityContext 검사 + ToolAuditLogger 기록 후 `proceed()` 호출
 *
 * 도구 이름 결정 (`audit.tool` 로그용):
 * - `@Tool.name` / `@McpTool.name` 이 명시되면 그 값, 아니면 메서드명
 */
@Aspect
@Component
class ToolGuardAspect(
    private val auditLogger: ToolAuditLogger,
) {

    /**
     * `@RequireScope` 가 부착된 모든 메서드를 가로챈다.
     * 인자 바인딩: `@annotation(requireScope)` 가 advice 파라미터로 어노테이션 인스턴스를 전달.
     */
    @Around("@annotation(requireScope)")
    fun guard(joinPoint: ProceedingJoinPoint, requireScope: RequireScope): Any? {
        val method = (joinPoint.signature as MethodSignature).method
        val toolName = resolveToolName(method)
        val subject = currentSubject()
        val args = bindArguments(joinPoint, method)
        val start = System.nanoTime()
        return try {
            requireScopeOrThrow(requireScope.value)
            val result = joinPoint.proceed()
            auditLogger.success(subject, toolName, args, elapsedMs(start))
            result
        } catch (e: AccessDeniedException) {
            auditLogger.denied(subject, toolName, args, e.message ?: "access denied", elapsedMs(start))
            throw e
        } catch (e: IllegalArgumentException) {
            auditLogger.error(subject, toolName, args, e.message ?: "invalid argument", elapsedMs(start))
            throw e
        } catch (e: Exception) {
            auditLogger.error(subject, toolName, args, e.message ?: e.javaClass.simpleName, elapsedMs(start))
            throw e
        }
    }

    private fun requireScopeOrThrow(scope: String) {
        val authentication = SecurityContextHolder.getContext().authentication
        val hasScope = authentication.authorities.any { it.authority == "SCOPE_$scope" }
        if (!hasScope) {
            throw AccessDeniedException("missing required scope: $scope")
        }
    }

    private fun currentSubject(): String =
        SecurityContextHolder.getContext().authentication.principal.toString()

    private fun resolveToolName(method: Method): String {
        method.getAnnotation(Tool::class.java)?.takeIf { it.name.isNotBlank() }?.let { return it.name }
        method.getAnnotation(McpTool::class.java)?.takeIf { it.name.isNotBlank() }?.let { return it.name }
        return method.name
    }

    private fun bindArguments(joinPoint: ProceedingJoinPoint, method: Method): Map<String, Any?> {
        val names: Array<String> = method.parameters.map { it.name }.toTypedArray()
        return names.zip(joinPoint.args).toMap()
    }

    private fun elapsedMs(start: Long): Long = (System.nanoTime() - start) / 1_000_000
}
