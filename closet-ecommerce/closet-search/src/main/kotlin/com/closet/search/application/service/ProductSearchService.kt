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
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

/**
 * 상품 검색 서비스.
 *
 * 검색/필터/자동완성 기능과 ES 인덱싱(단건/벌크) 기능을 제공한다.
 */
@Service
class ProductSearchService(
    private val productSearchRepository: ProductSearchRepository,
    private val productSearchRepositoryCustom: ProductSearchRepositoryCustom,
    private val productServiceClient: ProductServiceClient,
) {

    /**
     * 키워드 + 필터 + 정렬 상품 검색.
     */
    fun search(filter: ProductSearchFilter, pageable: Pageable): Page<ProductSearchResponse> {
        val page = productSearchRepositoryCustom.search(filter, pageable)
        return page.map { ProductSearchResponse.from(it) }
    }

    /**
     * 자동완성 검색 (edge_ngram 분석기 활용).
     */
    fun autocomplete(keyword: String, size: Int = 10): List<AutocompleteResponse> {
        val docs = productSearchRepositoryCustom.autocomplete(keyword, size)
        return docs.map {
            AutocompleteResponse(
                productId = it.productId,
                name = it.name,
                brandName = it.brandName,
                imageUrl = it.imageUrl,
            )
        }
    }

    /**
     * 단건 상품 인덱싱 (product.created 이벤트).
     */
    fun indexProduct(
        productId: Long,
        name: String,
        description: String,
        brandId: Long,
        categoryId: Long,
        basePrice: java.math.BigDecimal,
        salePrice: java.math.BigDecimal,
        discountRate: Int,
        status: String,
        season: String?,
        fitType: String?,
        gender: String?,
        sizes: List<String>,
        colors: List<String>,
        imageUrl: String?,
    ) {
        val document = ProductDocument(
            productId = productId,
            name = name,
            description = description,
            brandId = brandId,
            categoryId = categoryId,
            basePrice = basePrice,
            salePrice = salePrice,
            discountRate = discountRate,
            status = status,
            season = season,
            fitType = fitType,
            gender = gender,
            sizes = sizes,
            colors = colors,
            imageUrl = imageUrl,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        productSearchRepository.save(document)
        logger.info { "상품 인덱싱 완료: productId=$productId, name=$name" }
    }

    /**
     * 상품 업데이트 인덱싱 (product.updated 이벤트).
     */
    fun updateProduct(
        productId: Long,
        name: String,
        description: String,
        brandId: Long,
        categoryId: Long,
        basePrice: java.math.BigDecimal,
        salePrice: java.math.BigDecimal,
        discountRate: Int,
        status: String,
        season: String?,
        fitType: String?,
        gender: String?,
        sizes: List<String>,
        colors: List<String>,
        imageUrl: String?,
    ) {
        val existing = productSearchRepository.findById(productId).orElse(null)

        val document = ProductDocument(
            productId = productId,
            name = name,
            description = description,
            brandId = brandId,
            categoryId = categoryId,
            basePrice = basePrice,
            salePrice = salePrice,
            discountRate = discountRate,
            status = status,
            season = season,
            fitType = fitType,
            gender = gender,
            sizes = sizes,
            colors = colors,
            imageUrl = imageUrl,
            popularityScore = existing?.popularityScore ?: 0.0,
            salesCount = existing?.salesCount ?: 0,
            reviewCount = existing?.reviewCount ?: 0,
            avgRating = existing?.avgRating ?: 0.0,
            createdAt = existing?.createdAt,
            updatedAt = LocalDateTime.now(),
        )

        productSearchRepository.save(document)
        logger.info { "상품 업데이트 인덱싱 완료: productId=$productId, name=$name" }
    }

    /**
     * 상품 삭제 (product.deleted 이벤트).
     */
    fun deleteProduct(productId: Long) {
        if (productSearchRepository.existsById(productId)) {
            productSearchRepository.deleteById(productId)
            logger.info { "상품 인덱스 삭제 완료: productId=$productId" }
        } else {
            logger.warn { "삭제 대상 상품 인덱스 없음: productId=$productId" }
        }
    }

    /**
     * 리뷰 집계 부분 업데이트 (review.summary.updated 이벤트).
     */
    fun updateReviewSummary(productId: Long, reviewCount: Int, avgRating: Double) {
        val existing = productSearchRepository.findById(productId).orElse(null)
        if (existing == null) {
            logger.warn { "리뷰 집계 업데이트 대상 상품 인덱스 없음: productId=$productId" }
            return
        }

        val updated = existing.copy(
            reviewCount = reviewCount,
            avgRating = avgRating,
            updatedAt = LocalDateTime.now(),
        )

        productSearchRepository.save(updated)
        logger.info { "리뷰 집계 업데이트 완료: productId=$productId, reviewCount=$reviewCount, avgRating=$avgRating" }
    }

    /**
     * 벌크 인덱싱 (전체 상품 리인덱싱).
     *
     * closet-product 서비스에서 1000건 단위로 페이지네이션하여 ES에 벌크 인덱싱한다.
     */
    fun bulkReindex(): IndexSyncResponse {
        val startTime = System.currentTimeMillis()
        var totalRequested = 0
        var totalIndexed = 0
        var totalFailed = 0
        var page = 0
        val pageSize = 1000

        logger.info { "벌크 리인덱싱 시작" }

        while (true) {
            val pageResponse = try {
                productServiceClient.fetchAllProducts(page, pageSize)
            } catch (e: Exception) {
                logger.error(e) { "상품 서비스 호출 실패: page=$page" }
                break
            }

            if (pageResponse.content.isEmpty()) break

            totalRequested += pageResponse.content.size

            val documents = pageResponse.content.map { it.toDocument() }

            try {
                productSearchRepository.saveAll(documents)
                totalIndexed += documents.size
                logger.info { "벌크 인덱싱 진행: page=$page, indexed=${documents.size}" }
            } catch (e: Exception) {
                totalFailed += documents.size
                logger.error(e) { "벌크 인덱싱 실패: page=$page, failed=${documents.size}" }
            }

            if (pageResponse.last) break
            page++
        }

        val elapsed = System.currentTimeMillis() - startTime
        logger.info { "벌크 리인덱싱 완료: totalRequested=$totalRequested, totalIndexed=$totalIndexed, totalFailed=$totalFailed, elapsed=${elapsed}ms" }

        return IndexSyncResponse(
            totalRequested = totalRequested,
            totalIndexed = totalIndexed,
            totalFailed = totalFailed,
            elapsedMillis = elapsed,
        )
    }

    private fun ProductServiceResponse.toDocument(): ProductDocument {
        return ProductDocument(
            productId = this.id,
            name = this.name,
            description = this.description,
            brandId = this.brandId,
            brandName = this.brandName,
            categoryId = this.categoryId,
            categoryName = this.categoryName,
            basePrice = this.basePrice,
            salePrice = this.salePrice,
            discountRate = this.discountRate,
            status = this.status,
            season = this.season,
            fitType = this.fitType,
            gender = this.gender,
            sizes = this.sizes,
            colors = this.colors,
            imageUrl = this.imageUrl,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
    }
}
