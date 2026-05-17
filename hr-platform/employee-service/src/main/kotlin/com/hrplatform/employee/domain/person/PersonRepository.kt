package com.hrplatform.employee.domain.person

interface PersonRepository {
    fun save(person: Person): Person
    fun findById(id: Long): Person?
    fun findByPersonalEmailHash(hash: String): Person?
}
