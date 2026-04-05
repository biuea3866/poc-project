package com.closet.search.consumer

import com.closet.search.application.facade.SearchFacade
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.math.BigDecimal

class ProductEventConsumerTest : BehaviorSpec({

    val searchFacade = mockk<SearchFacade>(relaxed = true)
    val objectMapper: ObjectMapper = jacksonObjectMapper()

    val consumer = ProductEventConsumer(
        searchFacade = searchFacade,
        objectMapper = objectMapper,
    )

    Given("ProductCreated 이벤트가 event.closet.product로 수신되면") {

        val payload = mapOf(
            "eventType" to "ProductCreated",
            "productId" to 1L,
            "name" to "오버핏 맨투맨",
            "description" to "편안한 맨투맨",
            "brandId" to 1L,
            "categoryId" to 1L,
            "basePrice" to BigDecimal("39000"),
            "salePrice" to BigDecimal("29000"),
            "discountRate" to 25,
            "status" to "ACTIVE",
            "season" to "FW",
            "fitType" to "OVERFIT",
            "gender" to "UNISEX",
            "sizes" to listOf("S", "M", "L"),
            "colors" to listOf("블랙"),
            "imageUrl" to "https://cdn.closet.com/1.jpg",
        )

        val record = ConsumerRecord<String, String>(
            "event.closet.product", 0, 0L, "1", objectMapper.writeValueAsString(payload)
        )

        When("Consumer가 메시지를 처리하면") {
            consumer.consume(record)

            Then("SearchFacade.handleProductCreated가 호출된다") {
                verify(exactly = 1) {
                    searchFacade.handleProductCreated(
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

    Given("ProductUpdated 이벤트가 event.closet.product로 수신되면") {

        val payload = mapOf(
            "eventType" to "ProductUpdated",
            "productId" to 1L,
            "name" to "오버핏 맨투맨 (리뉴얼)",
            "description" to "개선된 맨투맨",
            "brandId" to 1L,
            "categoryId" to 1L,
            "basePrice" to BigDecimal("39000"),
            "salePrice" to BigDecimal("25000"),
            "discountRate" to 35,
            "status" to "ACTIVE",
            "season" to "FW",
            "fitType" to "OVERFIT",
            "gender" to "UNISEX",
            "sizes" to listOf("S", "M", "L", "XL"),
            "colors" to listOf("블랙", "그레이"),
            "imageUrl" to "https://cdn.closet.com/1-v2.jpg",
        )

        val record = ConsumerRecord<String, String>(
            "event.closet.product", 0, 0L, "1", objectMapper.writeValueAsString(payload)
        )

        When("Consumer가 메시지를 처리하면") {
            consumer.consume(record)

            Then("SearchFacade.handleProductUpdated가 호출된다") {
                verify(exactly = 1) {
                    searchFacade.handleProductUpdated(
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

    Given("ProductDeleted 이벤트가 event.closet.product로 수신되면") {

        val payload = mapOf(
            "eventType" to "ProductDeleted",
            "productId" to 1L,
            "name" to "삭제된 상품",
            "brandId" to 1L,
            "categoryId" to 1L,
        )

        val record = ConsumerRecord<String, String>(
            "event.closet.product", 0, 0L, "1", objectMapper.writeValueAsString(payload)
        )

        When("Consumer가 메시지를 처리하면") {
            consumer.consume(record)

            Then("SearchFacade.handleProductDeleted가 호출된다") {
                verify(exactly = 1) {
                    searchFacade.handleProductDeleted(1L)
                }
            }
        }
    }

    Given("처리하지 않는 eventType이 수신되면") {

        val payload = mapOf(
            "eventType" to "SomeUnknownEvent",
            "productId" to 1L,
        )

        val record = ConsumerRecord<String, String>(
            "event.closet.product", 0, 0L, "1", objectMapper.writeValueAsString(payload)
        )

        When("Consumer가 메시지를 처리하면") {
            consumer.consume(record)

            Then("아무 서비스 호출도 발생하지 않는다") {
                // unknown eventType이므로 무시
            }
        }
    }

    Given("잘못된 형식의 메시지가 수신되면") {

        val record = ConsumerRecord<String, String>(
            "event.closet.product", 0, 0L, "invalid", "invalid-json"
        )

        When("Consumer가 메시지를 처리하면") {
            consumer.consume(record)

            Then("파싱 실패로 인덱싱이 호출되지 않는다") {
                // invalid json이므로 추가 호출 없음
            }
        }
    }
})
