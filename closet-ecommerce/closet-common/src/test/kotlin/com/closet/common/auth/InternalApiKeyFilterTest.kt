package com.closet.common.auth

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.PrintWriter
import java.io.StringWriter

class InternalApiKeyFilterTest : BehaviorSpec({

    val validApiKey = "closet-internal-api-secret-key"

    fun createFilter(
        apiKey: String = validApiKey,
        paths: String = "/internal/**",
    ): InternalApiKeyFilter {
        return InternalApiKeyFilter(
            internalApiKey = apiKey,
            internalPaths = paths,
        )
    }

    fun createRequest(
        path: String,
        apiKeyHeader: String? = null,
    ): HttpServletRequest {
        val request = mockk<HttpServletRequest>(relaxed = true)
        every { request.requestURI } returns path
        every { request.getHeader(InternalApiKeyFilter.HEADER_INTERNAL_API_KEY) } returns apiKeyHeader
        every { request.getAttribute(any()) } returns null
        every { request.dispatcherType } returns jakarta.servlet.DispatcherType.REQUEST
        return request
    }

    fun createResponse(): Pair<HttpServletResponse, StringWriter> {
        val response = mockk<HttpServletResponse>(relaxed = true)
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        every { response.writer } returns printWriter
        return Pair(response, stringWriter)
    }

    Given("내부 API 경로에 대한 요청") {

        When("올바른 X-Internal-Api-Key가 포함되면") {
            val filter = createFilter()
            val request = createRequest("/internal/inventories/1/reserve", apiKeyHeader = validApiKey)
            val (response, _) = createResponse()
            val filterChain = mockk<FilterChain>(relaxed = true)

            filter.doFilter(request, response, filterChain)

            Then("필터 체인을 계속 진행한다") {
                verify(exactly = 1) { filterChain.doFilter(request, response) }
            }
        }

        When("X-Internal-Api-Key가 없으면") {
            val filter = createFilter()
            val request = createRequest("/internal/inventories/1/reserve", apiKeyHeader = null)
            val (response, stringWriter) = createResponse()
            val filterChain = mockk<FilterChain>(relaxed = true)

            filter.doFilter(request, response, filterChain)

            Then("401 Unauthorized를 반환한다") {
                verify { response.status = HttpServletResponse.SC_UNAUTHORIZED }
                verify(exactly = 0) { filterChain.doFilter(any(), any()) }
            }
        }

        When("잘못된 X-Internal-Api-Key이면") {
            val filter = createFilter()
            val request = createRequest("/internal/inventories/1/reserve", apiKeyHeader = "wrong-key")
            val (response, _) = createResponse()
            val filterChain = mockk<FilterChain>(relaxed = true)

            filter.doFilter(request, response, filterChain)

            Then("401 Unauthorized를 반환한다") {
                verify { response.status = HttpServletResponse.SC_UNAUTHORIZED }
                verify(exactly = 0) { filterChain.doFilter(any(), any()) }
            }
        }
    }

    Given("내부 API 경로가 아닌 요청") {

        When("일반 API 경로이면") {
            val filter = createFilter()
            val request = createRequest("/api/v1/products", apiKeyHeader = null)
            val (response, _) = createResponse()
            val filterChain = mockk<FilterChain>(relaxed = true)

            filter.doFilter(request, response, filterChain)

            Then("API 키 검증 없이 통과시킨다") {
                verify(exactly = 1) { filterChain.doFilter(request, response) }
            }
        }
    }
})
