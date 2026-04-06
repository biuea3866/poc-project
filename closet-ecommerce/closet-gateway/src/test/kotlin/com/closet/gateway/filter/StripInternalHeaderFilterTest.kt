package com.closet.gateway.filter

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import reactor.core.publisher.Mono

/**
 * StripInternalHeaderFilter 단위 테스트.
 *
 * Gateway 모듈은 Kotest가 아닌 JUnit5 + Spring Mock 기반으로 테스트한다.
 * (WebFlux 모듈이라 Kotest Spring Extension이 적용되지 않음)
 */
class StripInternalHeaderFilterTest {
    private val filter = StripInternalHeaderFilter()

    @Test
    fun `외부 요청에 X-Internal-Api-Key가 포함되면 제거한다`() {
        val request =
            MockServerHttpRequest.get("/api/v1/inventories/1/reserve")
                .header(StripInternalHeaderFilter.HEADER_INTERNAL_API_KEY, "malicious-key")
                .build()
        val exchange = MockServerWebExchange.from(request)

        var chainCalled = false
        var strippedExchange: org.springframework.web.server.ServerWebExchange? = null

        val chain =
            GatewayFilterChain { ex ->
                chainCalled = true
                strippedExchange = ex
                Mono.empty()
            }

        filter.filter(exchange, chain).block()

        assertTrue(chainCalled, "필터 체인이 호출되어야 한다")
        assertFalse(
            strippedExchange!!.request.headers.containsKey(StripInternalHeaderFilter.HEADER_INTERNAL_API_KEY),
            "X-Internal-Api-Key 헤더가 제거되어야 한다",
        )
    }

    @Test
    fun `X-Internal-Api-Key가 없는 요청은 그대로 통과한다`() {
        val request =
            MockServerHttpRequest.get("/api/v1/products")
                .build()
        val exchange = MockServerWebExchange.from(request)

        var chainCalled = false
        val chain =
            GatewayFilterChain {
                chainCalled = true
                Mono.empty()
            }

        filter.filter(exchange, chain).block()

        assertTrue(chainCalled, "필터 체인이 호출되어야 한다")
    }

    @Test
    fun `필터 우선순위가 최상위(-10)이다`() {
        assertTrue(filter.order == -10, "StripInternalHeaderFilter의 order는 -10이어야 한다")
    }
}
