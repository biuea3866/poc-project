package com.closet.product.domain.repository

import com.closet.product.domain.entity.Product
import com.closet.product.domain.enums.ProductStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.math.BigDecimal

interface ProductRepositoryCustom {
    fun findByFilter(
        categoryId: Long?,
        brandId: Long?,
        minPrice: BigDecimal?,
        maxPrice: BigDecimal?,
        status: ProductStatus?,
        pageable: Pageable,
    ): Page<Product>
}
