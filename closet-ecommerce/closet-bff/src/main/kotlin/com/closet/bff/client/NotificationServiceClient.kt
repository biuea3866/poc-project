package com.closet.bff.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(name = "notification-service", url = "\${service.notification.url}")
interface NotificationServiceClient {

    // Notifications
    @GetMapping("/notifications")
    fun getNotifications(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): Any

    @GetMapping("/notifications/unread-count")
    fun getUnreadCount(@RequestHeader("X-Member-Id") memberId: Long): Any

    @PatchMapping("/notifications/{id}/read")
    fun markAsRead(@PathVariable id: Long): Any

    @PostMapping("/notifications/send")
    fun send(@RequestBody request: Any): Any

    // Templates
    @GetMapping("/notifications/templates")
    fun getTemplates(@RequestParam type: String, @RequestParam channel: String): Any

    @PostMapping("/notifications/templates")
    fun createTemplate(@RequestBody request: Any): Any

    @PutMapping("/notifications/templates/{id}")
    fun updateTemplate(@PathVariable id: Long, @RequestBody request: Any): Any

    // Restock Subscriptions
    @PostMapping("/notifications/restock-subscriptions")
    fun subscribe(@RequestHeader("X-Member-Id") memberId: Long, @RequestBody request: Any): Any

    @DeleteMapping("/notifications/restock-subscriptions/{productOptionId}")
    fun unsubscribe(@RequestHeader("X-Member-Id") memberId: Long, @PathVariable productOptionId: Long)

    @GetMapping("/notifications/restock-subscriptions/my")
    fun getMySubscriptions(@RequestHeader("X-Member-Id") memberId: Long): Any
}
