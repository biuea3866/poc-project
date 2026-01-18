package com.biuea.springdata.repository

import com.biuea.springdata.domain.Comment
import com.biuea.springdata.domain.CommentId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface CommentRepository : JpaRepository<Comment, CommentId> {

    @Query("""
        SELECT c FROM Comment c
        WHERE c.productId = :productId
    """)
    fun findByProductId(@Param("productId") productId: Long): List<Comment>

    @Query("""
        SELECT c FROM Comment c
        inner join Product p on p.id = c.productId
        WHERE p.id IN :productIds
        AND c.id.createdDate >= :startDate
        AND c.id.createdDate <= :endDate
    """)
    fun findByProductIdAndDateRange(
        @Param("productIds") productIds: List<Long>,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<Comment>

    @Query("""
        SELECT c FROM Comment c
        inner join Product p on p.id = c.productId
        WHERE c.productId IN :productIds
    """)
    fun findByProductIdIn(@Param("productIds") productIds: List<Long>): List<Comment>
}
