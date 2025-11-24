package com.example.cachepractice.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface OrderRepository : JpaRepository<Order, Long> {

    fun findByCustomerId(customerId: Long): List<Order>

    @Query("SELECT o FROM Order o WHERE o.id IN :ids")
    fun findAllByIdIn(ids: List<Long>): List<Order>
}
