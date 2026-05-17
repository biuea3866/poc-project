package com.hrplatform.employee.domain.history

import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * 발령 이력 도메인 엔티티 — append-only.
 *
 * ADR-002 §3: 모든 Employment 변경을 append-only 로그로 보존한다.
 * - UPDATE 금지: setter/var 노출 없음. 단일 상태 변경은 markCancelled()로만 허용.
 * - 신규 생성: companion object factory(create)만 허용.
 * - DB 복원: infrastructure 모듈에서 internal constructor 직접 사용.
 * - cancelledAt: 발령 취소 보상 처리 시 기록. 직전 1건만 취소 가능 (검증은 DomainService).
 */
class EmploymentHistory internal constructor(
    val id: Long? = null,
    val employmentId: Long,
    val eventType: EmploymentHistoryEventType,
    val oldValue: Map<String, Any?>? = null,
    val newValue: Map<String, Any?>,
    val effectiveDate: LocalDate,
    val createdByEmploymentId: Long? = null,
    val note: String? = null,
    var cancelledAt: ZonedDateTime? = null,
    val createdAt: ZonedDateTime,
) {

    fun markCancelled(at: ZonedDateTime) {
        cancelledAt = at
    }

    companion object {
        fun create(
            employmentId: Long,
            eventType: EmploymentHistoryEventType,
            newValue: Map<String, Any?>,
            effectiveDate: LocalDate,
            createdAt: ZonedDateTime,
            oldValue: Map<String, Any?>? = null,
            createdByEmploymentId: Long? = null,
            note: String? = null,
        ): EmploymentHistory = EmploymentHistory(
            id = null,
            employmentId = employmentId,
            eventType = eventType,
            oldValue = oldValue,
            newValue = newValue,
            effectiveDate = effectiveDate,
            createdByEmploymentId = createdByEmploymentId,
            note = note,
            cancelledAt = null,
            createdAt = createdAt,
        )
    }
}
