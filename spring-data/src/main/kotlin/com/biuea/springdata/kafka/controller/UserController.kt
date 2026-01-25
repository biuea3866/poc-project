package com.biuea.springdata.kafka.controller

import com.biuea.springdata.kafka.dto.User
import com.biuea.springdata.kafka.producer.UserEventProducer
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/kafka/users")
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class UserController(
    private val userEventProducer: UserEventProducer
) {

    @PostMapping("/send")
    fun sendUserEvent(@RequestBody user: User): ResponseEntity<Map<String, Any>> {
        userEventProducer.send(user)
        return ResponseEntity.ok(
            mapOf(
                "status" to "sent",
                "user" to user
            )
        )
    }

    @PostMapping("/send/bulk")
    fun sendBulkUserEvents(@RequestBody request: BulkUserRequest): ResponseEntity<Map<String, Any>> {
        repeat(request.count) { i ->
            val user = User(
                id = request.startId + i,
                name = "User-$i",
                email = "user$i@example.com"
            )
            userEventProducer.send(user)
        }
        return ResponseEntity.ok(
            mapOf(
                "status" to "sent",
                "count" to request.count
            )
        )
    }
}

data class BulkUserRequest(
    val count: Int = 10,
    val startId: Long = 1
)
