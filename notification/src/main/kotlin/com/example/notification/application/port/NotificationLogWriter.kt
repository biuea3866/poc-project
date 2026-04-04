// 패턴: 통합 알림 시스템의 로그 저장 포트
package com.example.notification.application.port

import com.example.notification.domain.model.NotificationLog

interface NotificationLogWriter {
    fun save(log: NotificationLog)
    fun existsByIdempotencyKey(idempotencyKey: String): Boolean
}
