package com.hrplatform.employee.infrastructure.department

import com.hrplatform.employee.domain.department.Department
import jakarta.persistence.EntityManager
import java.util.Optional
import org.springframework.data.domain.Example
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.query.FluentQuery
import java.util.function.Function

/**
 * 통합 테스트용 DepartmentJpaRepository 어댑터.
 * Spring 컨텍스트 없이 EntityManager를 직접 주입해 사용합니다.
 */
class InMemoryDepartmentJpaRepository(private val em: EntityManager) : DepartmentJpaRepository {

    override fun <S : Department> save(entity: S): S {
        if (entity.id == null) {
            em.persist(entity)
        } else {
            em.merge(entity)
        }
        return entity
    }

    override fun findById(id: Long): Optional<Department> =
        Optional.ofNullable(em.find(Department::class.java, id))

    override fun findAll(): List<Department> =
        em.createQuery("SELECT d FROM Department d", Department::class.java).resultList

    override fun <S : Department> saveAll(entities: Iterable<S>): List<S> = entities.map { save(it) }

    override fun existsById(id: Long): Boolean = em.find(Department::class.java, id) != null

    override fun findAll(sort: Sort): List<Department> = findAll()

    override fun findAll(pageable: Pageable): Page<Department> = PageImpl(findAll())

    override fun findAllById(ids: Iterable<Long>): List<Department> =
        ids.mapNotNull { em.find(Department::class.java, it) }

    override fun count(): Long =
        em.createQuery("SELECT COUNT(d) FROM Department d", Long::class.javaObjectType).singleResult

    override fun deleteById(id: Long) {
        em.find(Department::class.java, id)?.let { em.remove(it) }
    }

    override fun delete(entity: Department) {
        em.remove(if (em.contains(entity)) entity else em.merge(entity))
    }

    override fun deleteAllById(ids: Iterable<Long>) = ids.forEach { deleteById(it) }

    override fun deleteAll(entities: Iterable<Department>) = entities.forEach { delete(it) }

    override fun deleteAll() {
        em.createQuery("DELETE FROM Department").executeUpdate()
    }

    override fun flush() = em.flush()

    override fun <S : Department> saveAndFlush(entity: S): S = save(entity).also { em.flush() }

    override fun <S : Department> saveAllAndFlush(entities: Iterable<S>): List<S> =
        saveAll(entities).also { em.flush() }

    override fun deleteAllInBatch(entities: Iterable<Department>) = deleteAll(entities)

    override fun deleteAllByIdInBatch(ids: Iterable<Long>) = deleteAllById(ids)

    override fun deleteAllInBatch() = deleteAll()

    override fun getReferenceById(id: Long): Department =
        em.getReference(Department::class.java, id)

    @Deprecated("Deprecated in Java")
    override fun getOne(id: Long): Department = getReferenceById(id)

    @Deprecated("Deprecated in Java")
    override fun getById(id: Long): Department = getReferenceById(id)

    override fun <S : Department> findOne(example: Example<S>): Optional<S> = Optional.empty()

    override fun <S : Department> findAll(example: Example<S>): List<S> = emptyList()

    override fun <S : Department> findAll(example: Example<S>, sort: Sort): List<S> = emptyList()

    override fun <S : Department> findAll(example: Example<S>, pageable: Pageable): Page<S> =
        PageImpl(emptyList())

    override fun <S : Department> count(example: Example<S>): Long = 0

    override fun <S : Department> exists(example: Example<S>): Boolean = false

    override fun <S : Department, R : Any> findBy(
        example: Example<S>,
        queryFunction: Function<FluentQuery.FetchableFluentQuery<S>, R>,
    ): R = throw UnsupportedOperationException("findBy with FluentQuery is not supported in test adapter")
}
