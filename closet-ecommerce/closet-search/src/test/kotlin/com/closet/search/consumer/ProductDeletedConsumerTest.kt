package com.closet.search.consumer

import com.closet.search.application.service.ProductSearchService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.consumer.ConsumerRecord

class ProductDeletedConsumerTest : BehaviorSpec({

    val productSearchService = mockk<ProductSearchService>(relaxed = true)
    val objectMapper: ObjectMapper = jacksonObjectMapper()

    val consumer = ProductDeletedConsumer(
        productSearchService = productSearchService,
        objectMapper = objectMapper,
    )

    Given("product.deleted 이벤트가 수신되면") {

        val payload = ProductDeletedConsumer.ProductDeletedPayload(
            productId = 1L,
            name = "삭제된 상품",
            brandId = 1L,
            categoryId = 1L,
        )

        val record = ConsumerRecord<String, String>(
            "product.deleted", 0, 0L, "1", objectMapper.writeValueAsString(payload)
        )

        When("Consumer가 메시지를 처리하면") {
            consumer.consume(record)

            Then("ProductSearchService.deleteProduct이 호출된다") {
                verify(exactly = 1) {
                    productSearchService.deleteProduct(1L)
                }
            }
        }
    }
})
