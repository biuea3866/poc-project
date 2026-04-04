package com.closet.search.consumer

import com.closet.search.application.service.ProductSearchService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.consumer.ConsumerRecord

class ReviewSummaryConsumerTest : BehaviorSpec({

    val productSearchService = mockk<ProductSearchService>(relaxed = true)
    val objectMapper: ObjectMapper = jacksonObjectMapper()

    val consumer = ReviewSummaryConsumer(
        productSearchService = productSearchService,
        objectMapper = objectMapper,
    )

    Given("review.summary.updated 이벤트가 수신되면") {

        val payload = ReviewSummaryConsumer.ReviewSummaryPayload(
            productId = 5L,
            reviewCount = 55,
            avgRating = 4.3,
        )

        val record = ConsumerRecord<String, String>(
            "review.summary.updated", 0, 0L, "5", objectMapper.writeValueAsString(payload)
        )

        When("Consumer가 메시지를 처리하면") {
            consumer.consume(record)

            Then("ProductSearchService.updateReviewSummary가 호출된다") {
                verify(exactly = 1) {
                    productSearchService.updateReviewSummary(
                        productId = 5L,
                        reviewCount = 55,
                        avgRating = 4.3,
                    )
                }
            }
        }
    }
})
