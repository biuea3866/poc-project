package com.closet.shipping.domain

/**
 * 반품/교환 사유.
 *
 * PD-11: 사유별 배송비 부담 매핑
 * - DEFECTIVE, WRONG_ITEM -> SELLER 부담 (0원)
 * - SIZE_MISMATCH, CHANGE_OF_MIND -> BUYER 부담 (3,000원)
 */
enum class ReturnReason(val description: String) {
    DEFECTIVE("불량"),
    WRONG_ITEM("오배송"),
    SIZE_MISMATCH("사이즈 불일치"),
    CHANGE_OF_MIND("단순변심"),
}
