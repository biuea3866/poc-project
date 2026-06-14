package com.biuea.batch.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class NotificationDispatchServiceTest : BehaviorSpec({

    val gateway = mockk<NotificationSendGateway>()
    val service = NotificationDispatchService(gateway)

    Given("묶음(itemCount>1) NotificationGroup") {
        val group = NotificationGroup(
            userId = 7L,
            type = NotificationType.KEYWORD_INTEREST,
            groupKey = "keyword-9",
            itemCount = 4,
            samplePayload = "신규 물건",
        )
        val captured = slot<NotificationMessage>()
        every { gateway.send(capture(captured)) } just Runs

        When("dispatch 를 호출하면") {
            service.dispatch(group)

            Then("[U-12] Gateway.send 가 1회 호출된다") {
                verify(exactly = 1) { gateway.send(any()) }
            }
            Then("[U-13] 전달된 메시지는 묶음 메시지(bundled=true)다") {
                captured.captured.bundled shouldBe true
            }
        }
    }
})
