package com.hrplatform.employee.domain.person

import com.hrplatform.core.domain.AggregateRoot
import com.hrplatform.employee.domain.encryption.AesGcmStringConverter
import io.hypersistence.utils.hibernate.type.json.JsonStringType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import java.time.LocalDate
import java.time.Period

@Entity
@Table(name = "person")
@Suppress("LongParameterList")
class Person(
    @Column(nullable = false)
    var name: String,

    @Column(name = "personal_email", nullable = false)
    @Convert(converter = AesGcmStringConverter::class)
    var personalEmail: String,

    @Column(name = "phone_number")
    @Convert(converter = AesGcmStringConverter::class)
    var phoneNumber: String? = null,

    @Column(name = "birth_date")
    var birthDate: LocalDate? = null,

    @Column(name = "nationality", columnDefinition = "CHAR(2)")
    var nationality: String? = null,

    @Enumerated(EnumType.STRING)
    @Column
    var gender: Gender? = null,

    @Type(JsonStringType::class)
    @Column(name = "emergency_contacts", columnDefinition = "TEXT")
    var emergencyContacts: List<EmergencyContact> = emptyList(),
) : AggregateRoot() {

    init {
        validateNotMinor()
    }

    fun updateContact(personalEmail: String, phoneNumber: String?) {
        this.personalEmail = personalEmail
        this.phoneNumber = phoneNumber
    }

    fun updateEmergencyContacts(contacts: List<EmergencyContact>) {
        this.emergencyContacts = contacts
    }

    private fun validateNotMinor() {
        val birth = birthDate ?: return
        val age = Period.between(birth, LocalDate.now()).years
        if (age < 18) {
            throw MinorPersonNotAllowedException()
        }
    }
}
