package com.hrplatform.employee.infrastructure.person

import com.hrplatform.employee.domain.person.EmergencyContact
import com.hrplatform.employee.domain.person.Gender
import com.hrplatform.employee.domain.person.Person
import com.hrplatform.employee.domain.person.PersonRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldHaveSize
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.ZoneOffset

/**
 * PersonRepositoryImpl 통합 테스트.
 *
 * Testcontainers MySQL 8.0 + Spring Boot Test로 실제 DB 연동을 검증한다.
 * 검증 항목:
 *   1) save / findById / findAll 기본 CRUD
 *   2) emergency_contacts JSON 컬럼 직렬화/역직렬화
 *   3) personal_email AES-GCM 암호화 round-trip
 */
@SpringBootTest
@ActiveProfiles("integration-test")
class PersonRepositoryImplTest(
    private val personRepository: PersonRepository,
) : BehaviorSpec({

    fun makePerson(email: String = "user${System.nanoTime()}@example.com"): Person = Person(
        id = null,
        name = "홍길동",
        personalEmail = email,
        phoneNumber = "01012345678",
        birthDate = LocalDate.of(1990, 1, 1),
        nationality = "KR",
        gender = Gender.MALE,
        emergencyContacts = emptyList(),
        createdAt = ZonedDateTime.now(ZoneOffset.UTC),
        updatedAt = ZonedDateTime.now(ZoneOffset.UTC),
    )

    given("Person을 save하면") {
        `when`("save()를 호출하면") {
            val saved = personRepository.save(makePerson())
            then("id가 채번된다") {
                saved.id.shouldNotBeNull()
            }
        }
    }

    given("저장된 Person이 있을 때") {
        val saved = personRepository.save(makePerson())

        `when`("findById()로 조회하면") {
            val found = personRepository.findById(saved.id!!)
            then("Person이 반환된다") {
                found.shouldNotBeNull()
                found.id shouldBe saved.id
                found.name shouldBe "홍길동"
            }
        }

        `when`("존재하지 않는 id로 findById()를 호출하면") {
            val notFound = personRepository.findById(99999L)
            then("null이 반환된다") {
                notFound.shouldBeNull()
            }
        }
    }

    given("여러 Person이 저장되어 있을 때") {
        personRepository.save(makePerson())
        personRepository.save(makePerson())

        `when`("findAll()을 호출하면") {
            val all = personRepository.findAll()
            then("1건 이상의 Person이 반환된다") {
                all.isNotEmpty() shouldBe true
            }
        }
    }

    given("emergency_contacts JSON 컬럼 직렬화 검증") {
        val contacts = listOf(
            EmergencyContact(name = "김철수", relation = "배우자", phone = "01011112222"),
            EmergencyContact(name = "김영희", relation = "부모", phone = "01033334444"),
        )
        val personWithContacts = Person(
            id = null,
            name = "테스트",
            personalEmail = "contacts${System.nanoTime()}@example.com",
            phoneNumber = null,
            birthDate = LocalDate.of(1985, 6, 15),
            nationality = "KR",
            gender = Gender.FEMALE,
            emergencyContacts = contacts,
            createdAt = ZonedDateTime.now(ZoneOffset.UTC),
            updatedAt = ZonedDateTime.now(ZoneOffset.UTC),
        )

        `when`("emergencyContacts가 포함된 Person을 저장 후 조회하면") {
            val saved = personRepository.save(personWithContacts)
            val found = personRepository.findById(saved.id!!)!!

            then("emergencyContacts가 올바르게 직렬화/역직렬화된다") {
                found.emergencyContacts shouldHaveSize 2
                found.emergencyContacts[0].name shouldBe "김철수"
                found.emergencyContacts[0].relation shouldBe "배우자"
                found.emergencyContacts[1].name shouldBe "김영희"
            }
        }
    }

    given("personal_email AES-GCM 암호화 round-trip 검증") {
        val plainEmail = "encrypted${System.nanoTime()}@secret.com"
        val person = makePerson(email = plainEmail)

        `when`("personalEmail이 있는 Person을 저장 후 조회하면") {
            val saved = personRepository.save(person)
            val found = personRepository.findById(saved.id!!)!!

            then("personalEmail이 복호화되어 원본 평문으로 반환된다") {
                found.personalEmail shouldBe plainEmail
            }
        }
    }
}) {
    override fun extensions() = listOf(SpringExtension)

    companion object {
        private val mysql: MySQLContainer<*> = MySQLContainer(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("employee_db")
            .withUsername("test")
            .withPassword("test")
            .withCommand(
                "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_unicode_ci",
            )
            .also { it.start() }

        @JvmStatic
        @DynamicPropertySource
        fun overrideProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") {
                mysql.jdbcUrl + "?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&allowPublicKeyRetrieval=true"
            }
            registry.add("spring.datasource.username") { mysql.username }
            registry.add("spring.datasource.password") { mysql.password }
            registry.add("spring.jpa.hibernate.ddl-auto") { "none" }
            registry.add("spring.flyway.enabled") { "true" }
            registry.add("spring.flyway.locations") { "classpath:db/migration" }
            registry.add("hrplatform.encryption.aes-key") {
                "dGVzdGtleXRlc3RrZXl0ZXN0a2V5dGVzdGtleXRlczE="
            }
        }
    }
}
