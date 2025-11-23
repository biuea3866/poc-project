package com.example.filepractice.claude.dto

/**
 * 동적 컬럼 설정
 *
 * 사용자가 원하는 컬럼만 선택하여 다운로드할 수 있도록 하는 설정 클래스
 *
 * @property orderColumns 주문 정보에서 포함할 컬럼 목록
 * @property productColumns 상품 정보에서 포함할 컬럼 목록
 * @property couponColumns 쿠폰 정보에서 포함할 컬럼 목록
 */
data class ColumnConfig(
    val orderColumns: Set<OrderColumn> = OrderColumn.entries.toSet(),
    val productColumns: Set<ProductColumn> = ProductColumn.entries.toSet(),
    val couponColumns: Set<CouponColumn> = CouponColumn.entries.toSet()
) {
    /**
     * 주문 컬럼 열거형
     */
    enum class OrderColumn(val displayName: String) {
        ID("주문 ID"),
        ORDER_NUMBER("주문 번호"),
        USER_ID("사용자 ID"),
        TOTAL_AMOUNT("총 주문 금액"),
        DISCOUNTED_AMOUNT("할인 후 금액"),
        ORDER_DATE("주문 일시"),
        STATUS("주문 상태")
    }

    /**
     * 상품 컬럼 열거형
     */
    enum class ProductColumn(val displayName: String) {
        ID("상품 ID"),
        NAME("상품명"),
        PRICE("상품 가격"),
        QUANTITY("주문 수량"),
        CATEGORY("상품 카테고리")
    }

    /**
     * 쿠폰 컬럼 열거형
     */
    enum class CouponColumn(val displayName: String) {
        ID("쿠폰 ID"),
        CODE("쿠폰 코드"),
        NAME("쿠폰명"),
        DISCOUNT_RATE("할인율"),
        DISCOUNT_AMOUNT("할인 금액")
    }

    companion object {
        /**
         * 모든 컬럼을 포함하는 기본 설정
         */
        fun default() = ColumnConfig()

        /**
         * 예제: 가격 정보를 제외한 설정
         */
        fun withoutPriceInfo() = ColumnConfig(
            orderColumns = OrderColumn.entries.toSet() - setOf(
                OrderColumn.TOTAL_AMOUNT,
                OrderColumn.DISCOUNTED_AMOUNT
            ),
            productColumns = ProductColumn.entries.toSet() - setOf(ProductColumn.PRICE),
            couponColumns = CouponColumn.entries.toSet() - setOf(
                CouponColumn.DISCOUNT_RATE,
                CouponColumn.DISCOUNT_AMOUNT
            )
        )
    }
}
