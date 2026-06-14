package com.biuea.batch.application

import com.biuea.batch.domain.NotificationDispatchService
import com.biuea.batch.domain.NotificationGroup
import com.biuea.batch.domain.NotificationType
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify

class SendNotificationGroupUseCaseTest : BehaviorSpec({

    val dispatchService = mockk<NotificationDispatchService>()
    val useCase = SendNotificationGroupUseCase(dispatchService)

    Given("발송 단위(묶음) 1건") {
        val group = NotificationGroup(
            userId = 1L,
            type = NotificationType.POST_COMMENT,
            groupKey = "post-1",
            itemCount = 3,
            samplePayload = "댓글",
        )
        every { dispatchService.dispatch(group) } just Runs

        When("execute 를 호출하면") {
            val result = useCase.execute(group)

            Then("[U-10] DomainService.dispatch 가 정확히 1회 호출된다") {
                verify(exactly = 1) { dispatchService.dispatch(group) }
            }
            Then("[U-11] 발송한 group 을 그대로 반환한다(Writer 갱신 대상)") {
                result shouldBe group
            }
        }
    }
})
