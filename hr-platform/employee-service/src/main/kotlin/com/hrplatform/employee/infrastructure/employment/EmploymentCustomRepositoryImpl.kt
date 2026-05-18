package com.hrplatform.employee.infrastructure.employment

import com.hrplatform.employee.domain.department.QDepartment
import com.hrplatform.employee.domain.employment.Employment
import com.hrplatform.employee.domain.employment.EmploymentStatus
import com.hrplatform.employee.domain.employment.QEmployment
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.ZonedDateTime

class EmploymentCustomRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : EmploymentCustomRepository {

    private val employment = QEmployment.employment
    private val department = QDepartment.department

    override fun findByCompanyIdAndEmployeeNumber(companyId: Long, employeeNumber: String): Employment? {
        val deletedAtPath = Expressions.dateTimePath(ZonedDateTime::class.java, employment, "deletedAt")
        return queryFactory.selectFrom(employment)
            .where(
                employment.companyId.eq(companyId),
                employment.employeeNumber.eq(employeeNumber),
                deletedAtPath.isNull,
            )
            .fetchOne()
    }

    override fun findByPersonId(personId: Long): List<Employment> {
        val deletedAtPath = Expressions.dateTimePath(ZonedDateTime::class.java, employment, "deletedAt")
        return queryFactory.selectFrom(employment)
            .where(
                employment.personId.eq(personId),
                deletedAtPath.isNull,
            )
            .fetch()
    }

    override fun findByDepartmentTreePath(pathPrefix: String): List<Employment> {
        val employmentDeletedAt = Expressions.dateTimePath(ZonedDateTime::class.java, employment, "deletedAt")
        val departmentId = Expressions.numberPath(Long::class.java, department, "id")
        val departmentDeletedAt = Expressions.dateTimePath(ZonedDateTime::class.java, department, "deletedAt")
        return queryFactory.selectFrom(employment)
            .join(department)
            .on(
                employment.departmentId.eq(departmentId),
                department.path.like("$pathPrefix%"),
                departmentDeletedAt.isNull,
            )
            .where(employmentDeletedAt.isNull)
            .fetch()
    }

    override fun findManagedBy(managerEmploymentId: Long): List<Employment> {
        val deletedAtPath = Expressions.dateTimePath(ZonedDateTime::class.java, employment, "deletedAt")
        return queryFactory.selectFrom(employment)
            .where(
                employment.managerEmploymentId.eq(managerEmploymentId),
                deletedAtPath.isNull,
            )
            .fetch()
    }

    override fun findByCompanyIdAndStatus(companyId: Long, status: EmploymentStatus): List<Employment> {
        val deletedAtPath = Expressions.dateTimePath(ZonedDateTime::class.java, employment, "deletedAt")
        return queryFactory.selectFrom(employment)
            .where(
                employment.companyId.eq(companyId),
                employment.status.eq(status),
                deletedAtPath.isNull,
            )
            .fetch()
    }

    override fun findByCompanyIdWithPage(
        companyId: Long,
        departmentPathPrefix: String?,
        employmentId: Long?,
        pageable: Pageable,
    ): Page<Employment> {
        val deletedAtPath = Expressions.dateTimePath(ZonedDateTime::class.java, employment, "deletedAt")
        val pathPrefixCondition: BooleanExpression? = departmentPathPrefix?.let {
            val departmentId = Expressions.numberPath(Long::class.java, department, "id")
            val departmentDeletedAt = Expressions.dateTimePath(ZonedDateTime::class.java, department, "deletedAt")
            employment.departmentId.`in`(
                queryFactory.select(departmentId)
                    .from(department)
                    .where(
                        department.path.like("$it%"),
                        departmentDeletedAt.isNull,
                    ),
            )
        }
        val employmentIdCondition: BooleanExpression? = employmentId?.let {
            Expressions.numberPath(Long::class.java, employment, "id").eq(it)
        }
        val whereConditions = arrayOf(
            employment.companyId.eq(companyId),
            deletedAtPath.isNull,
            pathPrefixCondition,
            employmentIdCondition,
        )
        val total = queryFactory.select(employment.count())
            .from(employment)
            .where(*whereConditions)
            .fetchOne() ?: 0L
        val content = queryFactory.selectFrom(employment)
            .where(*whereConditions)
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()
        return PageImpl(content, pageable, total)
    }
}
