package com.hrplatform.employee.domain.person

import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PersonDomainService(
    private val personRepository: PersonRepository,
) {
    fun findOrCreate(
        personalEmail: String,
        name: String,
        birthDate: LocalDate?,
        nationality: String?,
        gender: Gender?,
    ): Person {
        val existing = personRepository.findByPersonalEmail(personalEmail)
        if (existing != null) {
            existing.name = name
            return personRepository.save(existing)
        }
        val newPerson = Person(
            name = name,
            personalEmail = personalEmail,
            birthDate = birthDate,
            nationality = nationality,
            gender = gender,
        )
        return personRepository.save(newPerson)
    }

    fun updateContact(personId: Long, personalEmail: String, phoneNumber: String?): Person {
        val person = personRepository.findById(personId) ?: throw PersonNotFoundException(personId)
        person.updateContact(personalEmail, phoneNumber)
        return personRepository.save(person)
    }

    fun updateEmergencyContacts(personId: Long, contacts: List<EmergencyContact>): Person {
        val person = personRepository.findById(personId) ?: throw PersonNotFoundException(personId)
        person.updateEmergencyContacts(contacts)
        return personRepository.save(person)
    }
}
