package com.hrplatform.employee.domain.person

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDate
import java.time.ZonedDateTime

class PersonTest : BehaviorSpec({

    fun buildAdultPerson(
        name: String = "홍길동",
        personalEmail: String = "hong@example.com",
        birthDate: LocalDate = LocalDate.now().minusYears(25),
    ) = Person(
        name = name,
        personalEmail = personalEmail,
        birthDate = birthDate,
    )

    given("Person 생성 시") {

        `when`("만 18세 이상이면") {
            val person = buildAdultPerson(birthDate = LocalDate.now().minusYears(18))
            then("정상 생성된다") {
                person.name shouldBe "홍길동"
            }
        }

        `when`("만 18세 미만이면") {
            then("MinorPersonNotAllowedException 이 발생한다") {
                shouldThrow<MinorPersonNotAllowedException> {
                    Person(
                        name = "미성년자",
                        personalEmail = "minor@example.com",
                        birthDate = LocalDate.now().minusYears(17),
                    )
                }
            }
        }

        `when`("생년월일이 없으면") {
            then("성인 검증을 생략하고 정상 생성된다") {
                val person = Person(name = "이름없는", personalEmail = "no@example.com")
                person.birthDate shouldBe null
            }
        }

        `when`("오늘이 정확히 18번째 생일이면") {
            then("성인으로 간주해 정상 생성된다") {
                val person = buildAdultPerson(birthDate = LocalDate.now().minusYears(18))
                person.name shouldNotBe null
            }
        }
    }

    given("updateContact 호출 시") {
        val person = buildAdultPerson()

        `when`("새 이메일과 전화번호를 전달하면") {
            person.updateContact("new@example.com", "010-1234-5678")
            then("personalEmail 과 phoneNumber 가 변경된다") {
                person.personalEmail shouldBe "new@example.com"
                person.phoneNumber shouldBe "010-1234-5678"
            }
        }

        `when`("전화번호를 null 로 전달하면") {
            person.updateContact("other@example.com", null)
            then("phoneNumber 가 null 이 된다") {
                person.phoneNumber shouldBe null
            }
        }
    }

    given("updateEmergencyContacts 호출 시") {
        val person = buildAdultPerson()
        val contacts = listOf(
            EmergencyContact(name = "배우자", relation = "spouse", phone = "010-9999-0000"),
            EmergencyContact(name = "부모", relation = "parent", phone = "010-8888-7777"),
        )

        `when`("비상연락처 목록을 전달하면") {
            person.updateEmergencyContacts(contacts)
            then("emergencyContacts 가 교체된다") {
                person.emergencyContacts.size shouldBe 2
                person.emergencyContacts[0].name shouldBe "배우자"
            }
        }

        `when`("빈 목록을 전달하면") {
            person.updateEmergencyContacts(emptyList())
            then("emergencyContacts 가 비어 있다") {
                person.emergencyContacts.isEmpty() shouldBe true
            }
        }
    }

    given("softDelete 호출 시") {
        val person = buildAdultPerson()
        val now = ZonedDateTime.now()

        `when`("삭제된 적 없는 Person 이면") {
            person.softDelete(now, 1L)
            then("isDeleted 가 true 이고 deletedAt 이 설정된다") {
                person.isDeleted shouldBe true
                person.deletedAt shouldBe now
                person.deletedBy shouldBe 1L
            }
        }

        `when`("이미 삭제된 Person 에 다시 softDelete 를 호출하면") {
            then("예외가 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    person.softDelete(ZonedDateTime.now(), 2L)
                }
            }
        }
    }

    given("restore 호출 시") {
        `when`("소프트 삭제된 Person 을 복구하면") {
            val person = buildAdultPerson()
            person.softDelete(ZonedDateTime.now(), 1L)
            person.restore()
            then("isDeleted 가 false 이고 deletedAt 이 null 이다") {
                person.isDeleted shouldBe false
                person.deletedAt shouldBe null
            }
        }

        `when`("삭제되지 않은 Person 에 restore 를 호출하면") {
            val person = buildAdultPerson()
            then("예외가 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    person.restore()
                }
            }
        }
    }
})
