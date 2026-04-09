package com.closet.member.domain.point

import com.closet.member.domain.MemberGrade
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZonedDateTime

@Entity
@Table(name = "point_policy")
@EntityListeners(AuditingEntityListener::class)
class PointPolicy(
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val eventType: PointEventType,
    @Enumerated(EnumType.STRING)
    @Column(name = "grade_type", length = 30, columnDefinition = "VARCHAR(30)")
    val gradeType: MemberGrade? = null,
    @Column(name = "point_amount")
    val pointAmount: Int? = null,
    @Column(name = "point_rate", columnDefinition = "DECIMAL(5,2)")
    val pointRate: BigDecimal? = null,
    @Column(name = "description", nullable = false, length = 500)
    val description: String,
    @Column(name = "is_active", nullable = false, columnDefinition = "TINYINT(1)")
    var isActive: Boolean = true,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: ZonedDateTime

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    lateinit var updatedAt: ZonedDateTime

    /**
     * 적립금 계산.
     * gradeType이 null이면 전체 등급에 적용 (기존 GradeType.ALL 대체).
     */
    fun calculatePoint(
        orderAmount: BigDecimal,
        grade: MemberGrade,
    ): Int {
        if (!isActive) return 0
        if (gradeType != null && gradeType != grade) return 0

        return when {
            pointAmount != null -> pointAmount
            pointRate != null ->
                orderAmount
                    .multiply(pointRate)
                    .divide(BigDecimal(100), 0, RoundingMode.DOWN)
                    .toInt()
            else -> 0
        }
    }
}
