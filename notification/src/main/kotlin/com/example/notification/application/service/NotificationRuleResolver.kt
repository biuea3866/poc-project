// 패턴: 통합 알림 시스템의 규칙 결정 서비스
package com.example.notification.application.service

import com.example.notification.application.port.NotificationRuleReader
import com.example.notification.domain.enums.Frequency
import com.example.notification.domain.enums.NotificationChannel
import com.example.notification.domain.enums.NotificationTriggerType
import com.example.notification.domain.enums.ScopeType
import com.example.notification.domain.model.EffectiveRule
import com.example.notification.domain.model.NotificationRule

import org.springframework.stereotype.Component

/**
 * notification_rules에서 최종 적용 규칙(EffectiveRule)을 결정한다.
 *
 * 우선순위 결정 체계 (4계층 Preference):
 * 0. category.isMandatory -> 모든 채널 강제 ON
 * 1. Store Policy (user_id=NULL) -> 비활성화 시 개인 설정 무관 차단
 * 2. User 개인 설정 -> PRODUCT > STORE > GLOBAL 스코프 우선순위
 * 3. 모두 없으면 -> 시스템 디폴트 (category.defaultChannels)
 *
 * 역할 기반 디폴트:
 * - 매장 주인(isStoreOwner=true): 모든 트리거 활성화
 * - 구매자(isStoreOwner=false): SHIPMENT, REFUND만 활성화, 나머지 OFF
 */
@Component
class NotificationRuleResolver(
    private val notificationRuleReader: NotificationRuleReader,
) {
    fun resolve(
        userId: Long,
        storeId: Long,
        triggerType: NotificationTriggerType,
        productId: Long?,
        isStoreOwner: Boolean = false,
    ): List<EffectiveRule> {
        // 0단계: Mandatory 카테고리 → 모든 채널 강제 ON
        if (triggerType.category.isMandatory) {
            return NotificationChannel.entries.map { channel ->
                EffectiveRule(
                    channel = channel,
                    enabled = true,
                    priority = triggerType.defaultPriority,
                    frequency = Frequency.IMMEDIATE,
                )
            }
        }

        val policies = notificationRuleReader.findStorePolicies(storeId, triggerType)
        val userRules = notificationRuleReader.findUserRules(storeId, userId, triggerType)

        return NotificationChannel.entries.map { channel ->
            resolveForChannel(policies, userRules, channel, productId, storeId, triggerType, isStoreOwner)
        }
    }

    private fun resolveForChannel(
        policies: List<NotificationRule>,
        userRules: List<NotificationRule>,
        channel: NotificationChannel,
        productId: Long?,
        storeId: Long,
        triggerType: NotificationTriggerType,
        isStoreOwner: Boolean,
    ): EffectiveRule {
        // 1단계: Store Policy 확인 (비활성화 시 차단)
        val policy = policies.find { it.channel == channel }
        if (policy?.enabled == false) {
            return EffectiveRule(channel = channel, enabled = false)
        }

        // 2단계: User 개인 설정 (PRODUCT > STORE > GLOBAL)
        val channelRules = userRules.filter { it.channel == channel }

        val matched = channelRules.firstOrNull {
            it.scopeType == ScopeType.PRODUCT && it.scopeId == productId
        } ?: channelRules.firstOrNull {
            it.scopeType == ScopeType.STORE && it.scopeId == storeId
        } ?: channelRules.firstOrNull {
            it.scopeType == ScopeType.GLOBAL
        }

        if (matched != null) {
            return matched.toEffectiveRule()
        }

        // 3단계: 시스템 디폴트
        return EffectiveRule.systemDefault(channel, triggerType)
    }
}
