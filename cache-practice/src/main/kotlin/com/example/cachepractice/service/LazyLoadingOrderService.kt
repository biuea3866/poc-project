package com.example.cachepractice.service

import com.example.cachepractice.config.CacheConfig
import com.example.cachepractice.domain.Order
import com.example.cachepractice.domain.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LazyLoadingOrderService(
    private val orderRepository: OrderRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Cacheable(value = [CacheConfig.LAZY_CACHE], key = "#id")
    @Transactional(readOnly = true)
    fun getOrder(id: Long): Order? {
        logger.debug("Fetching order from database: $id")
        return orderRepository.findById(id).orElse(null)
    }

    @Transactional(readOnly = true)
    fun getOrderWithoutCache(id: Long): Order? {
        logger.debug("Fetching order from database without cache: $id")
        return orderRepository.findById(id).orElse(null)
    }
}
