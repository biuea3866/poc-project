package com.hrplatform.employee.infrastructure.department

import com.hrplatform.employee.infrastructure.department.QDepartmentEntity.departmentEntity
import com.querydsl.jpa.impl.JPAQueryFactory

class DepartmentCustomRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : DepartmentCustomRepository {

    override fun findByPathPrefixQuery(prefix: String): List<DepartmentEntity> =
        queryFactory.selectFrom(departmentEntity)
                    .where(departmentEntity.path.like("$prefix%"))
                    .fetch()

    override fun findByParentIdQuery(parentId: Long): List<DepartmentEntity> =
        queryFactory.selectFrom(departmentEntity)
                    .where(departmentEntity.parentId.eq(parentId))
                    .fetch()
}
