package com.closet.common.event

/**
 * Closet 플랫폼 Kafka 토픽 상수.
 *
 * 토픽은 도메인 단위로 통합한다. (event.closet.{domain})
 * 동일 도메인의 이벤트는 하나의 토픽에 발행하여 파티션 키 기반 순서 보장을 유지한다.
 * 이벤트 유형은 메시지 내 eventType 필드로 구분한다.
 */
object ClosetTopics {

    const val ORDER = "event.closet.order"
    const val PRODUCT = "event.closet.product"
    const val INVENTORY = "event.closet.inventory"
    const val SHIPPING = "event.closet.shipping"
    const val REVIEW = "event.closet.review"
    const val PAYMENT = "event.closet.payment"
    const val MEMBER = "event.closet.member"
    const val NOTIFICATION = "event.closet.notification"
}
