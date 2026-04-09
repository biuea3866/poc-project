package com.closet.inventory.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.ZonedDateTime

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class InventoryCustomRepositoryTest : BehaviorSpec() {
    override fun extensions() = listOf(SpringExtension)

    @Autowired
    private lateinit var inventoryRepository: InventoryRepository

    companion object {
        private val mysqlContainer: MySQLContainer<*> =
            MySQLContainer("mysql:8.0")
                .withDatabaseName("closet_test")
                .withUsername("test")
                .withPassword("test")
                .withCommand(
                    "--character-set-server=utf8mb4",
                    "--collation-server=utf8mb4_unicode_ci",
                    "--default-time-zone=+09:00",
                )
                .apply { start() }

        private val redisContainer: GenericContainer<*> =
            GenericContainer("redis:7.0-alpine")
                .withExposedPorts(6379)
                .apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun overrideProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { mysqlContainer.jdbcUrl }
            registry.add("spring.datasource.username") { mysqlContainer.username }
            registry.add("spring.datasource.password") { mysqlContainer.password }
            registry.add("spring.datasource.driver-class-name") { "com.mysql.cj.jdbc.Driver" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
            registry.add("spring.flyway.enabled") { "false" }
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
        }
    }

    init {
        Given("안전재고 이하인 재고와 초과인 재고가 DB에 저장되어 있을 때") {
            inventoryRepository.deleteAll()

            // 안전재고 이하 (available=5 <= threshold=10)
            inventoryRepository.save(
                Inventory.create(
                    productId = 1L,
                    productOptionId = 101L,
                    sku = "SKU-BELOW-001",
                    totalQuantity = 5,
                    safetyThreshold = 10,
                ),
            )

            // 안전재고 이하 (available=10 <= threshold=10, 경계값)
            inventoryRepository.save(
                Inventory.create(
                    productId = 2L,
                    productOptionId = 102L,
                    sku = "SKU-BELOW-002",
                    totalQuantity = 10,
                    safetyThreshold = 10,
                ),
            )

            // 안전재고 초과 (available=100 > threshold=10)
            inventoryRepository.save(
                Inventory.create(
                    productId = 3L,
                    productOptionId = 103L,
                    sku = "SKU-ABOVE-001",
                    totalQuantity = 100,
                    safetyThreshold = 10,
                ),
            )

            When("findBelowSafetyThreshold 호출") {
                val result = inventoryRepository.findBelowSafetyThreshold()

                Then("안전재고 이하인 재고만 조회된다") {
                    result shouldHaveSize 2
                    result.map { it.sku } shouldContainExactlyInAnyOrder listOf("SKU-BELOW-001", "SKU-BELOW-002")
                }
            }
        }

        Given("소프트 삭제된 재고가 포함되어 있을 때") {
            inventoryRepository.deleteAll()

            // 안전재고 이하이지만 삭제된 재고
            val deletedInventory =
                Inventory.create(
                    productId = 10L,
                    productOptionId = 110L,
                    sku = "SKU-DELETED-001",
                    totalQuantity = 3,
                    safetyThreshold = 10,
                )
            val saved = inventoryRepository.save(deletedInventory)
            saved.deletedAt = ZonedDateTime.now()
            inventoryRepository.save(saved)

            // 안전재고 이하이고 삭제되지 않은 재고
            inventoryRepository.save(
                Inventory.create(
                    productId = 11L,
                    productOptionId = 111L,
                    sku = "SKU-ACTIVE-001",
                    totalQuantity = 2,
                    safetyThreshold = 10,
                ),
            )

            When("findBelowSafetyThreshold 호출") {
                val result = inventoryRepository.findBelowSafetyThreshold()

                Then("삭제된 재고는 제외되고 활성 재고만 조회된다") {
                    result shouldHaveSize 1
                    result[0].sku shouldBe "SKU-ACTIVE-001"
                }
            }
        }

        Given("모든 재고가 안전재고 초과일 때") {
            inventoryRepository.deleteAll()

            inventoryRepository.save(
                Inventory.create(
                    productId = 20L,
                    productOptionId = 120L,
                    sku = "SKU-PLENTY-001",
                    totalQuantity = 500,
                    safetyThreshold = 10,
                ),
            )

            When("findBelowSafetyThreshold 호출") {
                val result = inventoryRepository.findBelowSafetyThreshold()

                Then("빈 목록이 반환된다") {
                    result shouldHaveSize 0
                }
            }
        }

        Given("품절 재고(available=0)가 있을 때") {
            inventoryRepository.deleteAll()

            val outOfStock =
                Inventory.create(
                    productId = 30L,
                    productOptionId = 130L,
                    sku = "SKU-OOS-001",
                    totalQuantity = 10,
                    safetyThreshold = 5,
                )
            outOfStock.reserve(10) // available = 0
            inventoryRepository.save(outOfStock)

            When("findBelowSafetyThreshold 호출") {
                val result = inventoryRepository.findBelowSafetyThreshold()

                Then("품절 재고도 안전재고 이하로 조회된다") {
                    result shouldHaveSize 1
                    result[0].sku shouldBe "SKU-OOS-001"
                    result[0].availableQuantity shouldBe 0
                }
            }
        }
    }
}
