package com.closet.search.infrastructure.client

import com.closet.search.application.dto.ProductServicePageResponse
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

private val logger = KotlinLogging.logger {}

/**
 * closet-product 서비스 REST 클라이언트.
 * 벌크 인덱싱 시 상품 목록을 페이지네이션으로 조회한다.
 */
@Component
class ProductServiceClient(
    private val restTemplate: RestTemplate,
    @Value("\${product-service.base-url:http://localhost:8082}")
    private val baseUrl: String,
) {
    /**
     * 상품 목록 조회 (페이지네이션).
     *
     * @param page 페이지 번호 (0-based)
     * @param size 페이지 크기
     * @return 상품 목록 페이지 응답
     */
    fun fetchAllProducts(
        page: Int,
        size: Int,
    ): ProductServicePageResponse {
        val url = "$baseUrl/api/v1/products?page=$page&size=$size"

        logger.debug { "상품 서비스 호출: $url" }

        val responseType = object : ParameterizedTypeReference<ApiWrapper<ProductServicePageResponse>>() {}
        val response = restTemplate.exchange(url, HttpMethod.GET, null, responseType)

        return response.body?.data
            ?: throw RuntimeException("상품 서비스 응답이 비어 있습니다: page=$page, size=$size")
    }

    /**
     * closet-product 서비스 API 응답 래퍼.
     */
    data class ApiWrapper<T>(
        val success: Boolean,
        val data: T?,
        val error: Any?,
    )
}
