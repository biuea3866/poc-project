package com.example.notification.infrastructure.adapter

import com.example.notification.application.port.NotificationLogWriter
import com.example.notification.domain.model.NotificationLog
import com.example.notification.infrastructure.entity.NotificationLogEntity
import com.example.notification.infrastructure.repository.NotificationLogJpaRepository
import org.springframework.stereotype.Repository

@Repository
class NotificationLogWriterImpl(
    private val jpaRepository: NotificationLogJpaRepository,
) : NotificationLogWriter {

    override fun save(log: NotificationLog) {
        jpaRepository.save(NotificationLogEntity.from(log))
    }

    override fun existsByIdempotencyKey(idempotencyKey: String): Boolean =
        jpaRepository.existsByIdempotencyKey(idempotencyKey)
}
