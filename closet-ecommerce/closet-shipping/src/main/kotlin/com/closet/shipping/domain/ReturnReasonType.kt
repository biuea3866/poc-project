package com.closet.shipping.domain

enum class ReturnReasonType(val displayName: String) {
    CHANGE_OF_MIND("단순 변심"),
    DEFECT("상품 불량"),
    WRONG_ITEM("오배송"),
    SIZE_MISMATCH("사이즈 불일치");
}
