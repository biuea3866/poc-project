// 패턴: 통합 알림 시스템의 레거시 브릿지
package com.example.notification.application.service

import com.example.notification.domain.enums.NotificationTriggerType
import com.example.notification.domain.model.NotificationEvent
import com.example.notification.domain.model.NotificationPayload
import org.springframework.stereotype.Component

/**
 * 레거시 알림 -> V2 알림 전환 브릿지.
 *
 * 기존 코드에서 Mail/Push/SMS 3개 이벤트를 발행하기 전에
 * tryPublishV2()를 호출한다.
 * - true 반환: V2로 발행 완료 -> 레거시 발행 스킵
 * - false 반환: Flag OFF -> 레거시 발행 계속
 *
 * 사용 예:
 * ```
 * if (!bridge.tryPublishV2(...)) {
 *     // 레거시 3개 이벤트 발행
 *     legacyUseCase.notifyOrderPlaced(order)
 * }
 * ```
 */
@Component
class NotificationLegacyBridge(
    private val orchestrator: NotificationOrchestrator,
    private val featureFlags: FeatureFlagService,
) {
    fun tryPublishV2(
        triggerType: NotificationTriggerType,
        storeId: Long,
        orderId: Long?,
        productId: Long? = null,
        actorUserId: Long? = null,
        payload: NotificationPayload,
    ): Boolean {
        if (!featureFlags.isEnabled(triggerType, storeId)) {
            return false
        }

        orchestrator.handle(
            NotificationEvent(
                triggerType = triggerType,
                storeId = storeId,
                orderId = orderId,
                productId = productId,
                actorUserId = actorUserId,
                payload = payload,
            )
        )

        println(
            "NotificationEvent published via LegacyBridge: trigger=$triggerType, storeId=$storeId, orderId=$orderId"
        )

        return true
    }
}
