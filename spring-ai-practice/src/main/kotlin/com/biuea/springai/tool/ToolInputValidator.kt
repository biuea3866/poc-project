package com.biuea.springai.tool

import org.springframework.stereotype.Component

/**
 * 도구 인자에 대한 화이트리스트/정규식 기반 입력 검증.
 *
 * MCP 도구는 외부 LLM 이 임의 문자열을 보내므로
 * SQL/프롬프트 인젝션 시도, 제어 문자, 비현실적 길이를 사전에 차단한다.
 */
@Component
class ToolInputValidator {

    private val productIdRegex = Regex("^P-\\d{1,8}$")
    private val orderIdRegex = Regex("^ORD-\\d{1,8}$")
    private val sizeRegex = Regex("^[A-Za-z0-9]{1,4}$") // S/M/L/XL/28/30...
    private val freeTextRegex = Regex("^[\\p{L}\\p{N} _.,\\-/]{0,60}$")
    private val maxPrice = 100_000_000
    private val allowedOrderStatuses = setOf("결제완료", "배송준비중", "배송중", "배송완료", "취소")

    fun requireProductId(value: String): String {
        val trimmed = value.trim()
        require(productIdRegex.matches(trimmed)) { "productId 형식이 올바르지 않습니다. 예: P-1001" }
        return trimmed
    }

    fun requireOrderId(value: String): String {
        val trimmed = value.trim()
        require(orderIdRegex.matches(trimmed)) { "orderId 형식이 올바르지 않습니다. 예: ORD-1001" }
        return trimmed
    }

    fun requireSize(value: String): String {
        val trimmed = value.trim()
        require(sizeRegex.matches(trimmed)) { "size 형식이 올바르지 않습니다. 영숫자 1~4자만 허용됩니다." }
        return trimmed.uppercase()
    }

    fun sanitizeFreeText(value: String?): String? {
        if (value.isNullOrBlank()) return null
        val trimmed = value.trim()
        require(freeTextRegex.matches(trimmed)) { "검색어에 허용되지 않은 문자가 포함되었습니다." }
        return trimmed
    }

    fun sanitizeCategory(value: String?): String? = sanitizeFreeText(value)

    fun sanitizeColor(value: String?): String? = sanitizeFreeText(value)

    fun sanitizeMaxPrice(value: Int?): Int? {
        if (value == null || value <= 0) return null
        require(value <= maxPrice) { "maxPrice 가 비정상적으로 큽니다." }
        return value
    }

    fun sanitizeOrderStatus(value: String?): String? {
        if (value.isNullOrBlank()) return null
        val trimmed = value.trim()
        require(trimmed in allowedOrderStatuses) {
            "허용되지 않는 주문 상태입니다. 허용: ${allowedOrderStatuses.joinToString(", ")}"
        }
        return trimmed
    }

    /** 주문 수량: 1~100 사이 정수. */
    fun requirePositiveQuantity(value: Int): Int {
        require(value in 1..100) { "수량은 1~100 사이여야 합니다." }
        return value
    }

    /** 재고 증감 폭: -100~+1000. 음수=출고, 양수=입고. 0 은 거부. */
    fun requireRestockDelta(value: Int): Int {
        require(value != 0) { "재고 변경량 0 은 의미가 없습니다." }
        require(value in -100..1000) { "재고 변경량은 -100 ~ +1000 사이여야 합니다." }
        return value
    }

    /** 배송 이벤트 status (자유 단어이지만 길이/문자 제한). */
    fun requireEventStatus(value: String): String {
        val trimmed = value.trim()
        require(freeTextRegex.matches(trimmed) && trimmed.isNotEmpty()) {
            "배송 이벤트 status 형식이 올바르지 않습니다. (한글/영문/숫자 1~60자)"
        }
        return trimmed
    }

    /** 배송 이벤트 location (자유 단어). */
    fun requireEventLocation(value: String): String {
        val trimmed = value.trim()
        require(freeTextRegex.matches(trimmed) && trimmed.isNotEmpty()) {
            "배송 이벤트 location 형식이 올바르지 않습니다. (한글/영문/숫자 1~60자)"
        }
        return trimmed
    }
}
