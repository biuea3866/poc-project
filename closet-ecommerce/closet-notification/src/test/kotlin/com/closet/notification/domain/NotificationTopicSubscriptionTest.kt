package com.closet.notification.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class NotificationTopicSubscriptionTest : BehaviorSpec({

    Given("NotificationTopicSubscription 생성 시") {
        When("상품 토픽으로 구독을 생성하면") {
            val subscription =
                NotificationTopicSubscription.create(
                    memberId = 1L,
                    topicType = TopicType.PRODUCT,
                    topicId = 100L,
                )

            Then("구독이 활성화 상태로 생성된다") {
                subscription.memberId shouldBe 1L
                subscription.topicType shouldBe TopicType.PRODUCT
                subscription.topicId shouldBe 100L
                subscription.isSubscribed shouldBe true
                subscription.subscribedAt shouldNotBe null
            }
        }

        When("카테고리 토픽으로 구독을 생성하면") {
            val subscription =
                NotificationTopicSubscription.create(
                    memberId = 1L,
                    topicType = TopicType.CATEGORY,
                    topicId = 10L,
                )

            Then("카테고리 토픽 구독이 활성화된다") {
                subscription.topicType shouldBe TopicType.CATEGORY
                subscription.topicId shouldBe 10L
                subscription.isSubscribed shouldBe true
            }
        }

        When("브랜드 토픽으로 구독을 생성하면") {
            val subscription =
                NotificationTopicSubscription.create(
                    memberId = 1L,
                    topicType = TopicType.BRAND,
                    topicId = 50L,
                )

            Then("브랜드 토픽 구독이 활성화된다") {
                subscription.topicType shouldBe TopicType.BRAND
                subscription.topicId shouldBe 50L
            }
        }

        When("이벤트 토픽으로 구독을 생성하면") {
            val subscription =
                NotificationTopicSubscription.create(
                    memberId = 1L,
                    topicType = TopicType.EVENT,
                    topicId = 200L,
                )

            Then("이벤트 토픽 구독이 활성화된다") {
                subscription.topicType shouldBe TopicType.EVENT
                subscription.topicId shouldBe 200L
            }
        }
    }

    Given("구독 해제 시") {
        val subscription =
            NotificationTopicSubscription.create(
                memberId = 1L,
                topicType = TopicType.PRODUCT,
                topicId = 100L,
            )

        When("구독을 해제하면") {
            subscription.unsubscribe()

            Then("구독 상태가 비활성화된다") {
                subscription.isSubscribed shouldBe false
                subscription.unsubscribedAt shouldNotBe null
            }
        }
    }

    Given("구독 재활성화 시") {
        val subscription =
            NotificationTopicSubscription.create(
                memberId = 1L,
                topicType = TopicType.PRODUCT,
                topicId = 100L,
            )

        When("구독 해제 후 재활성화하면") {
            subscription.unsubscribe()
            subscription.resubscribe()

            Then("구독 상태가 다시 활성화된다") {
                subscription.isSubscribed shouldBe true
                subscription.unsubscribedAt shouldBe null
            }
        }
    }
})
