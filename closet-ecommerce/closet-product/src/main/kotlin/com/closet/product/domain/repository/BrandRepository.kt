package com.closet.product.domain.repository

import com.closet.product.domain.entity.Brand
import org.springframework.data.jpa.repository.JpaRepository

interface BrandRepository : JpaRepository<Brand, Long> {
    fun findByDeletedAtIsNull(): List<Brand>
}
