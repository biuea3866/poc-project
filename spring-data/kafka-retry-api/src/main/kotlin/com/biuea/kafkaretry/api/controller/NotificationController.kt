package com.biuea.kafkaretry.api.controller

import com.biuea.kafkaretry.api.service.NotificationQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationQueryService: NotificationQueryService
) {
    @GetMapping
    fun listUnacknowledged(): ResponseEntity<Any> {
        return ResponseEntity.ok(notificationQueryService.getUnacknowledged())
    }

    @GetMapping("/topic/{topic}")
    fun listByTopic(@PathVariable topic: String): ResponseEntity<Any> {
        return ResponseEntity.ok(notificationQueryService.getByTopic(topic))
    }

    @PostMapping("/{id}/acknowledge")
    fun acknowledge(@PathVariable id: Long): ResponseEntity<Map<String, Any>> {
        notificationQueryService.acknowledge(id)
        return ResponseEntity.ok(mapOf("acknowledged" to true))
    }
}
