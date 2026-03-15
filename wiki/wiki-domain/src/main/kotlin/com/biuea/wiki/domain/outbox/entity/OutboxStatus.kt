package com.biuea.wiki.domain.outbox.entity

enum class OutboxStatus {
    PENDING,
    SUCCESS,
    FAILED,
    DEAD_LETTER,
}
