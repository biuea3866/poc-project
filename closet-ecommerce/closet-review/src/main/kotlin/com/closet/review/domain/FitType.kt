package com.closet.review.domain

/**
 * 사이즈 후기 핏 타입 (US-802).
 *
 * 구매한 상품의 사이즈 핏을 3단계로 평가한다.
 * - SMALL: 작아요
 * - PERFECT: 딱 맞아요
 * - LARGE: 커요
 */
enum class FitType {
    SMALL,
    PERFECT,
    LARGE,
}
