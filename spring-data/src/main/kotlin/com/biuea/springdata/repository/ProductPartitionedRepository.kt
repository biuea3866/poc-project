package com.biuea.springdata.repository

import com.biuea.springdata.domain.ProductPartitioned
import com.biuea.springdata.domain.ProductPartitionedId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface ProductPartitionedRepository : JpaRepository<ProductPartitioned, ProductPartitionedId> {

    @Query("""
        SELECT p FROM ProductPartitioned p
        WHERE p.category = :category
    """)
    fun findByCategory(@Param("category") category: String, pageable: Pageable): Page<ProductPartitioned>

    @Query("""
        SELECT p FROM ProductPartitioned p
        WHERE p.id.createdDate >= :startDate AND p.id.createdDate <= :endDate
    """)
    fun findByCreatedDateBetween(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
        pageable: Pageable
    ): Page<ProductPartitioned>

    @Query("""
        SELECT p FROM ProductPartitioned p
        WHERE p.id.createdDate >= :startDate AND p.id.createdDate <= :endDate
    """)
    fun findProductsByDateRange(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
        pageable: Pageable
    ): Page<ProductPartitioned>
}
