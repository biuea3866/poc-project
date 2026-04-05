package com.closet.search.consumer

import com.closet.search.application.facade.SearchFacade
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.consumer.ConsumerRecord

class ReviewEventConsumerTest : BehaviorSpec({

    val searchFacade = mockk<SearchFacade>(relaxed = true)
    val objectMapper: ObjectMapper = jacksonObjectMapper()

    val consumer = ReviewEventConsumer(
        searchFacade = searchFacade,
        objectMapper = objectMapper,
    )

    Given("ReviewSummaryUpdated 이벤트가 event.closet.review로 수신되면") {

        val payload = mapOf(
            "eventType" to "ReviewSummaryUpdated",
            "productId" to 5L,
            "reviewCount" to 55,
            "avgRating" to 4.3,
        )

        val record = ConsumerRecord<String, String>(
            "event.closet.review", 0, 0L, "5", objectMapper.writeValueAsString(payload)
        )

        When("Consumer가 메시지를 처리하면") {
            consumer.consume(record)

            Then("SearchFacade.handleReviewSummaryUpdated가 호출된다") {
                verify(exactly = 1) {
                    searchFacade.handleReviewSummaryUpdated(
                        productId = 5L,
                        reviewCount = 55,
                        avgRating = 4.3,
                    )
                }
            }
        }
    }

    Given("처리하지 않는 eventType이 수신되면") {

        val payload = mapOf(
            "eventType" to "ReviewCreated",
            "productId" to 5L,
        )

        val record = ConsumerRecord<String, String>(
            "event.closet.review", 0, 0L, "5", objectMapper.writeValueAsString(payload)
        )

        When("Consumer가 메시지를 처리하면") {
            consumer.consume(record)

            Then("handleReviewSummaryUpdated가 추가 호출되지 않는다") {
                // ReviewCreated는 search-service에서 처리하지 않으므로 무시
            }
        }
    }
})
