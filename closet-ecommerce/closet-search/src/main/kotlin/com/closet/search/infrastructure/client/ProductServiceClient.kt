package com.closet.search.infrastructure.client

import com.closet.common.response.ApiResponse
import com.closet.search.application.dto.ProductServicePageResponse
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

private val logger = KotlinLogging.logger {}

@Component
class ProductServiceClient(
    private val restTemplate: RestTemplate,
    @Value("\${closet.product-service.url:http://localhost:8082}") private val productServiceUrl: String,
) {

    fun fetchAllProducts(page: Int, size: Int): ProductServicePageResponse? {
        val url = "$productServiceUrl/api/v1/products?page=$page&size=$size"
        logger.info { "product-service 호출: $url" }

        return try {
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductServicePageResponse>>() {}
            val response = restTemplate.exchange(url, HttpMethod.GET, null, responseType)
            response.body?.data
        } catch (e: Exception) {
            logger.error(e) { "product-service 호출 실패: $url" }
            null
        }
    }
}
