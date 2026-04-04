package com.closet.search.consumer

import com.closet.search.application.service.ProductSearchService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.math.BigDecimal

class ProductUpdatedConsumerTest : BehaviorSpec({

    val productSearchService = mockk<ProductSearchService>(relaxed = true)
    val objectMapper: ObjectMapper = jacksonObjectMapper()

    val consumer = ProductUpdatedConsumer(
        productSearchService = productSearchService,
        objectMapper = objectMapper,
    )

    Given("product.updated 이벤트가 수신되면") {

        val payload = ProductUpdatedConsumer.ProductUpdatedPayload(
            productId = 1L,
            name = "오버핏 맨투맨 (리뉴얼)",
            description = "개선된 맨투맨",
            brandId = 1L,
            categoryId = 1L,
            basePrice = BigDecimal("39000"),
            salePrice = BigDecimal("25000"),
            discountRate = 35,
            status = "ACTIVE",
            season = "FW",
            fitType = "OVERFIT",
            gender = "UNISEX",
            sizes = listOf("S", "M", "L", "XL"),
            colors = listOf("블랙", "그레이"),
            imageUrl = "https://cdn.closet.com/1-v2.jpg",
        )

        val record = ConsumerRecord<String, String>(
            "product.updated", 0, 0L, "1", objectMapper.writeValueAsString(payload)
        )

        When("Consumer가 메시지를 처리하면") {
            consumer.consume(record)

            Then("ProductSearchService.updateProduct이 호출된다") {
                verify(exactly = 1) {
                    productSearchService.updateProduct(
                        productId = 1L,
                        name = "오버핏 맨투맨 (리뉴얼)",
                        description = any(),
                        brandId = 1L,
                        categoryId = 1L,
                        basePrice = any(),
                        salePrice = any(),
                        discountRate = 35,
                        status = "ACTIVE",
                        season = any(),
                        fitType = any(),
                        gender = any(),
                        sizes = any(),
                        colors = any(),
                        imageUrl = any(),
                    )
                }
            }
        }
    }
})
