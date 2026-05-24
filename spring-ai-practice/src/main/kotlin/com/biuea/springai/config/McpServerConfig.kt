package com.biuea.springai.config

import com.biuea.springai.tool.CatalogTools
import com.biuea.springai.tool.ExtractionTool
import com.biuea.springai.tool.ImageGenerationTool
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * MCP 서버 — `@Tool` / `@McpTool` 메서드를 Model Context Protocol 도구로 외부 LLM 호스트에 노출.
 *
 * 1) `MethodToolCallbackProvider` 가 `@Tool` 메서드를 리플렉션으로 수집해 `ToolCallback` 으로 변환
 * 2) spring-ai-starter-mcp-server-webmvc 가 이 빈을 자동 찾아 `/sse` + `/mcp/message` 로 노출
 *
 * 스코프 검사는 Spring AOP (`ToolGuardAspect`) 가 `@RequireScope` 어노테이션을 가로채서 처리.
 * `MethodToolCallback` 가 `toolObject` (CGLIB 프록시 빈) 의 메서드를 `Method.invoke()` 로 호출하면
 * 프록시가 자동으로 advice 를 트리거한다.
 */
@Configuration
class McpServerConfig {

    /**
     * `@Tool` 어노테이션 메서드만 수집한다. `@McpTool` (McpAnnotationDemo) 은
     * `spring-ai-mcp-annotations` autoconfig 가 별도로 자동 발견해 MCP 서버에 등록한다.
     */
    @Bean
    fun toolCallbackProvider(
        catalogTools: CatalogTools,
        extractionTool: ExtractionTool,
        imageGenerationTool: ImageGenerationTool,
    ): ToolCallbackProvider =
        MethodToolCallbackProvider.builder()
            .toolObjects(catalogTools, extractionTool, imageGenerationTool)
            .build()
}
