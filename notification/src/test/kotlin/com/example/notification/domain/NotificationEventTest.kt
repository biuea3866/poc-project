package com.example.notification.domain

import com.example.notification.domain.enums.NotificationChannel
import com.example.notification.domain.enums.NotificationTriggerType
import com.example.notification.domain.model.NotificationEvent
import com.example.notification.domain.model.NotificationPayload
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class NotificationEventTest : BehaviorSpec({

    Given("correlationId가 있는 이벤트") {
        val event = NotificationEvent(
            triggerType = NotificationTriggerType.ORDER_PLACED,
            storeId = 1L,
            orderId = 100L,
            payload = NotificationPayload.OrderPlaced(
                orderId = 100L,
                buyerName = "홍길동",
                storeName = "맛있는 빵집",
                amount = 35000L,
                itemCount = 3,
            ),
            correlationId = "batch-001",
            correlationTotalCount = 5,
        )

        When("isCorrelated 확인") {
            Then("true를 반환한다") {
                event.isCorrelated shouldBe true
            }
        }

        When("isCorrelationCountReached 확인") {
            Then("currentCount < totalCount이면 false") {
                event.isCorrelationCountReached(3) shouldBe false
            }
            Then("currentCount == totalCount이면 true") {
                event.isCorrelationCountReached(5) shouldBe true
            }
            Then("currentCount > totalCount이면 true") {
                event.isCorrelationCountReached(7) shouldBe true
            }
        }
    }

    Given("correlationId가 없는 이벤트") {
        val event = NotificationEvent(
            triggerType = NotificationTriggerType.ORDER_PLACED,
            storeId = 1L,
            orderId = 100L,
            payload = NotificationPayload.OrderPlaced(
                orderId = 100L,
                buyerName = "홍길동",
                storeName = "맛있는 빵집",
                amount = 35000L,
                itemCount = 1,
            ),
        )

        When("isCorrelated 확인") {
            Then("false를 반환한다") {
                event.isCorrelated shouldBe false
            }
        }
    }

    Given("idempotencyKey 생성") {
        val event = NotificationEvent(
            triggerType = NotificationTriggerType.ORDER_PLACED,
            storeId = 1L,
            orderId = 100L,
            occurredAt = 1234567890L,
            payload = NotificationPayload.OrderPlaced(
                orderId = 100L,
                buyerName = "홍길동",
                storeName = "맛있는 빵집",
                amount = 35000L,
                itemCount = 1,
            ),
        )

        When("generateIdempotencyKey 호출") {
            val key = event.generateIdempotencyKey(42L, NotificationChannel.EMAIL)

            Then("올바른 포맷의 키가 생성된다") {
                key shouldBe "1:ORDER_PLACED:42:100:EMAIL:1234567890"
            }
        }
    }

    Given("correlationIdempotencyKey 생성") {
        val event = NotificationEvent(
            triggerType = NotificationTriggerType.ORDER_PLACED,
            storeId = 1L,
            orderId = 100L,
            payload = NotificationPayload.OrderPlaced(
                orderId = 100L,
                buyerName = "홍길동",
                storeName = "맛있는 빵집",
                amount = 35000L,
                itemCount = 1,
            ),
            correlationId = "batch-001",
        )

        When("generateCorrelationIdempotencyKey 호출") {
            val key = event.generateCorrelationIdempotencyKey(42L, NotificationChannel.PUSH)

            Then("올바른 포맷의 키가 생성된다") {
                key shouldBe "1:CORR:batch-001:42:PUSH"
            }
        }
    }
})
