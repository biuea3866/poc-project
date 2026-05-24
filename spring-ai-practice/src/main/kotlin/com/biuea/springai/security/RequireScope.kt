package com.biuea.springai.security

/**
 * 도구 메서드에 필요한 OAuth 스코프를 선언하는 어노테이션.
 *
 * 사용:
 * ```
 * @Tool(description = "...")
 * @RequireScope("catalog:read")
 * fun searchProducts(...) { ... }
 * ```
 *
 * `GuardedToolCallback` 데코레이터가 도구 호출 직전에 SecurityContext 의 권한 목록을 확인하고
 * 부족하면 `AccessDeniedException` 을 던진다. `@Tool` 또는 `@McpTool` 어노테이션과 함께
 * 사용되며, 어노테이션을 붙이지 않은 도구는 스코프 검사 없이 통과한다.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class RequireScope(val value: String)
