package com.closet.common.auth

/**
 * 회원 역할 enum.
 *
 * BUYER: 구매자 (기본값)
 * SELLER: 판매자 (송장 등록, 반품 처리 등)
 * ADMIN: 관리자 (블라인드, 전체 관리)
 */
enum class MemberRole {
    BUYER,
    SELLER,
    ADMIN,
    ;

    companion object {
        /** 문자열에서 MemberRole로 변환. 매칭 실패 시 BUYER 반환 (레거시 하위 호환) */
        fun fromStringOrDefault(value: String?): MemberRole {
            if (value.isNullOrBlank()) return BUYER
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                BUYER
            }
        }
    }
}
