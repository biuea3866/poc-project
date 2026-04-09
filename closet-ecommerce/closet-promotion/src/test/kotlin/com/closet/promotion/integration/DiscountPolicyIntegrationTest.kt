package com.closet.promotion.integration

import com.closet.promotion.application.DiscountPolicyService
import com.closet.promotion.domain.discount.ConditionType
import com.closet.promotion.domain.discount.DiscountType
import com.closet.promotion.presentation.dto.ApplyDiscountRequest
import com.closet.promotion.presentation.dto.CreateDiscountPolicyRequest
import com.closet.promotion.repository.DiscountHistoryRepository
import com.closet.promotion.repository.DiscountPolicyRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
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
import java.math.BigDecimal
import java.time.ZonedDateTime

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class DiscountPolicyIntegrationTest : BehaviorSpec() {
    override fun extensions() = listOf(SpringExtension)

    @Autowired
    private lateinit var discountPolicyService: DiscountPolicyService

    @Autowired
    private lateinit var discountPolicyRepository: DiscountPolicyRepository

    @Autowired
    private lateinit var discountHistoryRepository: DiscountHistoryRepository

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
        Given("할인 정책 생성 및 DB 저장") {
            val request =
                CreateDiscountPolicyRequest(
                    name = "전체 상품 10% 할인",
                    discountType = DiscountType.PERCENT,
                    discountValue = BigDecimal("10"),
                    maxDiscountAmount = BigDecimal("10000"),
                    conditionType = ConditionType.ALL,
                    conditionValue = "",
                    priority = 1,
                    isStackable = false,
                    startedAt = ZonedDateTime.now().minusDays(1),
                    endedAt = ZonedDateTime.now().plusDays(30),
                )

            When("정책 생성 API 호출") {
                val response = discountPolicyService.createPolicy(request)

                Then("DB에 정책이 저장된다") {
                    val saved = discountPolicyRepository.findById(response.id)
                    saved.isPresent shouldBe true
                    saved.get().name shouldBe "전체 상품 10% 할인"
                    saved.get().discountType shouldBe DiscountType.PERCENT
                    saved.get().isActive shouldBe true
                }
            }
        }

        Given("정액 할인 정책이 DB에 저장되어 있을 때") {
            discountPolicyRepository.deleteAll()
            discountHistoryRepository.deleteAll()

            discountPolicyService.createPolicy(
                CreateDiscountPolicyRequest(
                    name = "5000원 할인",
                    discountType = DiscountType.FIXED,
                    discountValue = BigDecimal("5000"),
                    conditionType = ConditionType.ALL,
                    conditionValue = "",
                    priority = 1,
                    isStackable = false,
                    startedAt = ZonedDateTime.now().minusDays(1),
                    endedAt = ZonedDateTime.now().plusDays(30),
                ),
            )

            When("50000원 주문에 할인 적용") {
                val result =
                    discountPolicyService.applyBestDiscount(
                        ApplyDiscountRequest(
                            orderId = 100L,
                            memberId = 1L,
                            originalAmount = BigDecimal("50000"),
                        ),
                    )

                Then("5000원 할인이 적용된다") {
                    result.discountAmount.compareTo(BigDecimal("5000")) shouldBe 0
                    result.finalAmount.compareTo(BigDecimal("45000")) shouldBe 0
                }

                Then("할인 이력이 DB에 저장된다") {
                    val histories = discountHistoryRepository.findAll()
                    histories shouldHaveSize 1
                    histories[0].orderId shouldBe 100L
                    histories[0].discountAmount.compareTo(BigDecimal("5000")) shouldBe 0
                }
            }
        }

        Given("중복 적용 가능한 할인 정책 2개가 DB에 저장되어 있을 때") {
            discountPolicyRepository.deleteAll()
            discountHistoryRepository.deleteAll()

            discountPolicyService.createPolicy(
                CreateDiscountPolicyRequest(
                    name = "전체 3000원 할인",
                    discountType = DiscountType.FIXED,
                    discountValue = BigDecimal("3000"),
                    conditionType = ConditionType.ALL,
                    conditionValue = "",
                    priority = 1,
                    isStackable = true,
                    startedAt = ZonedDateTime.now().minusDays(1),
                    endedAt = ZonedDateTime.now().plusDays(30),
                ),
            )
            discountPolicyService.createPolicy(
                CreateDiscountPolicyRequest(
                    name = "카테고리 10% 할인",
                    discountType = DiscountType.PERCENT,
                    discountValue = BigDecimal("10"),
                    conditionType = ConditionType.CATEGORY,
                    conditionValue = "1",
                    priority = 2,
                    isStackable = true,
                    startedAt = ZonedDateTime.now().minusDays(1),
                    endedAt = ZonedDateTime.now().plusDays(30),
                ),
            )

            When("카테고리1 상품 50000원 주문에 중복 할인 적용") {
                val result =
                    discountPolicyService.applyStackedDiscounts(
                        ApplyDiscountRequest(
                            orderId = 200L,
                            memberId = 1L,
                            originalAmount = BigDecimal("50000"),
                            categoryId = 1L,
                        ),
                    )

                Then("두 할인이 모두 적용된다 (3000 + 4700 = 7700)") {
                    // 1차: 50000 - 3000 = 47000
                    // 2차: 47000 * 10% = 4700
                    // 총 할인: 7700
                    result.appliedPolicies shouldHaveSize 2
                    result.totalDiscountAmount.compareTo(BigDecimal("7700")) shouldBe 0
                    result.finalAmount.compareTo(BigDecimal("42300")) shouldBe 0
                }

                Then("할인 이력이 2건 DB에 저장된다") {
                    val histories = discountHistoryRepository.findAll()
                    histories.filter { it.orderId == 200L } shouldHaveSize 2
                }
            }
        }

        Given("정책 비활성화") {
            discountPolicyRepository.deleteAll()

            val created =
                discountPolicyService.createPolicy(
                    CreateDiscountPolicyRequest(
                        name = "비활성화 대상",
                        discountType = DiscountType.FIXED,
                        discountValue = BigDecimal("1000"),
                        conditionType = ConditionType.ALL,
                        conditionValue = "",
                        priority = 1,
                        isStackable = false,
                        startedAt = ZonedDateTime.now().minusDays(1),
                        endedAt = ZonedDateTime.now().plusDays(30),
                    ),
                )

            When("비활성화 처리") {
                val response = discountPolicyService.deactivatePolicy(created.id)

                Then("DB에서 비활성 상태로 변경된다") {
                    response.isActive shouldBe false
                    val saved = discountPolicyRepository.findById(created.id)
                    saved.get().isActive shouldBe false
                }
            }

            When("비활성화된 정책에 할인 적용 시도") {
                val result =
                    discountPolicyService.applyBestDiscount(
                        ApplyDiscountRequest(
                            orderId = 300L,
                            memberId = 1L,
                            originalAmount = BigDecimal("50000"),
                        ),
                    )

                Then("할인이 적용되지 않는다") {
                    result.discountAmount.compareTo(BigDecimal.ZERO) shouldBe 0
                    result.finalAmount.compareTo(BigDecimal("50000")) shouldBe 0
                }
            }
        }

        Given("조건부 할인 정책 — 브랜드 기반 DB 조회") {
            discountPolicyRepository.deleteAll()
            discountHistoryRepository.deleteAll()

            discountPolicyService.createPolicy(
                CreateDiscountPolicyRequest(
                    name = "나이키 브랜드 15% 할인",
                    discountType = DiscountType.PERCENT,
                    discountValue = BigDecimal("15"),
                    conditionType = ConditionType.BRAND,
                    conditionValue = "10",
                    priority = 1,
                    isStackable = false,
                    startedAt = ZonedDateTime.now().minusDays(1),
                    endedAt = ZonedDateTime.now().plusDays(30),
                ),
            )

            When("매칭되는 브랜드(ID=10)로 할인 조회") {
                val policies =
                    discountPolicyService.findApplicablePolicies(
                        categoryId = null,
                        brandId = 10L,
                        orderAmount = BigDecimal("80000"),
                    )

                Then("1개 정책이 조회된다") {
                    policies shouldHaveSize 1
                    policies[0].name shouldBe "나이키 브랜드 15% 할인"
                }
            }

            When("매칭되지 않는 브랜드(ID=20)로 할인 조회") {
                val policies =
                    discountPolicyService.findApplicablePolicies(
                        categoryId = null,
                        brandId = 20L,
                        orderAmount = BigDecimal("80000"),
                    )

                Then("조회 결과가 없다") {
                    policies shouldHaveSize 0
                }
            }
        }

        Given("조건부 할인 정책 — 금액 범위 기반") {
            discountPolicyRepository.deleteAll()
            discountHistoryRepository.deleteAll()

            discountPolicyService.createPolicy(
                CreateDiscountPolicyRequest(
                    name = "5만원 이상 3000원 할인",
                    discountType = DiscountType.FIXED,
                    discountValue = BigDecimal("3000"),
                    conditionType = ConditionType.AMOUNT_RANGE,
                    conditionValue = "50000",
                    priority = 1,
                    isStackable = false,
                    startedAt = ZonedDateTime.now().minusDays(1),
                    endedAt = ZonedDateTime.now().plusDays(30),
                ),
            )

            When("60000원 주문으로 조건 충족") {
                val result =
                    discountPolicyService.applyBestDiscount(
                        ApplyDiscountRequest(
                            orderId = 400L,
                            memberId = 1L,
                            originalAmount = BigDecimal("60000"),
                        ),
                    )

                Then("3000원 할인 적용") {
                    result.discountAmount.compareTo(BigDecimal("3000")) shouldBe 0
                }
            }

            When("30000원 주문으로 조건 미충족") {
                val result =
                    discountPolicyService.applyBestDiscount(
                        ApplyDiscountRequest(
                            orderId = 401L,
                            memberId = 1L,
                            originalAmount = BigDecimal("30000"),
                        ),
                    )

                Then("할인 없음") {
                    result.discountAmount.compareTo(BigDecimal.ZERO) shouldBe 0
                }
            }
        }
    }
}
