package com.hrplatform.employee.domain.history

import com.hrplatform.core.domain.BaseEntity
import io.hypersistence.utils.hibernate.type.json.JsonStringType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import java.time.LocalDate
import java.time.ZonedDateTime

@Entity
@Table(name = "employment_history")
class EmploymentHistory internal constructor(
    @Column(name = "employment_id", nullable = false)
    val employmentId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    val eventType: EmploymentHistoryEventType,

    @Type(JsonStringType::class)
    @Column(name = "old_value", columnDefinition = "TEXT")
    val oldValue: Map<String, Any?>? = null,

    @Type(JsonStringType::class)
    @Column(name = "new_value", columnDefinition = "TEXT", nullable = false)
    val newValue: Map<String, Any?>,

    @Column(name = "effective_date", nullable = false)
    val effectiveDate: LocalDate,

    @Column(name = "created_by_employment_id")
    val createdByEmploymentId: Long? = null,

    @Column(length = 500)
    val note: String? = null,

    cancelledAtInit: ZonedDateTime? = null,
) : BaseEntity() {

    @Column(name = "cancelled_at")
    var cancelledAt: ZonedDateTime? = cancelledAtInit
        protected set

    fun markCancelled(at: ZonedDateTime) {
        check(cancelledAt == null) { "이미 취소된 이력입니다" }
        cancelledAt = at
    }

    companion object {
        fun create(
            employmentId: Long,
            eventType: EmploymentHistoryEventType,
            oldValue: Map<String, Any?>?,
            newValue: Map<String, Any?>,
            effectiveDate: LocalDate,
            createdByEmploymentId: Long? = null,
            note: String? = null,
        ): EmploymentHistory = EmploymentHistory(
            employmentId = employmentId,
            eventType = eventType,
            oldValue = oldValue,
            newValue = newValue,
            effectiveDate = effectiveDate,
            createdByEmploymentId = createdByEmploymentId,
            note = note,
        )
    }
}
