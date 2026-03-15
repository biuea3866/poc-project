package com.biuea.wiki.domain.notification

import com.biuea.wiki.domain.notification.entity.Notification
import com.biuea.wiki.domain.notification.entity.NotificationType
import com.biuea.wiki.domain.notification.service.NotificationService
import com.biuea.wiki.infrastructure.notification.NotificationRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.Optional

class NotificationServiceTest {

    private lateinit var notificationRepository: NotificationRepository
    private lateinit var notificationService: NotificationService

    @BeforeEach
    fun setUp() {
        notificationRepository = mock()
        notificationService = NotificationService(notificationRepository)
    }

    private fun createNotification(
        id: Long = 1L,
        type: NotificationType = NotificationType.AI_COMPLETED,
        targetUserId: Long = 10L,
        refId: Long = 100L,
        message: String = "테스트 알림",
        isRead: Boolean = false,
    ): Notification = Notification(
        type = type,
        targetUserId = targetUserId,
        refId = refId,
        message = message,
        isRead = isRead,
        id = id,
    )

    // TC-1: 알림 생성 + 미읽음 카운트 증가
    @Test
    fun `알림을 생성하면 저장소에 저장된다`() {
        val notification = createNotification()
        whenever(notificationRepository.save(any<Notification>())).thenReturn(notification)

        val result = notificationService.createNotification(
            type = NotificationType.AI_COMPLETED,
            targetUserId = 10L,
            refId = 100L,
            message = "AI 완료 알림",
        )

        assertNotNull(result)
        assertEquals(NotificationType.AI_COMPLETED, result.type)
        verify(notificationRepository).save(any<Notification>())
    }

    @Test
    fun `미읽음 카운트를 조회하면 올바른 숫자를 반환한다`() {
        whenever(notificationRepository.countByTargetUserIdAndIsReadFalse(10L)).thenReturn(5L)

        val count = notificationService.getUnreadCount(10L)

        assertEquals(5L, count)
        verify(notificationRepository).countByTargetUserIdAndIsReadFalse(10L)
    }

    // TC-2: 단건 읽음 처리
    @Test
    fun `알림 읽음 처리하면 isRead가 true가 된다`() {
        val notification = createNotification(id = 1L, targetUserId = 10L, isRead = false)
        whenever(notificationRepository.findById(1L)).thenReturn(Optional.of(notification))
        whenever(notificationRepository.save(any<Notification>())).thenReturn(notification)

        notificationService.markAsRead(1L, 10L)

        assertEquals(true, notification.isRead)
        assertNotNull(notification.readAt)
        verify(notificationRepository).save(notification)
    }

    @Test
    fun `존재하지 않는 알림 읽음 처리 시 예외가 발생한다`() {
        whenever(notificationRepository.findById(999L)).thenReturn(Optional.empty())

        assertThrows(IllegalArgumentException::class.java) {
            notificationService.markAsRead(999L, 10L)
        }
    }

    // TC-3: 전체 읽음 처리
    @Test
    fun `전체 읽음 처리하면 markAllAsReadByUserId가 호출된다`() {
        whenever(notificationRepository.markAllAsReadByUserId(10L)).thenReturn(3)

        notificationService.markAllAsRead(10L)

        verify(notificationRepository).markAllAsReadByUserId(10L)
    }

    // TC-4: 다른 유저의 알림 읽음 처리 거부
    @Test
    fun `다른 유저의 알림 읽음 처리 시도 시 예외가 발생한다`() {
        val notification = createNotification(id = 1L, targetUserId = 10L, isRead = false)
        whenever(notificationRepository.findById(1L)).thenReturn(Optional.of(notification))

        assertThrows(IllegalArgumentException::class.java) {
            notificationService.markAsRead(1L, 99L) // 다른 userId
        }
    }

    @Test
    fun `알림 목록 조회 시 페이지네이션이 적용된다`() {
        val notifications = listOf(
            createNotification(id = 1L),
            createNotification(id = 2L),
        )
        val page = PageImpl(notifications, PageRequest.of(0, 20), 2)
        whenever(notificationRepository.findByTargetUserIdOrderByCreatedAtDesc(10L, PageRequest.of(0, 20)))
            .thenReturn(page)

        val result = notificationService.getNotifications(10L, 0, 20, false)

        assertEquals(2, result.totalElements)
        assertEquals(2, result.content.size)
    }

    @Test
    fun `unreadOnly 파라미터로 미읽음 알림만 조회된다`() {
        val notifications = listOf(
            createNotification(id = 1L, isRead = false),
        )
        val page = PageImpl(notifications, PageRequest.of(0, 20), 1)
        whenever(notificationRepository.findByTargetUserIdAndIsReadFalseOrderByCreatedAtDesc(10L, PageRequest.of(0, 20)))
            .thenReturn(page)

        val result = notificationService.getNotifications(10L, 0, 20, unreadOnly = true)

        assertEquals(1, result.totalElements)
        assertEquals(false, result.content[0].isRead)
    }

    @Test
    fun `알림 생성 시 올바른 필드가 저장된다`() {
        val captor = argumentCaptor<Notification>()
        val savedNotification = createNotification(
            type = NotificationType.AI_FAILED,
            targetUserId = 20L,
            refId = 200L,
            message = "AI 실패 알림",
        )
        whenever(notificationRepository.save(captor.capture())).thenReturn(savedNotification)

        notificationService.createNotification(
            type = NotificationType.AI_FAILED,
            targetUserId = 20L,
            refId = 200L,
            message = "AI 실패 알림",
        )

        val captured = captor.firstValue
        assertEquals(NotificationType.AI_FAILED, captured.type)
        assertEquals(20L, captured.targetUserId)
        assertEquals(200L, captured.refId)
        assertEquals("AI 실패 알림", captured.message)
        assertEquals(false, captured.isRead)
    }
}
