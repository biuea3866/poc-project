package com.hrplatform.employee.domain.person

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate

class PersonDomainServiceTest : BehaviorSpec({

    val personRepository = mockk<PersonRepository>()
    val personDomainService = PersonDomainService(personRepository)

    given("findOrCreate") {
        `when`("동일 personalEmail의 Person이 존재하지 않으면") {
            every { personRepository.findByPersonalEmail("new@example.com") } returns null
            every { personRepository.save(any()) } answers { firstArg() }

            then("신규 Person을 생성해서 반환한다") {
                val result = personDomainService.findOrCreate(
                    personalEmail = "new@example.com",
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 1),
                    nationality = null,
                    gender = null,
                )
                result.personalEmail shouldBe "new@example.com"
                result.name shouldBe "홍길동"
                verify(exactly = 1) { personRepository.save(any()) }
            }
        }

        `when`("동일 personalEmail의 Person이 이미 존재하면") {
            val existingPerson = Person(
                name = "김철수",
                personalEmail = "existing@example.com",
                birthDate = LocalDate.of(1985, 6, 15),
            )
            every { personRepository.findByPersonalEmail("existing@example.com") } returns existingPerson
            every { personRepository.save(any()) } answers { firstArg() }

            then("기존 Person을 반환하고 이름을 갱신한다") {
                val result = personDomainService.findOrCreate(
                    personalEmail = "existing@example.com",
                    name = "김철수 갱신",
                    birthDate = LocalDate.of(1985, 6, 15),
                    nationality = null,
                    gender = null,
                )
                result.name shouldBe "김철수 갱신"
            }
        }
    }

    given("updateContact") {
        `when`("존재하는 personId로 연락처 변경을 요청하면") {
            val person = Person(
                name = "이영희",
                personalEmail = "old@example.com",
                phoneNumber = "010-0000-0000",
                birthDate = LocalDate.of(1992, 3, 20),
            )
            every { personRepository.findById(1L) } returns person
            every { personRepository.save(any()) } answers { firstArg() }

            then("personalEmail과 phoneNumber가 변경된다") {
                val result = personDomainService.updateContact(
                    personId = 1L,
                    personalEmail = "new@example.com",
                    phoneNumber = "010-1234-5678",
                )
                result.personalEmail shouldBe "new@example.com"
                result.phoneNumber shouldBe "010-1234-5678"
            }
        }

        `when`("존재하지 않는 personId로 요청하면") {
            every { personRepository.findById(999L) } returns null

            then("PersonNotFoundException이 발생한다") {
                shouldThrow<PersonNotFoundException> {
                    personDomainService.updateContact(
                        personId = 999L,
                        personalEmail = "test@example.com",
                        phoneNumber = null,
                    )
                }
            }
        }
    }

    given("updateEmergencyContacts") {
        `when`("존재하는 personId로 비상연락처 변경을 요청하면") {
            val person = Person(
                name = "박민준",
                personalEmail = "park@example.com",
                birthDate = LocalDate.of(1988, 11, 5),
            )
            every { personRepository.findById(2L) } returns person
            every { personRepository.save(any()) } answers { firstArg() }

            then("emergencyContacts가 교체된다") {
                val newContacts = listOf(
                    EmergencyContact(name = "부모님", relation = "PARENT", phone = "010-9999-8888"),
                )
                val result = personDomainService.updateEmergencyContacts(
                    personId = 2L,
                    contacts = newContacts,
                )
                result.emergencyContacts shouldBe newContacts
            }
        }
    }
})
