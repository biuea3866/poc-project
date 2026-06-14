package com.biuea.batch.application

import com.biuea.batch.domain.ReservedNotificationRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class ResetBatchStateUseCaseTest : BehaviorSpec({

    val repository = mockk<ReservedNotificationRepository>()
    val useCase = ResetBatchStateUseCase(repository)

    Given("sent=true 인 행이 일부 존재") {
        every { repository.resetSentFlags() } returns 42

        When("execute 를 호출하면") {
            val reset = useCase.execute()

            Then("[U-14] repository.resetSentFlags 가 1회 호출되고 갱신 건수를 반환한다") {
                verify(exactly = 1) { repository.resetSentFlags() }
                reset shouldBe 42
            }
        }
    }
})
