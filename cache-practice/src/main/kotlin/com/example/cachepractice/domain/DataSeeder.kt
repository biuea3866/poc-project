package com.example.cachepractice.domain

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.random.Random

@Component
@Profile("seed")
class DataSeeder(
    private val orderRepository: OrderRepository
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String?) {
        logger.info("Starting data seeding...")
        val startTime = System.currentTimeMillis()

        val totalOrders = 1_000_000
        val batchSize = 1000

        var count = 0
        val orders = mutableListOf<Order>()

        for (i in 1..totalOrders) {
            val order = createRandomOrder(i.toLong())
            orders.add(order)

            if (orders.size >= batchSize) {
                orderRepository.saveAll(orders)
                orders.clear()
                count += batchSize
                if (count % 10000 == 0) {
                    logger.info("Seeded $count orders...")
                }
            }
        }

        if (orders.isNotEmpty()) {
            orderRepository.saveAll(orders)
            count += orders.size
        }

        val endTime = System.currentTimeMillis()
        logger.info("Data seeding completed! Total orders: $count, Time: ${(endTime - startTime) / 1000}s")
    }

    private fun createRandomOrder(orderId: Long): Order {
        val customerId = Random.nextLong(1, 100001)
        val customerName = "Customer$customerId"
        val totalAmount = BigDecimal(Random.nextDouble(100.0, 10000.0))

        return Order(
            customerId = customerId,
            customerName = customerName,
            totalAmount = totalAmount,
            orderDate = LocalDateTime.now().minusDays(Random.nextLong(0, 365)),
            status = OrderStatus.values()[Random.nextInt(OrderStatus.values().size)]
        )
    }
}
