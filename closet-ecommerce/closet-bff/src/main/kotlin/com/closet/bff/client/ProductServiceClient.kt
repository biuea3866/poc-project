package com.closet.bff.client

import com.closet.bff.dto.BrandResponse
import com.closet.bff.dto.CategoryResponse
import com.closet.bff.dto.PageResponse
import com.closet.bff.dto.ProductResponse
import com.closet.bff.dto.ProductSearchParams
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class ProductServiceClient(
    @Value("\${service.product.url}") private val baseUrl: String,
) {
    private val webClient: WebClient by lazy {
        WebClient.builder().baseUrl(baseUrl).build()
    }

    fun getProduct(productId: Long): Mono<ProductResponse> {
        return webClient.get()
            .uri("/products/{id}", productId)
            .retrieve()
            .bodyToMono(ProductResponse::class.java)
    }

    fun getProducts(params: ProductSearchParams): Mono<PageResponse<ProductResponse>> {
        return webClient.get()
            .uri { builder ->
                builder.path("/products")
                    .queryParam("page", params.page)
                    .queryParam("size", params.size)
                    .queryParam("sort", params.sort)
                params.categoryId?.let { builder.queryParam("categoryId", it) }
                params.brandId?.let { builder.queryParam("brandId", it) }
                params.minPrice?.let { builder.queryParam("minPrice", it) }
                params.maxPrice?.let { builder.queryParam("maxPrice", it) }
                builder.build()
            }
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<PageResponse<ProductResponse>>() {})
    }

    fun getCategories(): Mono<List<CategoryResponse>> {
        return webClient.get()
            .uri("/categories")
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<List<CategoryResponse>>() {})
    }

    fun getBrands(): Mono<List<BrandResponse>> {
        return webClient.get()
            .uri("/brands")
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<List<BrandResponse>>() {})
    }
}
