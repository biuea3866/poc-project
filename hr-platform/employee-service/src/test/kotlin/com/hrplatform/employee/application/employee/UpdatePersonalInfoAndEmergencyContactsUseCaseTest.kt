package com.hrplatform.employee.application.employee

import com.hrplatform.core.exception.ForbiddenException
import com.hrplatform.employee.domain.employment.Employment
import com.hrplatform.employee.domain.employment.EmploymentDomainService
import com.hrplatform.employee.domain.person.EmergencyContact
import com.hrplatform.employee.domain.person.Person
import com.hrplatform.employee.domain.person.PersonDomainService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class UpdatePersonalInfoAndEmergencyContactsUseCaseTest : BehaviorSpec({

    val personDomainService = mockk<PersonDomainService>()
    val employmentDomainService = mockk<EmploymentDomainService>()
    val updatePersonalInfoUseCase = UpdatePersonalInfoUseCase(personDomainService, employmentDomainService)
    val updateEmergencyContactsUseCase = UpdateEmergencyContactsUseCase(personDomainService, employmentDomainService)

    given("UpdatePersonalInfoUseCase.execute") {
        `when`("본인(viewerEmploymentId의 personId == command.personId)이 호출하면") {
            val command = UpdatePersonalInfoCommand(
                viewerEmploymentId = 1L,
                personId = 10L,
                personalEmail = "updated@example.com",
                phoneNumber = "010-1234-5678",
            )

            val viewerEmployment = mockk<Employment> {
                every { personId } returns 10L
            }
            every { employmentDomainService.getById(1L) } returns viewerEmployment

            val updatedPerson = mockk<Person> {
                every { id } returns 10L
                every { personalEmail } returns "updated@example.com"
                every { phoneNumber } returns "010-1234-5678"
            }
            every {
                personDomainService.updateContact(personId = 10L, personalEmail = "updated@example.com", phoneNumber = "010-1234-5678")
            } returns updatedPerson

            then("PersonDomainService.updateContact를 정확히 1회 호출하고 결과를 반환한다") {
                val result = updatePersonalInfoUseCase.execute(command)

                verify(exactly = 1) {
                    personDomainService.updateContact(
                        personId = 10L,
                        personalEmail = "updated@example.com",
                        phoneNumber = "010-1234-5678",
                    )
                }
                result.personalEmail shouldBe "updated@example.com"
            }
        }

        `when`("본인 외 personId로 호출하면") {
            val command = UpdatePersonalInfoCommand(
                viewerEmploymentId = 1L,
                personId = 999L,
                personalEmail = "other@example.com",
                phoneNumber = null,
            )

            val viewerEmployment = mockk<Employment> {
                every { personId } returns 10L
            }
            every { employmentDomainService.getById(1L) } returns viewerEmployment

            then("ForbiddenException이 발생한다") {
                shouldThrow<ForbiddenException> {
                    updatePersonalInfoUseCase.execute(command)
                }
            }
        }
    }

    given("UpdateEmergencyContactsUseCase.execute") {
        val contacts = listOf(EmergencyContact(name = "보호자", relation = "부모", phone = "010-9999-8888"))

        `when`("본인(viewerEmploymentId의 personId == command.personId)이 호출하면") {
            val command = UpdateEmergencyContactsCommand(
                viewerEmploymentId = 1L,
                personId = 10L,
                contacts = contacts,
            )

            val viewerEmployment = mockk<Employment> {
                every { personId } returns 10L
            }
            every { employmentDomainService.getById(1L) } returns viewerEmployment

            val updatedPerson = mockk<Person> {
                every { id } returns 10L
                every { emergencyContacts } returns contacts
            }
            every {
                personDomainService.updateEmergencyContacts(personId = 10L, contacts = contacts)
            } returns updatedPerson

            then("PersonDomainService.updateEmergencyContacts를 정확히 1회 호출한다") {
                val result = updateEmergencyContactsUseCase.execute(command)

                verify(exactly = 1) {
                    personDomainService.updateEmergencyContacts(personId = 10L, contacts = contacts)
                }
                result.contacts shouldBe contacts
            }
        }

        `when`("본인 외 personId로 호출하면") {
            val command = UpdateEmergencyContactsCommand(
                viewerEmploymentId = 1L,
                personId = 999L,
                contacts = contacts,
            )

            val viewerEmployment = mockk<Employment> {
                every { personId } returns 10L
            }
            every { employmentDomainService.getById(1L) } returns viewerEmployment

            then("ForbiddenException이 발생한다") {
                shouldThrow<ForbiddenException> {
                    updateEmergencyContactsUseCase.execute(command)
                }
            }
        }
    }
})
