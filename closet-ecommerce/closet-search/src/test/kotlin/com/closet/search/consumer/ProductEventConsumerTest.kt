package com.closet.search.consumer

import com.closet.search.application.facade.SearchFacade
import com.closet.search.consumer.event.ProductEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.mockk
import io.mockk.verify
import java.math.BigDecimal

class ProductEventConsumerTest : BehaviorSpec({

    val searchFacade = mockk<SearchFacade>(relaxed = true)

    val consumer = ProductEventConsumer(
        searchFacade = searchFacade,
    )

    Given("ProductCreated 이벤트가 event.closet.product로 수신되면") {

        val event = ProductEvent(
            eventType = "ProductCreated",
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

        When("Consumer가 메시지를 처리하면") {
            consumer.handle(event)

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

        val event = ProductEvent(
            eventType = "ProductUpdated",
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

        When("Consumer가 메시지를 처리하면") {
            consumer.handle(event)

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

        val event = ProductEvent(
            eventType = "ProductDeleted",
            productId = 1L,
            name = "삭제된 상품",
            brandId = 1L,
            categoryId = 1L,
        )

        When("Consumer가 메시지를 처리하면") {
            consumer.handle(event)

            Then("SearchFacade.handleProductDeleted가 호출된다") {
                verify(exactly = 1) {
                    searchFacade.handleProductDeleted(1L)
                }
            }
        }
    }

    Given("처리하지 않는 eventType이 수신되면") {

        val event = ProductEvent(
            eventType = "SomeUnknownEvent",
            productId = 1L,
        )

        When("Consumer가 메시지를 처리하면") {
            consumer.handle(event)

            Then("아무 서비스 호출도 발생하지 않는다") {
                // unknown eventType이므로 무시
            }
        }
    }
})
