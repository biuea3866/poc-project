package com.closet.product.domain.repository

import com.closet.product.domain.entity.ProductOption
import org.springframework.data.jpa.repository.JpaRepository

interface ProductOptionRepository : JpaRepository<ProductOption, Long> {
    fun findByProductId(productId: Long): List<ProductOption>
}
