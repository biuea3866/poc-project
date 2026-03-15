package com.biuea.wiki.presentation.admin.outbox

import com.biuea.wiki.domain.outbox.OutboxService
import com.biuea.wiki.domain.outbox.entity.OutboxStatus
import com.biuea.wiki.presentation.admin.outbox.response.OutboxEventResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/outbox")
class OutboxAdminController(
    private val outboxService: OutboxService,
) {
    @GetMapping
    fun listOutboxEvents(
        @RequestParam(required = false) status: OutboxStatus?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ResponseEntity<Page<OutboxEventResponse>> {
        val page = if (status != null) {
            outboxService.findByStatus(status, pageable)
        } else {
            outboxService.findAll(pageable)
        }
        return ResponseEntity.ok(page.map { OutboxEventResponse.from(it) })
    }

    @PostMapping("/{id}/retry")
    fun retryOutboxEvent(@PathVariable id: Long): ResponseEntity<OutboxEventResponse> {
        val event = outboxService.resetForRetry(id)
        return ResponseEntity.ok(OutboxEventResponse.from(event))
    }
}
