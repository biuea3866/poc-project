package com.hrplatform.employee.infrastructure.department

import com.hrplatform.employee.domain.department.Department
import com.hrplatform.employee.domain.department.DepartmentRepository
import com.hrplatform.employee.domain.department.QDepartment
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Repository
class DepartmentRepositoryImpl(
    private val jpaRepository: DepartmentJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : DepartmentRepository {

    override fun save(department: Department): Department =
        jpaRepository.save(department)

    override fun findById(id: Long): Department? =
        jpaRepository.findById(id).orElse(null)

    override fun findByPathPrefix(prefix: String): List<Department> {
        val q = QDepartment.department
        val deletedAtPath = Expressions.dateTimePath(ZonedDateTime::class.java, q, "deletedAt")
        return queryFactory.selectFrom(q)
            .where(
                q.path.like("$prefix%")
                    .and(deletedAtPath.isNull),
            )
            .fetch()
    }

    override fun findByParentId(parentId: Long?): List<Department> {
        val q = QDepartment.department
        val deletedAtPath = Expressions.dateTimePath(ZonedDateTime::class.java, q, "deletedAt")
        val parentCondition = if (parentId == null) {
            q.parentId.isNull
        } else {
            q.parentId.eq(parentId)
        }
        return queryFactory.selectFrom(q)
            .where(parentCondition.and(deletedAtPath.isNull))
            .fetch()
    }
}
