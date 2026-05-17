package com.hrplatform.employee.infrastructure.department

import com.hrplatform.employee.domain.department.Department
import com.hrplatform.employee.domain.department.DepartmentRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.LocalDate
import java.time.ZonedDateTime

@SpringBootTest
@ContextConfiguration(initializers = [DepartmentRepositoryImplTest.Initializer::class])
class DepartmentRepositoryImplTest(
    private val departmentRepository: DepartmentRepository,
    private val departmentJpaRepository: DepartmentJpaRepository,
) : BehaviorSpec({
    extension(SpringExtension)

    fun buildDepartment(
        parentId: Long?,
        path: String,
        companyId: Long = 1L,
        name: String = "부서",
        code: String = "CODE-${System.nanoTime()}",
    ): Department = Department(
        id = null,
        companyId = companyId,
        name = name,
        code = code,
        parentId = parentId,
        path = path,
        headEmploymentId = null,
        orderNo = 0,
        effectiveFrom = LocalDate.of(2024, 1, 1),
        effectiveTo = null,
        createdAt = ZonedDateTime.now(),
        updatedAt = ZonedDateTime.now(),
    )

    beforeEach {
        departmentJpaRepository.deleteAllInBatch()
    }

    afterSpec {
        departmentJpaRepository.deleteAllInBatch()
    }

    // save / findById
    given("save 후 findById 검증") {
        `when`("신규 Department 를 저장하면") {
            then("id 가 발급되고 findById 로 조회된다") {
                val saved = departmentRepository.save(buildDepartment(parentId = null, path = "/TMP/"))
                saved.id shouldNotBe null

                val found = departmentRepository.findById(requireNotNull(saved.id))
                found shouldNotBe null
                found!!.path shouldBe "/TMP/"
            }
        }
    }

    given("존재하지 않는 id 로 findById 호출 시") {
        `when`("findById(Long.MAX_VALUE) 를 호출하면") {
            then("null 이 반환된다") {
                departmentRepository.findById(Long.MAX_VALUE) shouldBe null
            }
        }
    }

    // findByPathPrefix — path LIKE 'prefix%' 는 prefix 자신도 포함한다
    given("findByPathPrefix 서브트리 조회") {
        `when`("루트(/ROOT/) 하위 자식 2건 저장 후 prefix '/ROOT/' 로 조회하면") {
            then("루트 포함 3건이 반환된다 (path LIKE '/ROOT/%' 는 '/ROOT/' 자신도 포함)") {
                val root = departmentRepository.save(
                    buildDepartment(parentId = null, path = "/ROOT/", code = "ROOT-${System.nanoTime()}"),
                )
                val rootId = requireNotNull(root.id)
                departmentRepository.save(
                    buildDepartment(parentId = rootId, path = "/ROOT/$rootId/", code = "CHILD1-${System.nanoTime()}"),
                )
                departmentRepository.save(
                    buildDepartment(parentId = rootId, path = "/ROOT/$rootId/C2/", code = "CHILD2-${System.nanoTime()}"),
                )

                val results = departmentRepository.findByPathPrefix("/ROOT/")
                results.size shouldBe 3
                results.all { it.path.startsWith("/ROOT/") } shouldBe true
            }
        }
    }

    given("findByPathPrefix 다른 prefix 격리 검증") {
        `when`("OTHER 와 TARGET prefix 부서가 있을 때 TARGET 으로 조회하면") {
            then("'/TARGET/Y/' 1건만 반환된다") {
                departmentRepository.save(buildDepartment(parentId = null, path = "/OTHER/X/", code = "OTHER-${System.nanoTime()}"))
                departmentRepository.save(buildDepartment(parentId = null, path = "/TARGET/Y/", code = "TARGET-${System.nanoTime()}"))

                val results = departmentRepository.findByPathPrefix("/TARGET/")
                results.size shouldBe 1
                results[0].path shouldBe "/TARGET/Y/"
            }
        }
    }

    // findByParentId
    given("findByParentId 검증") {
        `when`("자식 2건 저장 후 findByParentId 를 호출하면") {
            then("자식 2건이 반환된다") {
                val root = departmentRepository.save(buildDepartment(parentId = null, path = "/FR/", code = "FROOT-${System.nanoTime()}"))
                val rootId = requireNotNull(root.id)
                departmentRepository.save(buildDepartment(parentId = rootId, path = "/FR/$rootId/", code = "FC1-${System.nanoTime()}"))
                departmentRepository.save(buildDepartment(parentId = rootId, path = "/FR/$rootId/", code = "FC2-${System.nanoTime()}"))

                val children = departmentRepository.findByParentId(rootId)
                children.size shouldBe 2
            }
        }

        `when`("자식 없는 부서의 findByParentId 를 호출하면") {
            then("빈 리스트가 반환된다") {
                val leaf = departmentRepository.save(buildDepartment(parentId = null, path = "/LEAF/", code = "FLEAF-${System.nanoTime()}"))
                val results = departmentRepository.findByParentId(requireNotNull(leaf.id))
                results.isEmpty() shouldBe true
            }
        }
    }
}) {
    companion object {
        val mysql: MySQLContainer<*> = MySQLContainer(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("employee_db")
            .withUsername("test")
            .withPassword("test")
            .withCommand(
                "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_unicode_ci",
            )

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
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
            ).applyTo(context.environment)
        }
    }
}
