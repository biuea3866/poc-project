package com.biuea.concurrency.waitingnumber.repository

import com.biuea.concurrency.waitingnumber.domain.ProductWaiting
import org.springframework.data.jpa.repository.JpaRepository

interface ProductWaitingRepository: JpaRepository<ProductWaiting, Long> {
    fun findByProductId(productId: Long): ProductWaiting?
}