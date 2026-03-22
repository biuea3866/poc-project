package com.closet.search.application.service

import com.closet.search.application.dto.AutocompleteResponse
import com.closet.search.application.dto.IndexSyncResponse
import com.closet.search.application.dto.ProductSearchFilter
import com.closet.search.application.dto.ProductSearchResponse
import com.closet.search.application.dto.ProductServiceResponse
import com.closet.search.domain.ProductDocument
import com.closet.search.infrastructure.client.ProductServiceClient
import com.closet.search.infrastructure.repository.ProductSearchRepository
import com.closet.search.infrastructure.repository.ProductSearchRepositoryCustom
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class ProductSearchService(
    private val productSearchRepository: ProductSearchRepository,
    private val productSearchRepositoryCustom: ProductSearchRepositoryCustom,
    private val productServiceClient: ProductServiceClient,
) {

    fun search(keyword: String?, filter: ProductSearchFilter, pageable: Pageable): Page<ProductSearchResponse> {
        return productSearchRepositoryCustom.search(keyword, filter, pageable)
            .map { ProductSearchResponse.from(it) }
    }

    fun autocomplete(prefix: String, limit: Int): AutocompleteResponse {
        val suggestions = productSearchRepositoryCustom.autocomplete(prefix, limit)
        return AutocompleteResponse(suggestions = suggestions)
    }

    fun indexProduct(product: ProductServiceResponse) {
        val document = toDocument(product)
        productSearchRepository.save(document)
        logger.info { "상품 인덱싱 완료: id=${product.id}, name=${product.name}" }
    }

    fun bulkIndex(products: List<ProductServiceResponse>) {
        val documents = products.map { toDocument(it) }
        productSearchRepository.saveAll(documents)
        logger.info { "벌크 인덱싱 완료: ${documents.size}건" }
    }

    fun deleteProduct(productId: Long) {
        productSearchRepository.deleteById(productId)
        logger.info { "상품 인덱스 삭제: id=$productId" }
    }

    fun syncFromProductService(): IndexSyncResponse {
        logger.info { "product-service로부터 전체 동기화 시작" }
        var totalIndexed = 0
        var page = 0
        val size = 1000

        while (true) {
            val pageResponse = productServiceClient.fetchAllProducts(page, size)
                ?: break

            if (pageResponse.content.isEmpty()) break

            bulkIndex(pageResponse.content)
            totalIndexed += pageResponse.content.size

            logger.info { "동기화 진행: page=$page, indexed=${pageResponse.content.size}, total=$totalIndexed" }

            if (pageResponse.last) break
            page++
        }

        logger.info { "전체 동기화 완료: 총 ${totalIndexed}건 인덱싱" }
        return IndexSyncResponse(
            indexedCount = totalIndexed,
            message = "전체 동기화 완료: ${totalIndexed}건 인덱싱"
        )
    }

    private fun toDocument(product: ProductServiceResponse): ProductDocument {
        return ProductDocument(
            id = product.id,
            name = product.name,
            description = product.description,
            brandId = product.brandId,
            brandName = null,
            categoryId = product.categoryId,
            categoryName = null,
            basePrice = product.basePrice,
            salePrice = product.salePrice,
            discountRate = product.discountRate,
            status = product.status,
            season = product.season ?: "ALL",
            fitType = product.fitType ?: "REGULAR",
            gender = product.gender ?: "UNISEX",
            createdAt = null,
        )
    }
}
