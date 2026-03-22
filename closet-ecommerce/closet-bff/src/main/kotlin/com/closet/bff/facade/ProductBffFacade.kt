package com.closet.bff.facade

import com.closet.bff.client.ProductServiceClient
import com.closet.bff.dto.ProductDetailBffResponse
import com.closet.bff.dto.ProductSearchParams
import org.springframework.stereotype.Service

@Service
class ProductBffFacade(
    private val productClient: ProductServiceClient,
) {
    fun getProductDetail(productId: Long): ProductDetailBffResponse {
        val product = productClient.getProduct(productId).block()!!
        val relatedProducts = productClient.getProducts(
            ProductSearchParams(page = 0, size = 4)
        ).block()?.content?.filter { it.id != productId }?.take(4)

        return ProductDetailBffResponse(
            product = product,
            reviewSummary = null, // Phase 2
            relatedProducts = relatedProducts,
        )
    }
}
