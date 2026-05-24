package com.biuea.springai.security

import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/**
 * SecurityContext 기반 스코프 검증기.
 *
 * 호출 시점에 SecurityContextHolder 의 권한 목록에서 `SCOPE_<scope>` 를 직접 찾는다.
 * Spring AOP 프록시에 의존하지 않으므로 MCP `MethodToolCallbackProvider` 가
 * 리플렉션으로 도구 메서드를 호출해도 정상 동작한다.
 */
@Component
class ScopeGuard {

    fun requireScope(scope: String): String {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw AccessDeniedException("authentication required")
        val subject = authentication.principal?.toString() ?: "anonymous"
        val hasScope = authentication.authorities.any { it.authority == "SCOPE_$scope" }
        if (!hasScope) {
            throw AccessDeniedException("missing required scope: $scope")
        }
        return subject
    }
}
