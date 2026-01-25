package com.biuea.springdata.kafka.controller

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/kafka/admin")
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class KafkaAdminController(
    private val kafkaListenerEndpointRegistry: KafkaListenerEndpointRegistry
) {

    @GetMapping("/listeners")
    fun getListeners(): ResponseEntity<List<ListenerInfo>> {
        val listeners = kafkaListenerEndpointRegistry.listenerContainerIds.map { id ->
            val container = kafkaListenerEndpointRegistry.getListenerContainer(id)
            ListenerInfo(
                id = id,
                running = container?.isRunning ?: false,
                paused = container?.isContainerPaused ?: false,
                groupId = container?.groupId
            )
        }
        return ResponseEntity.ok(listeners)
    }

    @PostMapping("/listeners/{listenerId}/pause")
    fun pauseListener(@PathVariable listenerId: String): ResponseEntity<Map<String, Any>> {
        val container = kafkaListenerEndpointRegistry.getListenerContainer(listenerId)
            ?: return ResponseEntity.notFound().build()

        container.pause()
        return ResponseEntity.ok(
            mapOf(
                "listenerId" to listenerId,
                "status" to "paused"
            )
        )
    }

    @PostMapping("/listeners/{listenerId}/resume")
    fun resumeListener(@PathVariable listenerId: String): ResponseEntity<Map<String, Any>> {
        val container = kafkaListenerEndpointRegistry.getListenerContainer(listenerId)
            ?: return ResponseEntity.notFound().build()

        container.resume()
        return ResponseEntity.ok(
            mapOf(
                "listenerId" to listenerId,
                "status" to "resumed"
            )
        )
    }

    @PostMapping("/listeners/{listenerId}/start")
    fun startListener(@PathVariable listenerId: String): ResponseEntity<Map<String, Any>> {
        val container = kafkaListenerEndpointRegistry.getListenerContainer(listenerId)
            ?: return ResponseEntity.notFound().build()

        if (!container.isRunning) {
            container.start()
        }
        return ResponseEntity.ok(
            mapOf(
                "listenerId" to listenerId,
                "status" to "started"
            )
        )
    }

    @PostMapping("/listeners/{listenerId}/stop")
    fun stopListener(@PathVariable listenerId: String): ResponseEntity<Map<String, Any>> {
        val container = kafkaListenerEndpointRegistry.getListenerContainer(listenerId)
            ?: return ResponseEntity.notFound().build()

        if (container.isRunning) {
            container.stop()
        }
        return ResponseEntity.ok(
            mapOf(
                "listenerId" to listenerId,
                "status" to "stopped"
            )
        )
    }

    @PostMapping("/listeners/pause-all")
    fun pauseAllListeners(): ResponseEntity<Map<String, Any>> {
        kafkaListenerEndpointRegistry.listenerContainerIds.forEach { id ->
            kafkaListenerEndpointRegistry.getListenerContainer(id)?.pause()
        }
        return ResponseEntity.ok(
            mapOf(
                "status" to "all paused",
                "count" to kafkaListenerEndpointRegistry.listenerContainerIds.size
            )
        )
    }

    @PostMapping("/listeners/resume-all")
    fun resumeAllListeners(): ResponseEntity<Map<String, Any>> {
        kafkaListenerEndpointRegistry.listenerContainerIds.forEach { id ->
            kafkaListenerEndpointRegistry.getListenerContainer(id)?.resume()
        }
        return ResponseEntity.ok(
            mapOf(
                "status" to "all resumed",
                "count" to kafkaListenerEndpointRegistry.listenerContainerIds.size
            )
        )
    }

    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<KafkaHealthStatus> {
        val containers = kafkaListenerEndpointRegistry.listenerContainerIds.mapNotNull { id ->
            kafkaListenerEndpointRegistry.getListenerContainer(id)
        }

        val allRunning = containers.all { it.isRunning }
        val anyPaused = containers.any { it.isContainerPaused }

        return ResponseEntity.ok(
            KafkaHealthStatus(
                healthy = allRunning,
                totalListeners = containers.size,
                runningListeners = containers.count { it.isRunning },
                pausedListeners = containers.count { it.isContainerPaused }
            )
        )
    }
}

data class ListenerInfo(
    val id: String,
    val running: Boolean,
    val paused: Boolean,
    val groupId: String?
)

data class KafkaHealthStatus(
    val healthy: Boolean,
    val totalListeners: Int,
    val runningListeners: Int,
    val pausedListeners: Int
)
