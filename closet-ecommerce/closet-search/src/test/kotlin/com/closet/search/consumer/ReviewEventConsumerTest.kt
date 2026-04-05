package com.closet.search.consumer

import com.closet.search.application.facade.SearchFacade
import com.closet.search.consumer.event.ReviewEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.mockk
import io.mockk.verify

class ReviewEventConsumerTest : BehaviorSpec({

    val searchFacade = mockk<SearchFacade>(relaxed = true)

    val consumer = ReviewEventConsumer(
        searchFacade = searchFacade,
    )

    Given("ReviewSummaryUpdated 이벤트가 event.closet.review로 수신되면") {

        val event = ReviewEvent(
            eventType = "ReviewSummaryUpdated",
            productId = 5L,
            reviewCount = 55,
            avgRating = 4.3,
        )

        When("Consumer가 메시지를 처리하면") {
            consumer.handle(event)

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

        val event = ReviewEvent(
            eventType = "ReviewCreated",
            productId = 5L,
        )

        When("Consumer가 메시지를 처리하면") {
            consumer.handle(event)

            Then("handleReviewSummaryUpdated가 추가 호출되지 않는다") {
                // ReviewCreated는 search-service에서 처리하지 않으므로 무시
            }
        }
    }
})
