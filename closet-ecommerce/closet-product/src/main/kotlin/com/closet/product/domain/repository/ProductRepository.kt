package com.closet.product.domain.repository

import com.closet.product.domain.entity.Product
import org.springframework.data.jpa.repository.JpaRepository

interface ProductRepository : JpaRepository<Product, Long>, ProductRepositoryCustom
