package com.closet.common.kafka.outbox

enum class OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED,
}
