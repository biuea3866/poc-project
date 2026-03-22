package com.closet.bff.presentation

import com.closet.bff.client.NotificationServiceClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationProxyController(
    private val notificationClient: NotificationServiceClient,
) {

    // Notifications
    @GetMapping
    fun getNotifications(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ) = notificationClient.getNotifications(memberId, page, size)

    @GetMapping("/unread-count")
    fun getUnreadCount(@RequestHeader("X-Member-Id") memberId: Long) =
        notificationClient.getUnreadCount(memberId)

    @PatchMapping("/{id}/read")
    fun markAsRead(@PathVariable id: Long) =
        notificationClient.markAsRead(id)

    @PostMapping("/send")
    fun send(@RequestBody request: Any) =
        notificationClient.send(request)

    // Templates
    @GetMapping("/templates")
    fun getTemplates(@RequestParam type: String, @RequestParam channel: String) =
        notificationClient.getTemplates(type, channel)

    @PostMapping("/templates")
    fun createTemplate(@RequestBody request: Any) =
        notificationClient.createTemplate(request)

    @PutMapping("/templates/{id}")
    fun updateTemplate(@PathVariable id: Long, @RequestBody request: Any) =
        notificationClient.updateTemplate(id, request)

    // Restock Subscriptions
    @PostMapping("/restock-subscriptions")
    fun subscribe(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestBody request: Any,
    ) = notificationClient.subscribe(memberId, request)

    @DeleteMapping("/restock-subscriptions/{productOptionId}")
    fun unsubscribe(
        @RequestHeader("X-Member-Id") memberId: Long,
        @PathVariable productOptionId: Long,
    ): ResponseEntity<Void> {
        notificationClient.unsubscribe(memberId, productOptionId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/restock-subscriptions/my")
    fun getMySubscriptions(@RequestHeader("X-Member-Id") memberId: Long) =
        notificationClient.getMySubscriptions(memberId)
}
