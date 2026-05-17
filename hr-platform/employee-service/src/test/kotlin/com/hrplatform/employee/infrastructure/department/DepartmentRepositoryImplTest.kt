package com.hrplatform.employee.infrastructure.department

import com.hrplatform.core.util.ZonedDateTimes
import com.hrplatform.employee.domain.department.Department
import com.hrplatform.employee.domain.department.DepartmentRepository
import com.querydsl.jpa.impl.JPAQueryFactory
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.flywaydb.core.Flyway
import org.hibernate.cfg.AvailableSettings
import org.hibernate.jpa.HibernatePersistenceProvider
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Properties

class DepartmentRepositoryImplTest : BehaviorSpec({

    val today = LocalDate.now()

    /**
     * 매 테스트마다 독립적인 EntityManager + Repository를 생성하는 픽스처 함수.
     * 트랜잭션 격리를 위해 test 종료 시 rollback.
     */
    fun withRepo(
        block: (EntityManager, DepartmentRepository) -> Unit,
    ) {
        val em = Fixtures.emf.createEntityManager()
        val queryFactory = JPAQueryFactory(em)
        val jpaRepo = InMemoryDepartmentJpaRepository(em)
        val repo: DepartmentRepository = DepartmentRepositoryImpl(jpaRepo, queryFactory)

        em.transaction.begin()
        try {
            block(em, repo)
        } finally {
            if (em.transaction.isActive) em.transaction.rollback()
            em.close()
        }
    }

    fun EntityManager.persistDept(
        name: String,
        code: String,
        parentId: Long? = null,
        path: String,
        headEmploymentId: Long? = null,
        effectiveFrom: LocalDate = today,
        effectiveTo: LocalDate? = null,
        now: ZonedDateTime = ZonedDateTimes.nowUtc(),
    ): Department {
        val dept = Department(
            companyId = 1L,
            name = name,
            code = code,
            parentId = parentId,
            path = path,
            headEmploymentId = headEmploymentId,
            orderNo = 0,
            effectiveFrom = effectiveFrom,
            effectiveTo = effectiveTo,
        )
        // JPA Auditing 없이 audit 컬럼 수동 설정
        val auditCreatedAt = dept.javaClass.superclass.superclass.getDeclaredField("createdAt").also { it.isAccessible = true }
        auditCreatedAt.set(dept, now)
        val auditUpdatedAt = dept.javaClass.superclass.superclass.getDeclaredField("updatedAt").also { it.isAccessible = true }
        auditUpdatedAt.set(dept, now)

        persist(dept)
        flush()
        return dept
    }

    given("DepartmentRepositoryImpl save + findById") {
        `when`("Department를 저장하고 id로 조회하면") {
            then("저장된 엔티티를 반환한다") {
                withRepo { em, repo ->
                    val dept = em.persistDept(name = "개발팀-SFI", code = "DEV-SFI", path = "/placeholder/")
                    val realPath = "/${dept.id}/"
                    dept.path = realPath
                    em.flush()
                    em.clear()

                    val found = repo.findById(dept.id!!)
                    found shouldNotBe null
                    found!!.name shouldBe "개발팀-SFI"
                    found.code shouldBe "DEV-SFI"
                    found.path shouldBe realPath
                }
            }
        }

        `when`("존재하지 않는 id로 조회하면") {
            then("null을 반환한다") {
                withRepo { _, repo ->
                    repo.findById(Long.MAX_VALUE) shouldBe null
                }
            }
        }
    }

    given("DepartmentRepositoryImpl findByPathPrefix") {
        `when`("루트 부서와 하위 부서를 저장하면") {
            then("prefix로 서브트리 전체를 조회할 수 있다") {
                withRepo { em, repo ->
                    val root = em.persistDept(name = "루트-FPP", code = "ROOT-FPP", path = "/placeholder/")
                    val rootPath = "/${root.id}/"
                    root.path = rootPath
                    em.flush()

                    val child1 = em.persistDept(name = "하위1-FPP", code = "C1-FPP", parentId = root.id, path = "${rootPath}x/")
                    child1.path = "${rootPath}${child1.id}/"
                    em.flush()

                    val child2 = em.persistDept(name = "하위2-FPP", code = "C2-FPP", parentId = root.id, path = "${rootPath}y/")
                    child2.path = "${rootPath}${child2.id}/"
                    em.flush()

                    val other = em.persistDept(name = "별개루트-FPP", code = "OTHER-FPP", path = "/placeholder2/")
                    other.path = "/${other.id}/"
                    em.flush()
                    em.clear()

                    val results = repo.findByPathPrefix(rootPath)
                    results.map { it.name } shouldContainExactlyInAnyOrder listOf("루트-FPP", "하위1-FPP", "하위2-FPP")
                }
            }

            then("별개 루트는 포함되지 않는다") {
                withRepo { em, repo ->
                    val root = em.persistDept(name = "루트-FPP2", code = "ROOT-FPP2", path = "/placeholder/")
                    val rootPath = "/${root.id}/"
                    root.path = rootPath
                    em.flush()

                    val other = em.persistDept(name = "별개루트-FPP2", code = "OTHER-FPP2", path = "/placeholder2/")
                    other.path = "/${other.id}/"
                    em.flush()
                    em.clear()

                    val results = repo.findByPathPrefix(rootPath)
                    results.none { it.name == "별개루트-FPP2" } shouldBe true
                }
            }
        }

        `when`("soft-delete된 부서는 findByPathPrefix 결과에서 제외된다") {
            then("soft-delete된 부서는 결과에 포함되지 않는다") {
                withRepo { em, repo ->
                    val dept = em.persistDept(name = "삭제팀-FPP", code = "DELETED-FPP", path = "/placeholder/")
                    val deptPath = "/${dept.id}/"
                    dept.path = deptPath
                    em.flush()

                    dept.softDelete(ZonedDateTimes.nowUtc(), by = null)
                    em.flush()
                    em.clear()

                    repo.findByPathPrefix(deptPath) shouldHaveSize 0
                }
            }
        }
    }

    given("DepartmentRepositoryImpl findByParentId") {
        `when`("특정 parentId로 자식 조회") {
            then("해당 parentId의 자식 부서 2개가 반환된다") {
                withRepo { em, repo ->
                    val parent = em.persistDept(name = "부모팀-FBP", code = "PARENT-FBP", path = "/placeholder/")
                    val parentPath = "/${parent.id}/"
                    parent.path = parentPath
                    em.flush()

                    em.persistDept(name = "자식1-FBP", code = "C1-FBP", parentId = parent.id, path = "${parentPath}1/")
                    em.persistDept(name = "자식2-FBP", code = "C2-FBP", parentId = parent.id, path = "${parentPath}2/")
                    em.flush()
                    em.clear()

                    val results = repo.findByParentId(parent.id)
                    results shouldHaveSize 2
                    results.map { it.name } shouldContainExactlyInAnyOrder listOf("자식1-FBP", "자식2-FBP")
                }
            }
        }

        `when`("soft-delete된 자식 부서는 findByParentId 결과에서 제외된다") {
            then("soft-delete된 자식이 포함되지 않는다") {
                withRepo { em, repo ->
                    val parent = em.persistDept(name = "부모-SD-FBP", code = "PARENT-SD-FBP", path = "/placeholder/")
                    val parentPath = "/${parent.id}/"
                    parent.path = parentPath
                    em.flush()

                    val child = em.persistDept(name = "자식-SD-FBP", code = "CHILD-SD-FBP", parentId = parent.id, path = "${parentPath}1/")
                    em.flush()

                    child.softDelete(ZonedDateTimes.nowUtc(), by = null)
                    em.flush()
                    em.clear()

                    repo.findByParentId(parent.id) shouldHaveSize 0
                }
            }
        }
    }
}) {

    companion object Fixtures {
        private val mysql: MySQLContainer<*> = MySQLContainer(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("employee_db_test")
            .withUsername("test")
            .withPassword("test")
            .withCommand(
                "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_unicode_ci",
            )
            .also { container ->
                container.start()
                Flyway.configure()
                    .dataSource(container.jdbcUrl, container.username, container.password)
                    .locations("classpath:db/migration")
                    .load()
                    .migrate()
            }

        val emf: EntityManagerFactory = buildEmf()

        private fun buildEmf(): EntityManagerFactory {
            val dataSource = DriverManagerDataSource().also {
                it.setDriverClassName("com.mysql.cj.jdbc.Driver")
                it.url = mysql.jdbcUrl
                it.username = mysql.username
                it.password = mysql.password
            }

            val vendorAdapter = HibernateJpaVendorAdapter().also {
                it.setGenerateDdl(false)
                it.setShowSql(false)
            }

            return LocalContainerEntityManagerFactoryBean().also { bean ->
                bean.dataSource = dataSource
                bean.jpaVendorAdapter = vendorAdapter
                bean.setPackagesToScan(
                    "com.hrplatform.employee.domain",
                    "com.hrplatform.core.domain",
                )
                val props = Properties()
                props.setProperty(AvailableSettings.DIALECT, "org.hibernate.dialect.MySQL8Dialect")
                props.setProperty(AvailableSettings.HBM2DDL_AUTO, "none")
                props.setProperty("hibernate.timezone.default_storage", "NORMALIZE_UTC")
                props.setProperty(
                    "hibernate.physical_naming_strategy",
                    "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy",
                )
                bean.setJpaProperties(props)
                bean.persistenceProvider = HibernatePersistenceProvider()
                bean.afterPropertiesSet()
            }.`object`!!
        }
    }
}
