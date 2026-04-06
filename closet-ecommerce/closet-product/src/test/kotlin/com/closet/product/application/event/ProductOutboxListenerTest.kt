package com.closet.product.application.event

import com.closet.common.outbox.OutboxEventPublisher
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.math.BigDecimal

class ProductOutboxListenerTest : BehaviorSpec({

    val outboxEventPublisher = mockk<OutboxEventPublisher>(relaxed = true)
    val objectMapper: ObjectMapper = jacksonObjectMapper()
    val listener = ProductOutboxListener(outboxEventPublisher, objectMapper)

    Given("상품 생성 이벤트가 발행되었을 때") {
        val event =
            ProductCreatedEvent(
                productId = 1L,
                name = "오버핏 반팔 티셔츠",
                description = "시원한 여름 반팔 티셔츠",
                brandId = 10L,
                categoryId = 20L,
                basePrice = BigDecimal(39000),
                salePrice = BigDecimal(29000),
                discountRate = 25,
                status = "DRAFT",
                season = "SS",
                fitType = "OVERSIZED",
                gender = "UNISEX",
                sizes = listOf("M", "L", "XL"),
                colors = listOf("블랙", "화이트"),
                imageUrl = "https://cdn.closet.com/product/1/main.jpg",
            )

        When("ProductOutboxListener가 이벤트를 처리하면") {
            listener.handleProductCreated(event)

            Then("outbox_event에 event.closet.product 토픽으로 INSERT한다") {
                val eventTypeSlot = slot<String>()
                val partitionKeySlot = slot<String>()
                val payloadSlot = slot<String>()

                verify {
                    outboxEventPublisher.publish(
                        aggregateType = "Product",
                        aggregateId = "1",
                        eventType = capture(eventTypeSlot),
                        topic = "event.closet.product",
                        partitionKey = capture(partitionKeySlot),
                        payload = capture(payloadSlot),
                    )
                }

                eventTypeSlot.captured shouldBe "ProductCreated"
                partitionKeySlot.captured shouldBe "1"
                payloadSlot.captured shouldContain "오버핏 반팔 티셔츠"
                payloadSlot.captured shouldContain "\"productId\":1"
            }
        }
    }

    Given("상품 수정 이벤트가 발행되었을 때") {
        val event =
            ProductUpdatedEvent(
                productId = 2L,
                name = "수정된 청바지",
                description = "수정된 설명",
                brandId = 10L,
                categoryId = 30L,
                basePrice = BigDecimal(59000),
                salePrice = BigDecimal(49000),
                discountRate = 16,
                status = "ACTIVE",
                season = "ALL",
                fitType = "SLIM",
                gender = "MALE",
                sizes = listOf("S", "M"),
                colors = listOf("인디고"),
                imageUrl = null,
            )

        When("ProductOutboxListener가 이벤트를 처리하면") {
            listener.handleProductUpdated(event)

            Then("outbox_event에 event.closet.product 토픽으로 INSERT한다") {
                val eventTypeSlot = slot<String>()
                val partitionKeySlot = slot<String>()

                verify {
                    outboxEventPublisher.publish(
                        aggregateType = "Product",
                        aggregateId = "2",
                        eventType = capture(eventTypeSlot),
                        topic = "event.closet.product",
                        partitionKey = capture(partitionKeySlot),
                        payload = any(),
                    )
                }

                eventTypeSlot.captured shouldBe "ProductUpdated"
                partitionKeySlot.captured shouldBe "2"
            }
        }
    }

    Given("상품 삭제 이벤트가 발행되었을 때") {
        val event =
            ProductDeletedEvent(
                productId = 3L,
                name = "삭제된 상품",
                brandId = 10L,
                categoryId = 20L,
            )

        When("ProductOutboxListener가 이벤트를 처리하면") {
            listener.handleProductDeleted(event)

            Then("outbox_event에 event.closet.product 토픽으로 INSERT한다") {
                val eventTypeSlot = slot<String>()
                val partitionKeySlot = slot<String>()
                val payloadSlot = slot<String>()

                verify {
                    outboxEventPublisher.publish(
                        aggregateType = "Product",
                        aggregateId = "3",
                        eventType = capture(eventTypeSlot),
                        topic = "event.closet.product",
                        partitionKey = capture(partitionKeySlot),
                        payload = capture(payloadSlot),
                    )
                }

                eventTypeSlot.captured shouldBe "ProductDeleted"
                partitionKeySlot.captured shouldBe "3"
                payloadSlot.captured shouldContain "\"productId\":3"
                payloadSlot.captured shouldContain "삭제된 상품"
            }
        }
    }

    Given("이벤트 페이로드에 필수 필드가 포함되어 있을 때") {
        val event =
            ProductCreatedEvent(
                productId = 100L,
                name = "테스트 상품",
                description = "테스트 설명",
                brandId = 5L,
                categoryId = 15L,
                basePrice = BigDecimal(50000),
                salePrice = BigDecimal(40000),
                discountRate = 20,
                status = "ACTIVE",
                season = "FW",
                fitType = "REGULAR",
                gender = "FEMALE",
                sizes = listOf("S", "M", "L"),
                colors = listOf("레드", "블루"),
                imageUrl = "https://cdn.closet.com/test.jpg",
            )

        When("JSON 직렬화하면") {
            val json = objectMapper.writeValueAsString(event)

            Then("productId, name, brandId, categoryId, price, sizes, colors 등 필수 필드가 포함된다") {
                json shouldContain "\"productId\":100"
                json shouldContain "\"name\":\"테스트 상품\""
                json shouldContain "\"brandId\":5"
                json shouldContain "\"categoryId\":15"
                json shouldContain "\"basePrice\":50000"
                json shouldContain "\"salePrice\":40000"
                json shouldContain "\"sizes\":"
                json shouldContain "\"colors\":"
                json shouldContain "\"imageUrl\":"
                json shouldContain "\"status\":\"ACTIVE\""
            }
        }
    }

    Given("partitionKey가 productId로 설정될 때") {
        val event =
            ProductCreatedEvent(
                productId = 42L,
                name = "파티션키 테스트",
                description = "설명",
                brandId = 1L,
                categoryId = 1L,
                basePrice = BigDecimal(10000),
                salePrice = BigDecimal(8000),
                discountRate = 20,
                status = "DRAFT",
                season = null,
                fitType = null,
                gender = null,
                sizes = emptyList(),
                colors = emptyList(),
                imageUrl = null,
            )

        When("handleProductCreated를 호출하면") {
            listener.handleProductCreated(event)

            Then("partitionKey가 productId의 문자열 값이다") {
                verify {
                    outboxEventPublisher.publish(
                        aggregateType = any(),
                        aggregateId = any(),
                        eventType = any(),
                        topic = any(),
                        partitionKey = "42",
                        payload = any(),
                    )
                }
            }
        }
    }
})
