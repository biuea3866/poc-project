package com.biuea.springai.tool

import org.springaicommunity.mcp.annotation.McpTool
import org.springaicommunity.mcp.annotation.McpToolParam
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Spring AI 1.1.x 의 **`@McpTool`** 어노테이션 시연.
 *
 * `@Tool` (org.springframework.ai.tool.annotation.Tool) 과의 차이:
 *
 * | 항목 | `@Tool` | `@McpTool` |
 * |---|---|---|
 * | 사용 처 | 일반 LLM 도구(인앱 ChatClient + MCP 양쪽) | MCP 프로토콜 전용 |
 * | 위치 | `org.springframework.ai.tool.annotation` (1.0.x~) | `org.springaicommunity.mcp.annotation` (1.1.x~) |
 * | 부가 메타데이터 | 이름·설명·반환 직접화 | + MCP 어노테이션(예: `McpAnnotations`), progress 토큰, schema 옵션 |
 * | Provider | `MethodToolCallbackProvider` | `SyncMcpToolProvider` |
 *
 * 본 PoC 는 이 한 도구만 `@McpTool` 로 노출해 1.1.x 의 새 어노테이션이 정상 동작함을 시연한다.
 * 기존 `@Tool` 어노테이션은 그대로 유지되어 인앱 ChatClient + MCP 서버 양쪽에서 계속 동작한다.
 */
@Component
class McpAnnotationDemo {

    @McpTool(name = "todayTip", description = "월(1~12)을 받아 그 시즌에 어울리는 의류 코디 팁을 한 줄 반환한다. " +
        "month 가 비어있으면 오늘 날짜의 월을 사용한다.")
    fun todayTip(
        @McpToolParam(description = "월(1~12). 비워두면 오늘", required = false)
        month: Int?,
    ): String {
        val m = month?.takeIf { it in 1..12 } ?: LocalDate.now().monthValue
        return when (m) {
            in 3..5 -> "봄($m 월)에는 트렌치코트나 얇은 니트가 잘 어울려요."
            in 6..8 -> "여름($m 월)엔 린넨 셔츠와 와이드 슬랙스 조합이 시원해요."
            in 9..11 -> "가을($m 월)엔 코튼 자켓 + 청바지가 무난해요."
            else -> "겨울($m 월)엔 구스다운 패딩으로 따뜻하게 입으세요."
        }
    }
}
