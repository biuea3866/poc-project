package com.biuea.springai.config

import com.biuea.springai.tool.CatalogTools
import com.biuea.springai.tool.ExtractionTool
import com.biuea.springai.tool.ImageGenerationTool
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * MCP 서버 — `@Tool` 메서드를 Model Context Protocol 도구로 외부 LLM 호스트에 노출.
 *
 * `MethodToolCallbackProvider` 가 `toolObjects(...)` 로 받은 빈들의 `@Tool` 메서드를
 * 리플렉션으로 수집해 `ToolCallback` 으로 변환한다. spring-ai-starter-mcp-server-webmvc 가
 * 이 빈을 자동 찾아 `/sse` + `/mcp/message` SSE 엔드포인트로 노출.
 *
 * 노출되는 도구:
 *   - **CatalogTools** (조회 7 + 쓰기 4) — 의류 도메인
 *   - **ExtractionTool** (`BeanOutputConverter` 시연) — 자유 텍스트 → ExtractedProduct
 *   - **ImageGenerationTool** (`ImageModel` 시연) — DALL-E 이미지 생성
 *
 * → 외부 LLM 호스트는 우리 인앱 ChatClient 와 동일한 도구 집합을 사용 가능.
 */
@Configuration
class McpServerConfig {

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
