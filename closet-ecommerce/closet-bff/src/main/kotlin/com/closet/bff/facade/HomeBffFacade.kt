package com.closet.bff.facade

import com.closet.bff.client.ProductServiceClient
import com.closet.bff.dto.HomeBffResponse
import com.closet.bff.dto.ProductSearchParams
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class HomeBffFacade(
    private val productClient: ProductServiceClient,
) {
    fun getHome(): HomeBffResponse {
        val rankingsMono = productClient.getProducts(ProductSearchParams(page = 0, size = 10, sort = "popular"))
        val newArrivalsMono = productClient.getProducts(ProductSearchParams(page = 0, size = 10, sort = "newest"))

        val result = Mono.zip(rankingsMono, newArrivalsMono).block()!!

        return HomeBffResponse(
            banners = null, // Phase 3
            rankings = result.t1.content,
            newArrivals = result.t2.content,
            exhibitions = null, // Phase 3
        )
    }
}
