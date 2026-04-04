package com.example.notification.application.service

import com.example.notification.application.port.NotificationRuleWriter
import com.example.notification.domain.enums.Frequency
import com.example.notification.domain.enums.NotificationCategory
import com.example.notification.domain.enums.NotificationChannel
import com.example.notification.domain.enums.NotificationTriggerType
import com.example.notification.domain.enums.ScopeType
import com.example.notification.domain.model.NotificationRule
import com.example.notification.legacy.LegacyNotificationConfig
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 레거시 LegacyNotificationConfig -> notification_rules 이관 배치.
 *
 * 시스템 디폴트와 다른 설정만 이관한다.
 * dryRun 모드를 지원하여 실제 저장 없이 이관 대상을 확인할 수 있다.
 */
@Service
class MigrateLegacyConfigJob(
    private val ruleWriter: NotificationRuleWriter,
) {
    private val log = LoggerFactory.getLogger(MigrateLegacyConfigJob::class.java)

    data class MigrationResult(
        val totalConfigs: Int,
        val migratedCount: Int,
        val skippedCount: Int,
        val createdRules: List<NotificationRule>,
    )

    /**
     * 레거시 설정 목록을 신규 규칙으로 이관한다.
     *
     * @param legacyConfigs 이관 대상 레거시 설정
     * @param storeId 매장 ID (레거시에는 매장 개념이 없으므로 외부에서 지정)
     * @param dryRun true면 저장 없이 이관 대상만 반환
     */
    fun migrate(
        legacyConfigs: List<LegacyNotificationConfig>,
        storeId: Long,
        dryRun: Boolean = false,
    ): MigrationResult {
        val allCreatedRules = mutableListOf<NotificationRule>()
        var skippedCount = 0

        for (config in legacyConfigs) {
            val rules = convertToRules(config, storeId)
            if (rules.isEmpty()) {
                skippedCount++
                continue
            }

            allCreatedRules.addAll(rules)
        }

        if (!dryRun && allCreatedRules.isNotEmpty()) {
            ruleWriter.saveAll(allCreatedRules)
            log.info("Migrated {} rules from {} legacy configs", allCreatedRules.size, legacyConfigs.size)
        } else if (dryRun) {
            log.info("[DRY RUN] Would migrate {} rules from {} legacy configs", allCreatedRules.size, legacyConfigs.size)
        }

        return MigrationResult(
            totalConfigs = legacyConfigs.size,
            migratedCount = legacyConfigs.size - skippedCount,
            skippedCount = skippedCount,
            createdRules = allCreatedRules,
        )
    }

    /**
     * 시스템 디폴트와 다른 채널 설정만 규칙으로 변환한다.
     */
    internal fun convertToRules(config: LegacyNotificationConfig, storeId: Long): List<NotificationRule> {
        val channelStates = mapOf(
            NotificationChannel.EMAIL to config.emailEnabled,
            NotificationChannel.PUSH to config.pushEnabled,
            NotificationChannel.SMS to config.smsEnabled,
            NotificationChannel.IN_APP to config.inAppEnabled,
        )

        val rules = mutableListOf<NotificationRule>()

        for (triggerType in NotificationTriggerType.entries) {
            val category = triggerType.category
            for ((channel, enabled) in channelStates) {
                val systemDefault = category.isChannelEnabledByDefault(channel)
                if (enabled == systemDefault) continue

                rules.add(
                    NotificationRule.create(
                        storeId = storeId,
                        userId = config.userId,
                        scopeType = ScopeType.GLOBAL,
                        category = category,
                        triggerType = triggerType,
                        channel = channel,
                        enabled = enabled,
                        priority = triggerType.defaultPriority,
                        frequency = Frequency.IMMEDIATE,
                    ),
                )
            }
        }

        return rules
    }
}
