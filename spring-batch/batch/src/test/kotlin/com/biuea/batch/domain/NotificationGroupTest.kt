package com.biuea.batch.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class NotificationGroupTest : BehaviorSpec({

    Given("같은 그룹의 알림이 단건(itemCount=1)인 NotificationGroup") {
        val group = NotificationGroup(
            userId = 1L,
            type = NotificationType.POST_COMMENT,
            groupKey = "post-100",
            itemCount = 1,
            samplePayload = "첫 댓글이 달렸어요",
        )

        When("toMessage 를 호출하면") {
            val message = group.toMessage()

            Then("[U-01] 단건 메시지로 변환된다 (bundled=false)") {
                message.bundled.shouldBeFalse()
            }
            Then("[U-02] 단건 메시지 본문에 원본 payload 가 포함된다") {
                message.content shouldContain "첫 댓글이 달렸어요"
            }
            Then("[U-03] 메시지에 userId·type·groupKey 가 그대로 전달된다") {
                message.userId shouldBe 1L
                message.type shouldBe NotificationType.POST_COMMENT
                message.groupKey shouldBe "post-100"
            }
        }
    }

    Given("같은 그룹의 알림이 여러 건(itemCount=5)인 NotificationGroup") {
        val group = NotificationGroup(
            userId = 2L,
            type = NotificationType.KEYWORD_INTEREST,
            groupKey = "한정판 운동화",
            itemCount = 5,
            samplePayload = "신규 물건 등록",
        )

        When("toMessage 를 호출하면") {
            val message = group.toMessage()

            Then("[U-04] 묶음 메시지로 변환된다 (bundled=true)") {
                message.bundled.shouldBeTrue()
            }
            Then("[U-05] 묶음 메시지 본문에 건수(5건)가 포함된다") {
                message.content shouldContain "5건"
            }
            Then("[U-06] 묶음 메시지 본문에는 개별 payload 를 노출하지 않는다") {
                message.content shouldNotContain "신규 물건 등록"
            }
        }
    }

    Given("itemCount 가 0 이하인 입력") {
        When("NotificationGroup 을 생성하면") {
            Then("[U-07] 도메인 불변식 위반으로 IllegalArgumentException 이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    NotificationGroup(
                        userId = 3L,
                        type = NotificationType.TICKET_REMINDER,
                        groupKey = "game-1",
                        itemCount = 0,
                        samplePayload = "리마인드",
                    )
                }
            }
        }
    }
})
