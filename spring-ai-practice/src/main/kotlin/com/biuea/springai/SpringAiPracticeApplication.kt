package com.biuea.springai

import io.micrometer.context.ContextRegistry
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import reactor.core.publisher.Hooks

@SpringBootApplication
class SpringAiPracticeApplication

fun main(args: Array<String>) {
    // MCP server-webmvc 는 메시지 dispatch 를 Reactor boundedElastic worker 에서 수행한다.
    // servlet thread 의 ThreadLocal SecurityContext 를 worker thread 로 전파하기 위해
    // Reactor automatic context propagation 을 활성화하고, SecurityContext ThreadLocalAccessor 를
    // ContextRegistry 에 등록한다.
    ContextRegistry.getInstance().registerThreadLocalAccessor(
        "security.context",
        { SecurityContextHolder.getContext() },
        { value -> SecurityContextHolder.setContext(value as SecurityContext) },
        { SecurityContextHolder.clearContext() },
    )
    Hooks.enableAutomaticContextPropagation()
    runApplication<SpringAiPracticeApplication>(*args)
}
