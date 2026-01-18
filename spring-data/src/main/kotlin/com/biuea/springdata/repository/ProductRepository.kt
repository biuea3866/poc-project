package com.biuea.springdata.repository

import com.biuea.springdata.domain.Product
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface ProductRepository : JpaRepository<Product, Long> {

    fun findByCategory(category: String, pageable: Pageable): Page<Product>

    fun findByCreatedDateBetween(
        startDate: LocalDate,
        endDate: LocalDate,
        pageable: Pageable
    ): Page<Product>

    @Query("SELECT p FROM Product p WHERE p.createdDate >= :startDate AND p.createdDate <= :endDate")
    fun findProductsByDateRange(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
        pageable: Pageable
    ): Page<Product>
}
