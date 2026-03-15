package com.biuea.wiki.presentation.notification

import com.biuea.wiki.domain.auth.AuthenticatedUser
import com.biuea.wiki.notification.NotificationAppService
import com.biuea.wiki.notification.NotificationSseManager
import com.biuea.wiki.presentation.notification.response.NotificationPageResponse
import com.biuea.wiki.presentation.notification.response.NotificationResponse
import com.biuea.wiki.presentation.notification.response.UnreadCountResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val notificationAppService: NotificationAppService,
    private val notificationSseManager: NotificationSseManager,
) {
    @GetMapping
    fun getNotifications(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "false") unreadOnly: Boolean,
        authentication: Authentication,
    ): ResponseEntity<NotificationPageResponse> {
        val user = authentication.principal as AuthenticatedUser
        val result = notificationAppService.getNotifications(user.id, page, size, unreadOnly)
        return ResponseEntity.ok(
            NotificationPageResponse(
                content = result.content.map { NotificationResponse.from(it) },
                page = result.number,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            )
        )
    }

    @GetMapping("/unread-count")
    fun getUnreadCount(authentication: Authentication): ResponseEntity<UnreadCountResponse> {
        val user = authentication.principal as AuthenticatedUser
        val count = notificationAppService.getUnreadCount(user.id)
        return ResponseEntity.ok(UnreadCountResponse(count = count))
    }

    @PatchMapping("/{id}/read")
    fun markAsRead(
        @PathVariable id: Long,
        authentication: Authentication,
    ): ResponseEntity<Void> {
        val user = authentication.principal as AuthenticatedUser
        notificationAppService.markAsRead(id, user.id)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/read-all")
    fun markAllAsRead(authentication: Authentication): ResponseEntity<Void> {
        val user = authentication.principal as AuthenticatedUser
        notificationAppService.markAllAsRead(user.id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun stream(authentication: Authentication): SseEmitter {
        val user = authentication.principal as AuthenticatedUser
        return notificationSseManager.subscribe(user.id)
    }
}
