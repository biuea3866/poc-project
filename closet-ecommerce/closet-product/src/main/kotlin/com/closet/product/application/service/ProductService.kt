package com.closet.product.application.service

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.common.vo.Money
import com.closet.product.application.dto.ProductCreateRequest
import com.closet.product.application.dto.ProductListResponse
import com.closet.product.application.dto.ProductOptionCreateRequest
import com.closet.product.application.dto.ProductOptionResponse
import com.closet.product.application.dto.ProductResponse
import com.closet.product.application.dto.ProductUpdateRequest
import com.closet.product.domain.entity.Product
import com.closet.product.domain.entity.ProductOption
import com.closet.product.domain.enums.ProductStatus
import com.closet.product.domain.repository.ProductRepository
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class ProductService(
    private val productRepository: ProductRepository
) {

    @Transactional
    fun create(request: ProductCreateRequest): ProductResponse {
        val product = Product(
            name = request.name,
            description = request.description,
            brandId = request.brandId,
            categoryId = request.categoryId,
            basePrice = Money(request.basePrice),
            salePrice = Money(request.salePrice),
            discountRate = request.discountRate,
            season = request.season,
            fitType = request.fitType,
            gender = request.gender
        )
        val saved = productRepository.save(product)
        logger.info { "상품 생성 완료: id=${saved.id}, name=${saved.name}" }
        return ProductResponse.from(saved)
    }

    @Transactional
    fun update(id: Long, request: ProductUpdateRequest): ProductResponse {
        val product = findProductById(id)
        product.update(
            name = request.name,
            description = request.description,
            brandId = request.brandId,
            categoryId = request.categoryId,
            basePrice = Money(request.basePrice),
            salePrice = Money(request.salePrice),
            discountRate = request.discountRate,
            season = request.season,
            fitType = request.fitType,
            gender = request.gender
        )
        return ProductResponse.from(product)
    }

    fun findById(id: Long): ProductResponse {
        val product = findProductById(id)
        return ProductResponse.from(product)
    }

    fun findAll(
        categoryId: Long?,
        brandId: Long?,
        minPrice: BigDecimal?,
        maxPrice: BigDecimal?,
        status: ProductStatus?,
        pageable: Pageable
    ): Page<ProductListResponse> {
        return productRepository.findByFilter(categoryId, brandId, minPrice, maxPrice, status, pageable)
            .map { ProductListResponse.from(it) }
    }

    @Transactional
    fun changeStatus(id: Long, targetStatus: ProductStatus): ProductResponse {
        val product = findProductById(id)
        product.changeStatus(targetStatus)
        logger.info { "상품 상태 변경: id=$id, status=$targetStatus" }
        return ProductResponse.from(product)
    }

    @Transactional
    fun addOption(productId: Long, request: ProductOptionCreateRequest): ProductOptionResponse {
        val product = findProductById(productId)
        val option = ProductOption(
            size = request.size,
            colorName = request.colorName,
            colorHex = request.colorHex,
            skuCode = request.skuCode,
            additionalPrice = Money(request.additionalPrice)
        )
        product.addOption(option)
        productRepository.flush()
        logger.info { "상품 옵션 추가: productId=$productId, skuCode=${request.skuCode}" }
        return ProductOptionResponse.from(option)
    }

    @Transactional
    fun removeOption(productId: Long, optionId: Long) {
        val product = findProductById(productId)
        product.removeOption(optionId)
        logger.info { "상품 옵션 삭제: productId=$productId, optionId=$optionId" }
    }

    private fun findProductById(id: Long): Product {
        return productRepository.findById(id)
            .filter { !it.isDeleted() }
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "상품을 찾을 수 없습니다: $id") }
    }
}
