package com.closet.bff.facade

import com.closet.bff.client.ProductServiceClient
import com.closet.bff.dto.ProductDetailBffResponse
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@Service
class ProductBffFacade(
    private val productClient: ProductServiceClient,
) {
    private val executor = Executors.newVirtualThreadPerTaskExecutor()

    fun getProductDetail(productId: Long): ProductDetailBffResponse {
        val productFuture =
            CompletableFuture.supplyAsync(
                { productClient.getProduct(productId) },
                executor,
            )
        val relatedFuture =
            CompletableFuture.supplyAsync(
                {
                    productClient.getProducts(
                        categoryId = null,
                        brandId = null,
                        minPrice = null,
                        maxPrice = null,
                        page = 0,
                        size = 4,
                        sort = "newest",
                    )
                },
                executor,
            )

        CompletableFuture.allOf(productFuture, relatedFuture).join()

        val product = productFuture.get().data!!
        val relatedProducts =
            relatedFuture.get().data?.content
                ?.filter { it.id != productId }
                ?.take(4)

        return ProductDetailBffResponse(
            product = product,
            // Phase 2
            reviewSummary = null,
            relatedProducts = relatedProducts,
        )
    }
}
