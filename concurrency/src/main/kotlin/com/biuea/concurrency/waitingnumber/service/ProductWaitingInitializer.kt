package com.biuea.concurrency.waitingnumber.service

import com.biuea.concurrency.waitingnumber.domain.ProductWaiting
import com.biuea.concurrency.waitingnumber.repository.ProductWaitingRepository
import jakarta.persistence.EntityManager
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class ProductWaitingInitializer(
    private val productWaitingRepository: ProductWaitingRepository,
    private val entityManager: EntityManager
) {
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun getOrCreate(productId: Long): ProductWaiting {
        // 먼저 조회
        return productWaitingRepository.findByProductId(productId)
            ?: productWaitingRepository.save(ProductWaiting(null, 1, productId))
    }
}
