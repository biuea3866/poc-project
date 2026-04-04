package com.example.notification.application.service

import com.example.notification.application.port.NotificationRuleReader
import com.example.notification.application.port.NotificationRuleWriter
import com.example.notification.domain.enums.Frequency
import com.example.notification.domain.enums.NotificationCategory
import com.example.notification.domain.enums.NotificationChannel
import com.example.notification.domain.enums.NotificationTriggerType
import com.example.notification.domain.enums.ScopeType
import com.example.notification.domain.model.NotificationRule
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 첫 진입 시 디폴트 알림 규칙을 영속화한다.
 *
 * - initializeIfAbsent: GLOBAL 스코프 규칙 생성 (유저 최초 진입 시)
 * - initializeProcessRules: PRODUCT 스코프 규칙 생성 (상품별)
 */
@Service
class NotificationRuleInitializer(
    private val ruleReader: NotificationRuleReader,
    private val ruleWriter: NotificationRuleWriter,
) {

    /**
     * 유저의 GLOBAL 스코프 규칙이 없으면 생성한다.
     *
     * @param userId 유저 ID
     * @param storeId 매장 ID
     * @param isStoreOwner true면 모든 카테고리/채널 활성화, false면 시스템 디폴트 적용
     */
    @Transactional
    fun initializeIfAbsent(userId: Long, storeId: Long, isStoreOwner: Boolean) {
        if (ruleReader.existsByUserAndStore(userId, storeId)) {
            return
        }

        val rules = buildGlobalRules(userId, storeId, isStoreOwner)
        ruleWriter.saveAll(rules)
    }

    /**
     * 상품별 PRODUCT 스코프 규칙을 생성한다.
     *
     * @param userId 유저 ID
     * @param storeId 매장 ID
     * @param productId 상품 ID
     * @param isStoreOwner true면 모든 상품 알림 활성화, false면 비활성화
     */
    @Transactional
    fun initializeProcessRules(userId: Long, storeId: Long, productId: Long, isStoreOwner: Boolean) {
        val rules = buildProductRules(userId, storeId, productId, isStoreOwner)
        ruleWriter.saveAll(rules)
    }

    private fun buildGlobalRules(userId: Long, storeId: Long, isStoreOwner: Boolean): List<NotificationRule> {
        return NotificationTriggerType.entries.flatMap { triggerType ->
            val category = triggerType.category
            if (!category.supportsScopeType(ScopeType.GLOBAL)) return@flatMap emptyList()

            NotificationChannel.entries.map { channel ->
                val enabled = if (isStoreOwner) {
                    true
                } else {
                    category.isChannelEnabledByDefault(channel)
                }

                NotificationRule.create(
                    storeId = storeId,
                    userId = userId,
                    scopeType = ScopeType.GLOBAL,
                    category = category,
                    triggerType = triggerType,
                    channel = channel,
                    enabled = enabled,
                    priority = triggerType.defaultPriority,
                    frequency = Frequency.IMMEDIATE,
                )
            }
        }
    }

    private fun buildProductRules(
        userId: Long,
        storeId: Long,
        productId: Long,
        isStoreOwner: Boolean,
    ): List<NotificationRule> {
        return NotificationTriggerType.entries
            .filter { it.category.supportsScopeType(ScopeType.PRODUCT) }
            .flatMap { triggerType ->
                NotificationChannel.entries.map { channel ->
                    NotificationRule.create(
                        storeId = storeId,
                        userId = userId,
                        scopeType = ScopeType.PRODUCT,
                        scopeId = productId,
                        category = triggerType.category,
                        triggerType = triggerType,
                        channel = channel,
                        enabled = isStoreOwner,
                        priority = triggerType.defaultPriority,
                        frequency = Frequency.IMMEDIATE,
                    )
                }
            }
    }
}
