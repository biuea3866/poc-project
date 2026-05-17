package com.hrplatform.core.domain

/**
 * 행위 직후 aggregate snapshot. 컨슈머가 별도 조회 없이 read model을 재구성 가능해야 함.
 *
 * status는 모든 이벤트에 반드시 포함. 그 외 필드는 도메인별 자유.
 */
interface DomainEventState {
    val status: String                      // 행위 직후 aggregate status (예: "ACTIVE")
    val snapshot: Map<String, Any?>         // 도메인별 추가 필드
}
