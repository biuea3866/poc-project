package com.biuea.wiki.metrics

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("AiMetricsService 단위 테스트")
class AiMetricsServiceTest {

    private lateinit var meterRegistry: SimpleMeterRegistry
    private lateinit var aiMetricsService: AiMetricsService

    @BeforeEach
    fun setUp() {
        meterRegistry = SimpleMeterRegistry()
        aiMetricsService = AiMetricsService(meterRegistry)
    }

    @Test
    @DisplayName("recordAnthropicCall 호출 시 wiki_ai_anthropic_calls_total 카운터가 증가한다")
    fun `recordAnthropicCall increments anthropic counter`() {
        // when
        aiMetricsService.recordAnthropicCall()
        aiMetricsService.recordAnthropicCall()

        // then
        val counter = meterRegistry.find("wiki.ai.anthropic.calls").counter()
        assertEquals(2.0, counter?.count())
    }

    @Test
    @DisplayName("recordOpenAiCall 호출 시 wiki_ai_openai_calls_total 카운터가 증가한다")
    fun `recordOpenAiCall increments openai counter`() {
        // when
        aiMetricsService.recordOpenAiCall()

        // then
        val counter = meterRegistry.find("wiki.ai.openai.calls").counter()
        assertEquals(1.0, counter?.count())
    }

    @Test
    @DisplayName("Anthropic과 OpenAI 카운터는 독립적으로 증가한다")
    fun `anthropic and openai counters are independent`() {
        // when
        aiMetricsService.recordAnthropicCall()
        aiMetricsService.recordAnthropicCall()
        aiMetricsService.recordAnthropicCall()
        aiMetricsService.recordOpenAiCall()

        // then
        val anthropicCounter = meterRegistry.find("wiki.ai.anthropic.calls").counter()
        val openaiCounter = meterRegistry.find("wiki.ai.openai.calls").counter()

        assertEquals(3.0, anthropicCounter?.count())
        assertEquals(1.0, openaiCounter?.count())
    }

    @Test
    @DisplayName("초기 상태에서 카운터 값은 0이다")
    fun `initial counter value is zero`() {
        // then
        val anthropicCounter = meterRegistry.find("wiki.ai.anthropic.calls").counter()
        val openaiCounter = meterRegistry.find("wiki.ai.openai.calls").counter()

        assertEquals(0.0, anthropicCounter?.count())
        assertEquals(0.0, openaiCounter?.count())
    }
}
