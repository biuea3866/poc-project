package com.biuea.kafkaretry.api.controller

import com.biuea.kafkaretry.api.service.ManualRetryService
import com.biuea.kafkaretry.common.entity.OutboxStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/retry")
class RetryController(
    private val manualRetryService: ManualRetryService
) {
    @PostMapping("/{id}")
    fun retryMessage(@PathVariable id: Long): ResponseEntity<Map<String, Any>> {
        val result = manualRetryService.retryById(id)
        return ResponseEntity.ok(mapOf("id" to id, "status" to result))
    }

    @GetMapping("/failed")
    fun listFailedMessages(
        @RequestParam(required = false) status: OutboxStatus?
    ): ResponseEntity<Any> {
        val failedMessages = manualRetryService.listFailedMessages(status)
        return ResponseEntity.ok(failedMessages)
    }

    @PostMapping("/retry-all")
    fun retryAllPendingMessages(): ResponseEntity<Map<String, Any>> {
        val retriedCount = manualRetryService.retryAllPendingMessages()
        return ResponseEntity.ok(mapOf("retriedCount" to retriedCount))
    }
}
