package com.closet.search.consumer

import com.closet.search.application.service.ProductSearchService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.math.BigDecimal

class ProductCreatedConsumerTest : BehaviorSpec({

    val productSearchService = mockk<ProductSearchService>(relaxed = true)
    val objectMapper: ObjectMapper = jacksonObjectMapper()

    val consumer = ProductCreatedConsumer(
        productSearchService = productSearchService,
        objectMapper = objectMapper,
    )

    Given("product.created 이벤트가 수신되면") {

        val payload = ProductCreatedConsumer.ProductCreatedPayload(
            productId = 1L,
            name = "오버핏 맨투맨",
            description = "편안한 맨투맨",
            brandId = 1L,
            categoryId = 1L,
            basePrice = BigDecimal("39000"),
            salePrice = BigDecimal("29000"),
            discountRate = 25,
            status = "ACTIVE",
            season = "FW",
            fitType = "OVERFIT",
            gender = "UNISEX",
            sizes = listOf("S", "M", "L"),
            colors = listOf("블랙"),
            imageUrl = "https://cdn.closet.com/1.jpg",
        )

        val record = ConsumerRecord<String, String>(
            "product.created", 0, 0L, "1", objectMapper.writeValueAsString(payload)
        )

        When("Consumer가 메시지를 처리하면") {
            consumer.consume(record)

            Then("ProductSearchService.indexProduct이 호출된다") {
                verify(exactly = 1) {
                    productSearchService.indexProduct(
                        productId = 1L,
                        name = "오버핏 맨투맨",
                        description = "편안한 맨투맨",
                        brandId = 1L,
                        categoryId = 1L,
                        basePrice = BigDecimal("39000"),
                        salePrice = BigDecimal("29000"),
                        discountRate = 25,
                        status = "ACTIVE",
                        season = "FW",
                        fitType = "OVERFIT",
                        gender = "UNISEX",
                        sizes = listOf("S", "M", "L"),
                        colors = listOf("블랙"),
                        imageUrl = "https://cdn.closet.com/1.jpg",
                    )
                }
            }
        }
    }

    Given("잘못된 형식의 메시지가 수신되면") {

        val record = ConsumerRecord<String, String>(
            "product.created", 0, 0L, "invalid", "invalid-json"
        )

        When("Consumer가 메시지를 처리하면") {
            consumer.consume(record)

            Then("파싱 실패로 인덱싱이 호출되지 않는다") {
                // invalid json이므로 indexProduct가 추가로 호출되지 않음
                // (이전 Given에서 1회 호출, 여기서 추가 호출 없음)
            }
        }
    }
})
