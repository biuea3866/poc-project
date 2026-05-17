package com.hrplatform.employee.infrastructure.history

import com.hrplatform.employee.domain.history.EmploymentHistory
import com.hrplatform.employee.domain.history.EmploymentHistoryEventType
import com.hrplatform.employee.domain.history.EmploymentHistoryRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * EmploymentHistoryRepositoryImpl 통합 테스트.
 *
 * Testcontainers MySQL 8.0 싱글턴 + SpringBootTest로 실제 DB 동작을 검증한다.
 * 검증 항목:
 *   1) findByEmploymentId: effectiveDate DESC + id DESC 정렬
 *   2) findLastByEmploymentId: 최신 1건만 반환
 *   3) 다른 employment_id 데이터 격리
 *   4) effectiveDate 동일 시 id DESC tie-breaker
 */
@SpringBootTest
@ContextConfiguration(initializers = [EmploymentHistoryRepositoryImplTest.Initializer::class])
class EmploymentHistoryRepositoryImplTest(
    private val employmentHistoryRepository: EmploymentHistoryRepository,
    private val jpaRepository: EmploymentHistoryJpaRepository,
) : BehaviorSpec({
    extension(SpringExtension)

    val baseTime = ZonedDateTime.of(2026, 5, 17, 9, 0, 0, 0, ZoneOffset.UTC)

    beforeEach {
        jpaRepository.deleteAllInBatch()
    }

    afterSpec {
        jpaRepository.deleteAllInBatch()
    }

    given("동일 employment_id=10에 effectiveDate 다른 이력 3건을 저장하면") {

        `when`("findByEmploymentId(10L)를 호출하면") {
            saveHistory(employmentHistoryRepository, 10L, EmploymentHistoryEventType.HIRE, LocalDate.of(2024, 1, 1), baseTime)
            saveHistory(employmentHistoryRepository, 10L, EmploymentHistoryEventType.PROMOTION, LocalDate.of(2025, 3, 1), baseTime)
            saveHistory(employmentHistoryRepository, 10L, EmploymentHistoryEventType.DEPT_CHANGE, LocalDate.of(2026, 5, 1), baseTime)

            val results = employmentHistoryRepository.findByEmploymentId(10L)

            then("3건이 effectiveDate DESC 순으로 반환된다") {
                results shouldHaveSize 3
                results[0].effectiveDate shouldBe LocalDate.of(2026, 5, 1)
                results[1].effectiveDate shouldBe LocalDate.of(2025, 3, 1)
                results[2].effectiveDate shouldBe LocalDate.of(2024, 1, 1)
            }
        }

        `when`("findLastByEmploymentId(10L)를 호출하면") {
            saveHistory(employmentHistoryRepository, 10L, EmploymentHistoryEventType.HIRE, LocalDate.of(2024, 1, 1), baseTime)
            saveHistory(employmentHistoryRepository, 10L, EmploymentHistoryEventType.PROMOTION, LocalDate.of(2025, 3, 1), baseTime)
            saveHistory(employmentHistoryRepository, 10L, EmploymentHistoryEventType.DEPT_CHANGE, LocalDate.of(2026, 5, 1), baseTime)

            val last = employmentHistoryRepository.findLastByEmploymentId(10L)

            then("effectiveDate가 가장 최신인 DEPT_CHANGE 1건이 반환된다") {
                last.shouldNotBeNull()
                last.effectiveDate shouldBe LocalDate.of(2026, 5, 1)
                last.eventType shouldBe EmploymentHistoryEventType.DEPT_CHANGE
            }
        }
    }

    given("employment_id=20, 30이 혼재된 상태에서") {

        `when`("findByEmploymentId(20L)를 호출하면") {
            saveHistory(employmentHistoryRepository, 20L, EmploymentHistoryEventType.HIRE, LocalDate.of(2025, 1, 1), baseTime)
            saveHistory(employmentHistoryRepository, 30L, EmploymentHistoryEventType.HIRE, LocalDate.of(2025, 6, 1), baseTime)

            val results = employmentHistoryRepository.findByEmploymentId(20L)

            then("employment_id=20 이력만 반환된다") {
                results.all { it.employmentId == 20L } shouldBe true
            }
        }

        `when`("존재하지 않는 findLastByEmploymentId(999L)를 호출하면") {
            val last = employmentHistoryRepository.findLastByEmploymentId(999L)

            then("null을 반환한다") {
                last.shouldBeNull()
            }
        }
    }

    given("employment_id=40에 effectiveDate가 같고 id가 다른 이력 2건이 있을 때") {
        val sameDate = LocalDate.of(2026, 4, 1)

        `when`("findByEmploymentId(40L)를 호출하면") {
            val savedX = saveHistory(
                employmentHistoryRepository, 40L, EmploymentHistoryEventType.SALARY_CHANGE, sameDate, baseTime,
            )
            val savedY = saveHistory(
                employmentHistoryRepository, 40L, EmploymentHistoryEventType.RESUME, sameDate, baseTime.plusSeconds(1),
            )

            val results = employmentHistoryRepository.findByEmploymentId(40L)

            then("id DESC tie-breaker로 나중에 저장된 이력(savedY)이 먼저 반환된다") {
                results shouldHaveSize 2
                results[0].id shouldBe savedY.id
                results[1].id shouldBe savedX.id
            }
        }
    }
}) {
    companion object {
        val mysql: MySQLContainer<*> = MySQLContainer(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("employee_db")
            .withUsername("test")
            .withPassword("test")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci")

        init {
            mysql.start()
        }
    }

    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(context: ConfigurableApplicationContext) {
            TestPropertyValues.of(
                "spring.datasource.url=${mysql.jdbcUrl}",
                "spring.datasource.username=${mysql.username}",
                "spring.datasource.password=${mysql.password}",
                "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
            ).applyTo(context.environment)
        }
    }
}

private fun saveHistory(
    repository: EmploymentHistoryRepository,
    employmentId: Long,
    eventType: EmploymentHistoryEventType,
    effectiveDate: LocalDate,
    createdAt: ZonedDateTime,
): EmploymentHistory = repository.save(
    EmploymentHistory.create(
        employmentId = employmentId,
        eventType = eventType,
        newValue = mapOf("type" to eventType.name),
        effectiveDate = effectiveDate,
        createdAt = createdAt,
    ),
)
