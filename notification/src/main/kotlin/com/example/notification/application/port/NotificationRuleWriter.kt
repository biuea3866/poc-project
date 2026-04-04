package com.example.notification.application.port

import com.example.notification.domain.model.NotificationRule

interface NotificationRuleWriter {
    fun save(rule: NotificationRule): NotificationRule
    fun saveAll(rules: List<NotificationRule>): List<NotificationRule>
    fun upsert(rule: NotificationRule): NotificationRule
}
