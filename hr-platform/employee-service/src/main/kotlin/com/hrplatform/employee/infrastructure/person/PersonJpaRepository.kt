package com.hrplatform.employee.infrastructure.person

import com.hrplatform.employee.domain.person.Person
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PersonJpaRepository : JpaRepository<Person, Long>
