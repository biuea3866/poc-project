package com.hrplatform.core.domain

/**
 * 도메인 행위. type은 enum 또는 SNAKE_CASE 문자열, details는 행위별 가변 컨텍스트(이전값/입력값/사유).
 */
interface DomainEventAction {
    val type: String                        // 예: "HIRE", "TRANSFER_CANCELLED"
    val details: Map<String, Any?>          // 행위별 가변 필드
}
