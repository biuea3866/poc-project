package com.biuea.wiki.worker.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service

/**
 * AI API 호출 횟수를 Prometheus 메트릭으로 기록하는 서비스 (wiki-worker 모듈).
 * wiki-worker는 실제 LLM/Embedding 호출을 수행하므로 여기서 카운터를 증가시킨다.
 *
 * 메트릭:
 * - wiki_ai_anthropic_calls_total: Anthropic API (요약/태깅) 호출 횟수
 * - wiki_ai_openai_calls_total: OpenAI Embedding API 호출 횟수
 */
@Service
class AiMetricsService(private val meterRegistry: MeterRegistry) {

    private val anthropicCallCounter: Counter = Counter.builder("wiki.ai.anthropic.calls")
        .description("Anthropic API call count (summary + tagging)")
        .tag("service", "wiki-worker")
        .register(meterRegistry)

    private val openaiCallCounter: Counter = Counter.builder("wiki.ai.openai.calls")
        .description("OpenAI Embedding API call count")
        .tag("service", "wiki-worker")
        .register(meterRegistry)

    fun recordAnthropicCall() = anthropicCallCounter.increment()

    fun recordOpenAiCall() = openaiCallCounter.increment()
}
