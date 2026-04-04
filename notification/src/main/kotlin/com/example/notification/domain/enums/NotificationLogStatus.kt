// 패턴: 통합 알림 시스템의 발송 로그 상태
package com.example.notification.domain.enums

enum class NotificationLogStatus {
    PENDING,
    SENT,
    FAILED,
    SKIPPED,
}
