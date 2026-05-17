package com.hrplatform.employee.domain.person

import com.hrplatform.core.domain.AggregateRoot
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset
import java.time.ZonedDateTime

class Person(
    id: Long?,
    val name: String,
    personalEmail: String,
    phoneNumber: String?,
    val birthDate: LocalDate?,
    val nationality: String?,
    val gender: Gender?,
    emergencyContacts: List<EmergencyContact>,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
) : AggregateRoot(id, createdAt, updatedAt) {

    var personalEmail: String = personalEmail
        private set

    var phoneNumber: String? = phoneNumber
        private set

    var emergencyContacts: List<EmergencyContact> = emergencyContacts
        private set

    fun validateNotMinor() {
        val date = birthDate ?: return
        val age = Period.between(date, LocalDate.now(ZoneOffset.UTC)).years
        if (age < MINIMUM_AGE) {
            throw MinorPersonNotAllowedException()
        }
    }

    fun updateContact(phone: String?, email: String) {
        phoneNumber = phone
        personalEmail = email
    }

    fun updateEmergencyContacts(contacts: List<EmergencyContact>) {
        emergencyContacts = contacts
    }

    companion object {
        private const val MINIMUM_AGE = 18
    }
}
