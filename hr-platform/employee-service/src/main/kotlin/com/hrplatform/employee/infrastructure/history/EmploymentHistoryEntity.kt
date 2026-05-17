package com.hrplatform.employee.infrastructure.history

import com.hrplatform.employee.domain.history.EmploymentHistoryEventType
import io.hypersistence.utils.hibernate.type.json.JsonStringType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import java.time.LocalDate
import java.time.ZonedDateTime

@Entity
@Table(name = "employment_history")
class EmploymentHistoryEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "employment_id", nullable = false)
    val employmentId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    val eventType: EmploymentHistoryEventType,

    @Type(JsonStringType::class)
    @Column(name = "old_value", columnDefinition = "TEXT")
    val oldValue: Map<String, Any?>? = null,

    @Type(JsonStringType::class)
    @Column(name = "new_value", nullable = false, columnDefinition = "TEXT")
    val newValue: Map<String, Any?>,

    @Column(name = "effective_date", nullable = false)
    val effectiveDate: LocalDate,

    @Column(name = "created_by_employment_id")
    val createdByEmploymentId: Long? = null,

    @Column(name = "note", length = 500)
    val note: String? = null,

    @Column(name = "cancelled_at")
    var cancelledAt: ZonedDateTime? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: ZonedDateTime,
)
