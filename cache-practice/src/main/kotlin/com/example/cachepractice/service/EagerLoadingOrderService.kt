package com.example.cachepractice.service

import com.example.cachepractice.config.CacheConfig
import com.example.cachepractice.domain.Order
import com.example.cachepractice.domain.OrderRepository
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EagerLoadingOrderService(
    private val orderRepository: OrderRepository,
    private val cacheManager: CacheManager
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // @PostConstruct - Disabled for lazy loading strategy
    @Transactional(readOnly = true)
    fun loadAllOrdersToCache() {
        logger.info("Starting eager cache loading...")
        val startTime = System.currentTimeMillis()

        val cache = cacheManager.getCache(CacheConfig.EAGER_CACHE)
        if (cache == null) {
            logger.error("Cache ${CacheConfig.EAGER_CACHE} not found!")
            return
        }

        val batchSize = 10000
        var offset = 0L
        var loadedCount = 0

        while (true) {
            val orders = orderRepository.findAll()
                .drop(offset.toInt())
                .take(batchSize)

            if (orders.isEmpty()) break

            orders.forEach { order ->
                order.id?.let { id -> cache.put(id, order) }
            }

            loadedCount += orders.size
            offset += batchSize

            if (loadedCount % 100000 == 0) {
                logger.info("Loaded $loadedCount orders to cache...")
            }

            if (orders.size < batchSize) break
        }

        val endTime = System.currentTimeMillis()
        logger.info("Eager cache loading completed! Total orders: $loadedCount, Time: ${(endTime - startTime) / 1000}s")
    }

    @Transactional(readOnly = true)
    fun getOrder(id: Long): Order? {
        val cache = cacheManager.getCache(CacheConfig.EAGER_CACHE)
        val cachedOrder = cache?.get(id, Order::class.java)

        if (cachedOrder != null) {
            logger.debug("Order found in cache: $id")
            return cachedOrder
        }

        logger.debug("Cache miss, fetching from database: $id")
        val order = orderRepository.findById(id).orElse(null)
        if (order != null) {
            cache?.put(id, order)
        }
        return order
    }

    @Transactional(readOnly = true)
    fun getOrderWithoutCache(id: Long): Order? {
        logger.debug("Fetching order from database without cache: $id")
        return orderRepository.findById(id).orElse(null)
    }
}
