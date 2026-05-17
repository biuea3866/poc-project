package com.hrplatform.employee.infrastructure.person

import com.hrplatform.employee.domain.person.Person
import com.hrplatform.employee.domain.person.PersonRepository
import org.springframework.stereotype.Component

@Component
class PersonRepositoryImpl(
    private val jpaRepository: PersonJpaRepository,
) : PersonRepository {

    override fun save(person: Person): Person =
        jpaRepository.save(PersonEntity.fromDomain(person)).toDomain()

    override fun findById(id: Long): Person? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findAll(): List<Person> =
        jpaRepository.findAll().map { it.toDomain() }
}
