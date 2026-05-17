package com.hrplatform.employee.infrastructure.person

import com.hrplatform.employee.domain.person.EmergencyContact
import com.hrplatform.employee.domain.person.Gender
import com.hrplatform.employee.domain.person.Person
import com.hrplatform.employee.support.BaseIntegrationTest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.ZonedDateTime

class PersonRepositoryImplTest(
    @Autowired private val personRepositoryImpl: PersonRepositoryImpl,
    @Autowired private val personJpaRepository: PersonJpaRepository,
) : BaseIntegrationTest() {

    init {
        beforeEach {
            personJpaRepository.deleteAll()
        }

        given("PersonRepositoryImpl.save - 신규 Person 저장") {

            then("id, createdAt, updatedAt 이 자동 설정되고 AES/JSON 라운드트립이 성공한다") {
                val person = Person(
                    name = "홍길동",
                    personalEmail = "hong@example.com",
                    phoneNumber = "010-1234-5678",
                    birthDate = LocalDate.now().minusYears(30),
                    nationality = "KR",
                    gender = Gender.MALE,
                    emergencyContacts = listOf(
                        EmergencyContact(name = "배우자", relation = "spouse", phone = "010-0000-0000"),
                    ),
                )
                val saved = personRepositoryImpl.save(person)

                saved.id shouldNotBe null
                saved.createdAt shouldNotBe null
                saved.updatedAt shouldNotBe null
                saved.createdBy shouldBe null

                val found = requireNotNull(personRepositoryImpl.findById(requireNotNull(saved.id)))
                found.personalEmail shouldBe "hong@example.com"
                found.emergencyContacts.size shouldBe 1
                found.emergencyContacts[0].name shouldBe "배우자"
            }
        }

        given("PersonRepositoryImpl.save - 소프트 삭제 저장") {

            then("deletedAt 컬럼이 저장된다") {
                val person = Person(
                    name = "삭제테스트",
                    personalEmail = "delete@example.com",
                )
                val saved = personRepositoryImpl.save(person)
                saved.softDelete(ZonedDateTime.now(), null)
                val deleted = personRepositoryImpl.save(saved)

                val found = requireNotNull(personRepositoryImpl.findById(requireNotNull(deleted.id)))
                found.isDeleted shouldBe true
                found.deletedAt shouldNotBe null
            }
        }

        given("PersonRepositoryImpl.findById") {

            then("존재하는 id 로 조회하면 Person 이 반환된다") {
                val saved = personRepositoryImpl.save(Person(name = "조회테스트", personalEmail = "find@example.com"))
                val found = personRepositoryImpl.findById(requireNotNull(saved.id))
                found shouldNotBe null
                found!!.name shouldBe "조회테스트"
            }

            then("존재하지 않는 id 로 조회하면 null 이 반환된다") {
                val found = personRepositoryImpl.findById(99999L)
                found shouldBe null
            }
        }

        given("PersonRepositoryImpl.findByPersonalEmailHash") {

            then("BE-05 구현 전 stub 이므로 null 을 반환한다") {
                val result = personRepositoryImpl.findByPersonalEmailHash("any-hash")
                result shouldBe null
            }
        }
    }
}
