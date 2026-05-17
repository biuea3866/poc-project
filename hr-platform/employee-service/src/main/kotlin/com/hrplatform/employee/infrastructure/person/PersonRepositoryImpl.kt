package com.hrplatform.employee.infrastructure.person

import com.hrplatform.employee.domain.person.Person
import com.hrplatform.employee.domain.person.PersonRepository
import org.springframework.stereotype.Repository

@Repository
class PersonRepositoryImpl(
    private val personJpaRepository: PersonJpaRepository,
) : PersonRepository {

    override fun save(person: Person): Person = personJpaRepository.save(person)

    override fun findById(id: Long): Person? = personJpaRepository.findById(id).orElse(null)

    override fun findByPersonalEmailHash(hash: String): Person? = null
}
