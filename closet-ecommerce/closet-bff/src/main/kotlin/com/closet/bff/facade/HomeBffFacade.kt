package com.closet.bff.facade

import com.closet.bff.client.ProductServiceClient
import com.closet.bff.dto.HomeBffResponse
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@Service
class HomeBffFacade(
    private val productClient: ProductServiceClient,
) {
    private val executor = Executors.newVirtualThreadPerTaskExecutor()

    fun getHome(): HomeBffResponse {
        val rankingsFuture = CompletableFuture.supplyAsync(
            { productClient.getProducts(categoryId = null, brandId = null, minPrice = null, maxPrice = null, page = 0, size = 10, sort = "newest") },
            executor,
        )
        val newArrivalsFuture = CompletableFuture.supplyAsync(
            { productClient.getProducts(categoryId = null, brandId = null, minPrice = null, maxPrice = null, page = 0, size = 10, sort = "newest") },
            executor,
        )

        CompletableFuture.allOf(rankingsFuture, newArrivalsFuture).join()

        return HomeBffResponse(
            banners = null, // Phase 3
            rankings = rankingsFuture.get().data?.content ?: emptyList(),
            newArrivals = newArrivalsFuture.get().data?.content ?: emptyList(),
            exhibitions = null, // Phase 3
        )
    }
}
