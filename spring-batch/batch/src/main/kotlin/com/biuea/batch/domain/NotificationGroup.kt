package com.biuea.batch.domain

/**
 * 발송 단위(묶음). (userId, type, groupKey) 로 묶인 예약 알림의 집계 결과를 표현하는 Rich Domain Model.
 * 불변이며, 묶음/단건 메시지 결정 규칙을 자체 캡슐화한다.
 *
 * - itemCount == 1 → 단건 메시지
 * - itemCount  > 1 → 묶음 메시지
 */
class NotificationGroup(
    val userId: Long,
    val type: NotificationType,
    val groupKey: String,
    val itemCount: Int,
    val samplePayload: String,
) {
    init {
        require(itemCount >= 1) { "itemCount must be >= 1, but was $itemCount" }
    }

    val bundled: Boolean
        get() = itemCount > 1

    fun toMessage(): NotificationMessage {
        val content =
            if (bundled) {
                type.bundledContent(groupKey, itemCount)
            } else {
                type.singleContent(groupKey, samplePayload)
            }
        return NotificationMessage(
            userId = userId,
            type = type,
            groupKey = groupKey,
            content = content,
            bundled = bundled,
        )
    }
}
