package com.hrplatform.employee.domain.department

import com.hrplatform.core.event.DomainEventPublisher
import com.hrplatform.employee.domain.employment.EmploymentNotFoundException
import com.hrplatform.employee.domain.employment.EmploymentRepository
import com.hrplatform.employee.domain.employment.EmploymentStatus
import org.springframework.stereotype.Service
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Service
class DepartmentDomainService(
    private val departmentRepository: DepartmentRepository,
    private val employmentRepository: EmploymentRepository,
    private val eventPublisher: DomainEventPublisher,
) {

    fun create(command: CreateDepartmentCommand): Department {
        val parent = command.parentId?.let {
            departmentRepository.findById(it) ?: throw DepartmentNotFoundException()
        }
        val tempPath = if (parent == null) "/0/" else "${parent.path}0/"
        val department = Department(
            companyId = command.companyId,
            name = command.name,
            code = command.code,
            parentId = command.parentId,
            path = tempPath,
            orderNo = command.orderNo,
            effectiveFrom = command.effectiveFrom,
        )
        val saved = departmentRepository.save(department)
        val finalPath = if (parent == null) "/${saved.id}/" else "${parent.path}${saved.id}/"
        saved.path = finalPath
        return departmentRepository.save(saved)
    }

    fun moveTo(departmentId: Long, newParentId: Long?, actorEmploymentId: Long?): Department {
        val department = departmentRepository.findById(departmentId) ?: throw DepartmentNotFoundException()
        val newParent = newParentId?.let {
            departmentRepository.findById(it) ?: throw DepartmentNotFoundException()
        }
        val oldPath = department.path
        department.moveTo(newParent, actorEmploymentId, ZonedDateTime.now(ZoneOffset.UTC))
        updateDescendantPaths(oldPath, department.path, department.id ?: departmentId)
        val events = department.pullDomainEvents()
        val saved = departmentRepository.save(department)
        if (events.isNotEmpty()) eventPublisher.publishAll(events)
        return saved
    }

    fun assignHead(departmentId: Long, employmentId: Long, actorEmploymentId: Long?): Department {
        val department = departmentRepository.findById(departmentId) ?: throw DepartmentNotFoundException()
        val head = employmentRepository.findById(employmentId) ?: throw EmploymentNotFoundException()
        if (head.status != EmploymentStatus.ACTIVE) throw IneligibleHeadException()
        department.assignHead(employmentId, actorEmploymentId, ZonedDateTime.now(ZoneOffset.UTC))
        val events = department.pullDomainEvents()
        val saved = departmentRepository.save(department)
        if (events.isNotEmpty()) eventPublisher.publishAll(events)
        return saved
    }

    fun removeHead(departmentId: Long, actorEmploymentId: Long?): Department {
        val department = departmentRepository.findById(departmentId) ?: throw DepartmentNotFoundException()
        department.removeHead(actorEmploymentId, ZonedDateTime.now(ZoneOffset.UTC))
        val events = department.pullDomainEvents()
        val saved = departmentRepository.save(department)
        if (events.isNotEmpty()) eventPublisher.publishAll(events)
        return saved
    }

    private fun updateDescendantPaths(oldPath: String, newPath: String, movedId: Long) {
        val descendants = departmentRepository.findByPathPrefix(oldPath)
        descendants
            .filter { it.id != movedId }
            .forEach { child ->
                child.path = child.path.replaceFirst(oldPath, newPath)
                departmentRepository.save(child)
            }
    }
}
