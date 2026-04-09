package com.closet.promotion.integration

import com.closet.common.exception.BusinessException
import com.closet.promotion.application.CouponService
import com.closet.promotion.domain.coupon.CouponScope
import com.closet.promotion.domain.coupon.CouponType
import com.closet.promotion.domain.coupon.MemberCouponStatus
import com.closet.promotion.presentation.dto.CreateCouponRequest
import com.closet.promotion.repository.CouponRepository
import com.closet.promotion.repository.MemberCouponRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.StringRedisTemplate
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
class CouponServiceIntegrationTest : BehaviorSpec() {
    override fun extensions() = listOf(SpringExtension)

    @Autowired
    private lateinit var couponService: CouponService

    @Autowired
    private lateinit var couponRepository: CouponRepository

    @Autowired
    private lateinit var memberCouponRepository: MemberCouponRepository

    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

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
        Given("쿠폰 생성 — MySQL + Redis 통합") {
            couponRepository.deleteAll()
            memberCouponRepository.deleteAll()

            val request =
                CreateCouponRequest(
                    name = "5000원 할인 쿠폰",
                    couponType = CouponType.FIXED_AMOUNT,
                    discountValue = BigDecimal("5000"),
                    minOrderAmount = BigDecimal("30000"),
                    scope = CouponScope.ALL,
                    totalQuantity = 100,
                    validFrom = ZonedDateTime.now().minusDays(1),
                    validTo = ZonedDateTime.now().plusDays(30),
                )

            When("쿠폰 생성") {
                val response = couponService.createCoupon(request)

                Then("MySQL에 쿠폰이 저장된다") {
                    val saved = couponRepository.findById(response.id)
                    saved.isPresent shouldBe true
                    saved.get().name shouldBe "5000원 할인 쿠폰"
                    saved.get().totalQuantity shouldBe 100
                }

                Then("Redis에 재고가 세팅된다") {
                    val stock = redisTemplate.opsForValue().get("coupon:stock:${response.id}")
                    stock shouldBe "100"
                }
            }
        }

        Given("쿠폰 발급 — Redis DECR 동시성 제어 통합") {
            couponRepository.deleteAll()
            memberCouponRepository.deleteAll()

            val coupon =
                couponService.createCoupon(
                    CreateCouponRequest(
                        name = "선착순 2장 쿠폰",
                        couponType = CouponType.FIXED_AMOUNT,
                        discountValue = BigDecimal("3000"),
                        totalQuantity = 2,
                        validFrom = ZonedDateTime.now().minusDays(1),
                        validTo = ZonedDateTime.now().plusDays(30),
                    ),
                )

            When("1번째 발급") {
                val mc1 = couponService.issueCoupon(coupon.id, 1L)

                Then("발급 성공, Redis 재고 1로 감소") {
                    mc1.status shouldBe MemberCouponStatus.AVAILABLE
                    mc1.memberId shouldBe 1L
                    redisTemplate.opsForValue().get("coupon:stock:${coupon.id}") shouldBe "1"
                }
            }

            When("2번째 발급 (마지막 수량)") {
                val mc2 = couponService.issueCoupon(coupon.id, 2L)

                Then("발급 성공, Redis 재고 0") {
                    mc2.status shouldBe MemberCouponStatus.AVAILABLE
                    redisTemplate.opsForValue().get("coupon:stock:${coupon.id}") shouldBe "0"
                }
            }

            When("3번째 발급 시도 (수량 소진)") {
                Then("BusinessException 발생") {
                    shouldThrow<BusinessException> {
                        couponService.issueCoupon(coupon.id, 3L)
                    }
                }

                Then("Redis 재고가 음수로 내려가지 않는다") {
                    val stock = redisTemplate.opsForValue().get("coupon:stock:${coupon.id}")?.toLong() ?: 0
                    stock shouldBe 0L
                }
            }
        }

        Given("쿠폰 중복 발급 방지 통합") {
            couponRepository.deleteAll()
            memberCouponRepository.deleteAll()

            val coupon =
                couponService.createCoupon(
                    CreateCouponRequest(
                        name = "중복 방지 테스트",
                        couponType = CouponType.PERCENTAGE,
                        discountValue = BigDecimal("10"),
                        maxDiscountAmount = BigDecimal("5000"),
                        totalQuantity = 100,
                        validFrom = ZonedDateTime.now().minusDays(1),
                        validTo = ZonedDateTime.now().plusDays(30),
                    ),
                )

            couponService.issueCoupon(coupon.id, 1L)

            When("같은 회원이 다시 발급 시도") {
                Then("BusinessException 발생 (중복 발급)") {
                    shouldThrow<BusinessException> {
                        couponService.issueCoupon(coupon.id, 1L)
                    }
                }
            }
        }

        Given("쿠폰 사용 — DB 상태 변경 통합") {
            couponRepository.deleteAll()
            memberCouponRepository.deleteAll()

            val coupon =
                couponService.createCoupon(
                    CreateCouponRequest(
                        name = "사용 테스트 쿠폰",
                        couponType = CouponType.FIXED_AMOUNT,
                        discountValue = BigDecimal("5000"),
                        totalQuantity = 10,
                        validFrom = ZonedDateTime.now().minusDays(1),
                        validTo = ZonedDateTime.now().plusDays(30),
                    ),
                )

            couponService.issueCoupon(coupon.id, 1L)

            When("쿠폰 사용") {
                val used = couponService.useCoupon(coupon.id, 100L, 1L)

                Then("DB에서 상태가 USED로 변경된다") {
                    used.status shouldBe MemberCouponStatus.USED
                    used.usedOrderId shouldBe 100L
                }
            }
        }

        Given("쿠폰 검증 통합") {
            couponRepository.deleteAll()

            val coupon =
                couponService.createCoupon(
                    CreateCouponRequest(
                        name = "검증 테스트 쿠폰",
                        couponType = CouponType.PERCENTAGE,
                        discountValue = BigDecimal("20"),
                        maxDiscountAmount = BigDecimal("10000"),
                        minOrderAmount = BigDecimal("30000"),
                        totalQuantity = 50,
                        validFrom = ZonedDateTime.now().minusDays(1),
                        validTo = ZonedDateTime.now().plusDays(30),
                    ),
                )

            When("유효한 주문 금액으로 검증") {
                val validation = couponService.validateCoupon(coupon.id, BigDecimal("50000"))

                Then("유효하고 할인 금액이 계산된다") {
                    validation.isValid shouldBe true
                    validation.discountAmount.compareTo(BigDecimal("10000")) shouldBe 0
                }
            }

            When("최소 주문 금액 미만으로 검증") {
                val validation = couponService.validateCoupon(coupon.id, BigDecimal("20000"))

                Then("유효하지 않다") {
                    validation.isValid shouldBe false
                }
            }
        }
    }
}
