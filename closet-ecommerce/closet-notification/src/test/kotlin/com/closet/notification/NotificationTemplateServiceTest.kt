package com.closet.notification

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.notification.application.NotificationTemplateService
import com.closet.notification.domain.Notification
import com.closet.notification.domain.NotificationChannel
import com.closet.notification.domain.NotificationTemplate
import com.closet.notification.domain.NotificationType
import com.closet.notification.domain.repository.NotificationRepository
import com.closet.notification.domain.repository.NotificationTemplateRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.LocalDateTime

class NotificationTemplateServiceTest : BehaviorSpec({
    val templateRepository = mockk<NotificationTemplateRepository>()
    val notificationRepository = mockk<NotificationRepository>()
    val templateService = NotificationTemplateService(templateRepository, notificationRepository)

    Given("템플릿 렌더링 후 발송 요청이 주어졌을 때") {
        val template = NotificationTemplate(
            type = NotificationType.ORDER,
            channel = NotificationChannel.EMAIL,
            titleTemplate = "주문 확인 — {{orderNumber}}",
            contentTemplate = "{{memberName}}님, 주문번호 {{orderNumber}} 주문이 접수되었습니다.",
        ).apply {
            createdAt = LocalDateTime.now()
            updatedAt = LocalDateTime.now()
        }

        val variables = mapOf(
            "orderNumber" to "ORD-20260322-001",
            "memberName" to "홍길동",
        )

        When("활성화된 템플릿이 존재하면") {
            every {
                templateRepository.findByTypeAndChannelAndIsActiveTrueAndDeletedAtIsNull(
                    NotificationType.ORDER, NotificationChannel.EMAIL
                )
            } returns template

            val notificationSlot = slot<Notification>()
            every { notificationRepository.save(capture(notificationSlot)) } answers {
                notificationSlot.captured.apply {
                    createdAt = LocalDateTime.now()
                    updatedAt = LocalDateTime.now()
                }
            }

            val result = templateService.renderAndSend(
                type = NotificationType.ORDER,
                channel = NotificationChannel.EMAIL,
                memberId = 1L,
                variables = variables,
            )

            Then("변수가 치환된 알림이 생성된다") {
                result.title shouldContain "ORD-20260322-001"
                result.content shouldContain "홍길동"
                result.content shouldContain "ORD-20260322-001"
                result.type shouldBe NotificationType.ORDER
                result.channel shouldBe NotificationChannel.EMAIL
            }
        }

        When("활성화된 템플릿이 없으면") {
            every {
                templateRepository.findByTypeAndChannelAndIsActiveTrueAndDeletedAtIsNull(
                    NotificationType.MARKETING, NotificationChannel.SMS
                )
            } returns null

            Then("ENTITY_NOT_FOUND 예외가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    templateService.renderAndSend(
                        type = NotificationType.MARKETING,
                        channel = NotificationChannel.SMS,
                        memberId = 1L,
                        variables = emptyMap(),
                    )
                }
                exception.errorCode shouldBe ErrorCode.ENTITY_NOT_FOUND
            }
        }
    }
})
