package com.hrplatform.employee.domain.person

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.ZoneOffset

class PersonTest : BehaviorSpec({

    fun birthDateForAge(age: Int): LocalDate =
        LocalDate.now(ZoneOffset.UTC).minusYears(age.toLong())

    fun makePerson(
        birthDate: LocalDate? = birthDateForAge(25),
        phoneNumber: String? = "01012345678",
        personalEmail: String = "test@example.com",
    ): Person = Person(
        id = null,
        name = "홍길동",
        personalEmail = personalEmail,
        phoneNumber = phoneNumber,
        birthDate = birthDate,
        nationality = "KR",
        gender = Gender.MALE,
        emergencyContacts = emptyList(),
        createdAt = ZonedDateTime.now(ZoneOffset.UTC),
        updatedAt = ZonedDateTime.now(ZoneOffset.UTC),
    )

    given("만 18세 미만 생년월일을 가진 Person이 validateNotMinor()를 호출하면") {
        val person = makePerson(birthDate = birthDateForAge(17))
        `when`("validateNotMinor를 실행하면") {
            then("MinorPersonNotAllowedException이 발생한다") {
                shouldThrow<MinorPersonNotAllowedException> {
                    person.validateNotMinor()
                }
            }
        }
    }

    given("만 18세 생년월일을 정확히 맞춘 Person이 validateNotMinor()를 호출하면") {
        val person = makePerson(birthDate = birthDateForAge(18))
        `when`("validateNotMinor를 실행하면") {
            then("예외가 발생하지 않는다") {
                person.validateNotMinor()
            }
        }
    }

    given("만 30세 Person이 validateNotMinor()를 호출하면") {
        val person = makePerson(birthDate = birthDateForAge(30))
        `when`("validateNotMinor를 실행하면") {
            then("예외가 발생하지 않는다") {
                person.validateNotMinor()
            }
        }
    }

    given("birthDate가 null인 Person이 validateNotMinor()를 호출하면") {
        val person = makePerson(birthDate = null)
        `when`("validateNotMinor를 실행하면") {
            then("예외가 발생하지 않는다 (검증 생략)") {
                person.validateNotMinor()
            }
        }
    }

    given("Person이 존재하고") {
        val person = makePerson()
        `when`("updateContact(phone, email)을 호출하면") {
            val newPhone = "01099998888"
            val newEmail = "new@example.com"
            person.updateContact(phone = newPhone, email = newEmail)
            then("phoneNumber가 갱신된다") {
                person.phoneNumber shouldBe newPhone
            }
            then("personalEmail이 갱신된다") {
                person.personalEmail shouldBe newEmail
            }
        }
    }

    given("Person이 존재하고") {
        val person = makePerson()
        `when`("updateContact에서 phone을 null로 전달하면") {
            person.updateContact(phone = null, email = "another@example.com")
            then("phoneNumber가 null로 갱신된다") {
                person.phoneNumber shouldBe null
            }
        }
    }

    given("Person이 존재하고") {
        val person = makePerson()
        `when`("updateEmergencyContacts를 호출하면") {
            val contacts = listOf(
                EmergencyContact(name = "김철수", relation = "배우자", phone = "01011112222"),
                EmergencyContact(name = "김영희", relation = "부모", phone = "01033334444"),
            )
            person.updateEmergencyContacts(contacts)
            then("emergencyContacts가 갱신된다") {
                person.emergencyContacts shouldBe contacts
            }
        }
    }

    given("Person이 존재하고") {
        val person = makePerson(emergencyContacts = listOf(
            EmergencyContact(name = "기존연락처", relation = "부모", phone = "01000000000"),
        ))
        `when`("updateEmergencyContacts를 빈 리스트로 호출하면") {
            person.updateEmergencyContacts(emptyList())
            then("emergencyContacts가 빈 리스트로 갱신된다") {
                person.emergencyContacts shouldBe emptyList()
            }
        }
    }
}) {
    // makePersonWithContacts를 위한 보조 생성자
    companion object
}

private fun makePerson(emergencyContacts: List<EmergencyContact>): Person = Person(
    id = null,
    name = "홍길동",
    personalEmail = "test@example.com",
    phoneNumber = "01012345678",
    birthDate = LocalDate.now(ZoneOffset.UTC).minusYears(25),
    nationality = "KR",
    gender = Gender.MALE,
    emergencyContacts = emergencyContacts,
    createdAt = ZonedDateTime.now(ZoneOffset.UTC),
    updatedAt = ZonedDateTime.now(ZoneOffset.UTC),
)
