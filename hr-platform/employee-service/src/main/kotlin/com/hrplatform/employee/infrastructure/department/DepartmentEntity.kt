package com.hrplatform.employee.infrastructure.department

import com.hrplatform.employee.domain.department.Department
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.ZonedDateTime

@Entity
@Table(name = "department")
class DepartmentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Column(name = "name", nullable = false, length = 100)
    var name: String,

    @Column(name = "code", nullable = false, length = 50)
    val code: String,

    @Column(name = "parent_id")
    var parentId: Long?,

    @Column(name = "path", nullable = false, length = 500)
    var path: String,

    @Column(name = "head_employment_id")
    var headEmploymentId: Long?,

    @Column(name = "order_no", nullable = false)
    var orderNo: Int,

    @Column(name = "effective_from", nullable = false)
    val effectiveFrom: LocalDate,

    @Column(name = "effective_to")
    val effectiveTo: LocalDate?,

    @Column(name = "created_at", nullable = false)
    val createdAt: ZonedDateTime,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: ZonedDateTime,
) {
    fun toDomain(): Department = Department(
        id = id,
        companyId = companyId,
        name = name,
        code = code,
        parentId = parentId,
        path = path,
        headEmploymentId = headEmploymentId,
        orderNo = orderNo,
        effectiveFrom = effectiveFrom,
        effectiveTo = effectiveTo,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun fromDomain(department: Department): DepartmentEntity = DepartmentEntity(
            id = department.id,
            companyId = department.companyId,
            name = department.name,
            code = department.code,
            parentId = department.parentId,
            path = department.path,
            headEmploymentId = department.headEmploymentId,
            orderNo = department.orderNo,
            effectiveFrom = department.effectiveFrom,
            effectiveTo = department.effectiveTo,
            createdAt = department.createdAt,
            updatedAt = department.updatedAt,
        )
    }
}
