package com.biuea.springai.config

import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport
import io.modelcontextprotocol.spec.McpSchema
import org.slf4j.LoggerFactory
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider
import org.springframework.ai.tool.ToolCallback
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 우리 앱이 *다른* MCP 서버의 **클라이언트**가 되어 외부 도구를 가져오는 구성.
 *
 * Spring AI 컴포넌트 매핑:
 *
 * - **`McpClient`** — io.modelcontextprotocol.sdk 가 제공하는 MCP 클라이언트.
 *   transport(SSE/STDIO) + clientInfo + capabilities 를 묶어 `McpSyncClient` 를 빌드한다.
 *
 * - **`McpSyncClient.initialize()`** — `initialize` 핸드셰이크. 외부 서버의 메타데이터를 가져옴.
 *
 * - **`SyncMcpToolCallbackProvider`** — Spring AI 의 어댑터. MCP 도구 1개당 1개의
 *   `ToolCallback` 으로 변환해 `ChatClient.toolCallbacks(...)` 에 넣을 수 있게 한다.
 *   외부 서버 도구도 우리 `@Tool` 처럼 LLM 이 동적으로 호출하게 된다.
 *
 * 외부 서버가 꺼져 있어도 앱이 정상 부팅하도록 `enabled=false` 시에는 빈을 만들지 않는다.
 * 외부 서버 미실행이면 `enabled=true` 라도 빈 리스트를 반환해 graceful fallback.
 */
@Configuration
@EnableConfigurationProperties(ExternalMcpProperties::class)
class McpClientConfig {

    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun externalMcpClient(properties: ExternalMcpProperties): McpSyncClient? {
        if (!properties.enabled) {
            log.info("External MCP client 비활성 (app.external-mcp.enabled=false)")
            return null
        }
        return try {
            val transport = HttpClientSseClientTransport.builder(properties.serverUrl).build()
            val client = McpClient.sync(transport)
                .clientInfo(McpSchema.Implementation("clothing-ecommerce-app", "0.0.1"))
                .build()
            client.initialize()
            log.info("External MCP server 연결 완료: ${properties.serverUrl}")
            client
        } catch (e: Exception) {
            log.warn("External MCP server 연결 실패 (graceful skip): ${properties.serverUrl} — ${e.message}")
            null
        }
    }

    @Bean
    fun externalToolCallbacks(client: McpSyncClient?): List<ToolCallback> {
        if (client == null) return emptyList()
        return try {
            // 1.1.x 권장 패턴: deprecated 생성자 대신 builder() 사용.
            // McpToolFilter, McpToolNamePrefixGenerator, ToolContextToMcpMetaConverter 같은
            // 옵션을 함께 구성할 수 있는 표준 진입점이다.
            val callbacks = SyncMcpToolCallbackProvider.builder()
                .mcpClients(client)
                .build()
                .toolCallbacks
                .toList()
            log.info("External MCP server 도구 ${callbacks.size}개 노출됨")
            callbacks
        } catch (e: Exception) {
            log.warn("External MCP toolCallbacks 조회 실패: ${e.message}")
            emptyList()
        }
    }
}

@ConfigurationProperties(prefix = "app.external-mcp")
data class ExternalMcpProperties(
    val enabled: Boolean = false,
    val serverUrl: String = "http://localhost:9090",
)
