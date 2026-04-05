package com.closet.member.consumer

import com.closet.member.application.PointService
import com.closet.member.consumer.event.ReviewEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class ReviewEventConsumerTest : BehaviorSpec({

    val pointService = mockk<PointService>(relaxed = true)

    val consumer = ReviewEventConsumer(
        pointService = pointService,
    )

    Given("ReviewCreated 이벤트가 수신되면") {
        every { pointService.earnReviewPoint(any(), any(), any()) } returns 200

        val event = ReviewEvent(
            eventType = "ReviewCreated",
            reviewId = 1L,
            memberId = 10L,
            pointAmount = 200,
        )

        When("Consumer가 메시지를 처리하면") {
            consumer.handle(event)

            Then("PointService.earnReviewPoint가 호출된다") {
                verify(exactly = 1) {
                    pointService.earnReviewPoint(
                        memberId = 10L,
                        reviewId = 1L,
                        amount = 200,
                    )
                }
            }
        }
    }

    Given("ReviewDeleted 이벤트가 수신되면") {
        clearMocks(pointService)

        val event = ReviewEvent(
            eventType = "ReviewDeleted",
            reviewId = 2L,
            memberId = 10L,
            pointAmount = 500,
        )

        When("Consumer가 메시지를 처리하면") {
            consumer.handle(event)

            Then("PointService.revokeReviewPoint가 호출된다") {
                verify(exactly = 1) {
                    pointService.revokeReviewPoint(
                        memberId = 10L,
                        reviewId = 2L,
                        amount = 500,
                    )
                }
            }
        }
    }

    Given("처리하지 않는 eventType 수신") {
        val event = ReviewEvent(
            eventType = "ReviewUpdated",
            reviewId = 3L,
        )

        When("ReviewUpdated 이벤트 수신") {
            consumer.handle(event)

            Then("무시된다") {
                // eventType 필터에 의해 무시
            }
        }
    }
})
