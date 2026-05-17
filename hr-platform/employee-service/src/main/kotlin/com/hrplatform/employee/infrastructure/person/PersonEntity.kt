package com.hrplatform.employee.infrastructure.person

import com.hrplatform.employee.domain.person.EmergencyContact
import com.hrplatform.employee.domain.person.Gender
import com.hrplatform.employee.domain.person.Person
import com.hrplatform.employee.infrastructure.encryption.AesGcmStringConverter
import io.hypersistence.utils.hibernate.type.json.JsonStringType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import java.time.LocalDate
import java.time.ZonedDateTime

@Entity
@Table(name = "person")
class PersonEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long? = null,

    @Column(name = "name", nullable = false, length = 100)
    val name: String,

    @Convert(converter = AesGcmStringConverter::class)
    @Column(name = "personal_email", nullable = false, columnDefinition = "VARBINARY(255)")
    val personalEmail: String,

    @Convert(converter = AesGcmStringConverter::class)
    @Column(name = "phone_number", columnDefinition = "VARBINARY(255)")
    val phoneNumber: String?,

    @Column(name = "birth_date")
    val birthDate: LocalDate?,

    @Column(name = "nationality", length = 2)
    val nationality: String?,

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 15)
    val gender: Gender?,

    @Type(JsonStringType::class)
    @Column(name = "emergency_contacts", columnDefinition = "TEXT")
    val emergencyContacts: List<EmergencyContact>,

    @Column(name = "created_at", nullable = false)
    val createdAt: ZonedDateTime,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: ZonedDateTime,
) {

    fun toDomain(): Person = Person(
        id = id,
        name = name,
        personalEmail = personalEmail,
        phoneNumber = phoneNumber,
        birthDate = birthDate,
        nationality = nationality,
        gender = gender,
        emergencyContacts = emergencyContacts,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun fromDomain(person: Person): PersonEntity = PersonEntity(
            id = person.id,
            name = person.name,
            personalEmail = person.personalEmail,
            phoneNumber = person.phoneNumber,
            birthDate = person.birthDate,
            nationality = person.nationality,
            gender = person.gender,
            emergencyContacts = person.emergencyContacts,
            createdAt = person.createdAt,
            updatedAt = person.updatedAt,
        )
    }
}
