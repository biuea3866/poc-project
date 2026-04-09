package com.closet.review.domain

/**
 * 사이즈 후기 핏 타입 (US-802).
 *
 * 구매한 상품의 사이즈 핏을 3단계로 평가한다.
 * - SMALL: 작아요
 * - PERFECT: 딱 맞아요
 * - LARGE: 커요
 *
 * Note: closet-product의 FitType(OVERSIZED/REGULAR/SLIM)과 이름 충돌을 피하기 위해
 * SizeFit으로 명명. DB 컬럼(fit_type VARCHAR)에는 동일한 값(SMALL/PERFECT/LARGE)이 저장된다.
 */
enum class SizeFit {
    SMALL,
    PERFECT,
    LARGE,
}
