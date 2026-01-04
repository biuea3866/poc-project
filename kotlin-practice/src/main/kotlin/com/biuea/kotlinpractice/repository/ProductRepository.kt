package com.biuea.kotlinpractice.repository

import com.biuea.kotlinpractice.domain.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository : JpaRepository<Product, Long> {
    fun findByNameContaining(name: String): List<Product>
}
