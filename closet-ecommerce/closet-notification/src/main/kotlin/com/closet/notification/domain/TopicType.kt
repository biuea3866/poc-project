package com.closet.notification.domain

/**
 * 알림 토픽 유형.
 *
 * 회원이 구독할 수 있는 알림 대상의 종류를 정의한다.
 */
enum class TopicType {
    /** 특정 상품 알림 (재입고, 가격변동 등) */
    PRODUCT,

    /** 특정 카테고리 신상품/세일 알림 */
    CATEGORY,

    /** 특정 브랜드 신상품/세일 알림 */
    BRAND,

    /** 이벤트/기획전 알림 */
    EVENT,
}
