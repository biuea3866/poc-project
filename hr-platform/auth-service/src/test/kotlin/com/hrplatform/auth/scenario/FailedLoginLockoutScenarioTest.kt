package com.hrplatform.auth.scenario

import com.hrplatform.auth.domain.account.EmailHashService
import com.hrplatform.auth.domain.account.UserAccount
import com.hrplatform.auth.domain.account.UserAccountRepository
import com.hrplatform.auth.domain.account.UserAccountStatus
import com.hrplatform.auth.domain.role.RoleRepository
import com.hrplatform.auth.domain.role.UserAccountRole
import com.hrplatform.auth.domain.role.UserAccountRoleRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.ZoneOffset
import java.time.ZonedDateTime

private const val MAX_FAILED_ATTEMPTS = 5

/**
 * 시나리오 2: 로그인 실패 5회 → 잠금 → 관리자 해제
 *
 * 1. UserAccount 생성 (일반 사용자 + HR_MANAGER 관리자)
 * 2. POST /auth/login 잘못된 password 5회 반복 → 5번째에서 계정 잠금
 * 3. 잠금 후 추가 잘못된 시도 → 401 + UserLockedEvent Kafka 발행 검증
 * 4. UserAccount.status = LOCKED 확인
 * 5. 정상 password 로그인 → 401 (잠금 상태)
 * 6. POST /auth/admin/users/{id}/unlock (HR_MANAGER 토큰) → 200 + UserUnlockedEvent 발행
 * 7. 정상 password 로그인 → 200
 */
class FailedLoginLockoutScenarioTest(
    environment: Environment,
    @Autowired private val userAccountRepository: UserAccountRepository,
    @Autowired private val passwordEncoder: PasswordEncoder,
    @Autowired private val emailHashService: EmailHashService,
    @Autowired private val roleRepository: RoleRepository,
    @Autowired private val userAccountRoleRepository: UserAccountRoleRepository,
) : BaseE2eTest(environment) {

    private fun jsonHeaders(): HttpHeaders = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
    }

    private fun createUserAndAdmin(): Triple<Long, String, String> {
        val uniqueSuffix = System.nanoTime()
        val rawPassword = "TestPassword123!"

        val userEmail = "lockout-user-$uniqueSuffix@example.com"
        val userEmailHash = emailHashService.hash(userEmail)
        val userAccount = userAccountRepository.save(
            UserAccount.create(
                employmentId = 20001L,
                companyId = 1L,
                email = userEmail,
                emailHash = userEmailHash,
                passwordHash = passwordEncoder.encode(rawPassword),
            ),
        )
        val userAccountId = requireNotNull(userAccount.id)

        val adminRawPassword = "AdminPassword123!"
        val adminEmail = "lockout-admin-$uniqueSuffix@example.com"
        val adminEmailHash = emailHashService.hash(adminEmail)
        val adminAccount = userAccountRepository.save(
            UserAccount.create(
                employmentId = 20002L,
                companyId = 1L,
                email = adminEmail,
                emailHash = adminEmailHash,
                passwordHash = passwordEncoder.encode(adminRawPassword),
            ),
        )
        val adminAccountId = requireNotNull(adminAccount.id)

        val hrManagerRole = roleRepository.findAllByCompanyId(null).firstOrNull { it.code == "HR_MANAGER" }
        if (hrManagerRole != null) {
            userAccountRoleRepository.save(
                UserAccountRole(
                    userAccountId = adminAccountId,
                    roleId = requireNotNull(hrManagerRole.id),
                    assignedAt = ZonedDateTime.now(ZoneOffset.UTC),
                    assignedBy = null,
                ),
            )
        }

        val adminLoginResponse = restTemplate.postForEntity(
            baseUrl("/auth/login"),
            HttpEntity(mapOf("email" to adminEmail, "password" to adminRawPassword), jsonHeaders()),
            Map::class.java,
        )
        val adminAccessToken = adminLoginResponse.body?.get("accessToken") as? String ?: ""

        return Triple(userAccountId, userEmail, adminAccessToken)
    }

    init {
        given("일반 사용자와 HR_MANAGER 관리자 계정이 존재할 때") {
            val rawPassword = "TestPassword123!"
            val wrongPassword = "WrongPassword999!"

            `when`("잘못된 비밀번호로 $MAX_FAILED_ATTEMPTS 회 로그인 시도하면") {
                val (userAccountId, userEmail, _) = createUserAndAdmin()
                val wrongBody = HttpEntity(mapOf("email" to userEmail, "password" to wrongPassword), jsonHeaders())

                repeat(MAX_FAILED_ATTEMPTS) {
                    restTemplate.postForEntity(baseUrl("/auth/login"), wrongBody, Map::class.java)
                }

                // 5회 실패 후 잠금 여부를 추가 로그인 시도로 확인 (DB 직접 조회 대신)
                val postLockResponse = restTemplate.postForEntity(
                    baseUrl("/auth/login"),
                    HttpEntity(mapOf("email" to userEmail, "password" to wrongPassword), jsonHeaders()),
                    Map::class.java,
                )

                then("계정이 LOCKED 상태가 된다 (추가 로그인 시도 401로 확인)") {
                    postLockResponse.statusCode shouldBe HttpStatus.UNAUTHORIZED
                    val locked = userAccountRepository.findByEmailHash(emailHashService.hash(userEmail))
                    locked shouldNotBe null
                    requireNotNull(locked).status shouldBe UserAccountStatus.LOCKED
                }
            }

            `when`("5회 실패로 계정이 잠길 때 Kafka에 UserLockedEvent가 발행된다") {
                val (_, userEmail, _) = createUserAndAdmin()
                val wrongBody = HttpEntity(mapOf("email" to userEmail, "password" to wrongPassword), jsonHeaders())

                val authTopic = "event.hr.auth.v1"
                val consumer = buildKafkaConsumer()
                awaitPartitionAssignment(consumer, authTopic)

                // 5회 실패로 잠금 (5번째 실패에서 UserLockedEvent 발행)
                repeat(MAX_FAILED_ATTEMPTS) {
                    restTemplate.postForEntity(baseUrl("/auth/login"), wrongBody, Map::class.java)
                }

                then("401 응답과 UserLocked 이벤트가 발행된다") {
                    val lockedEvents = pollMessagesByEventType(
                        consumer = consumer,
                        eventTypes = setOf("UserLocked"),
                        expectedCount = 1,
                    )
                    consumer.close()
                    lockedEvents.size shouldBe 1
                    lockedEvents.first()["eventType"] shouldBe "UserLocked"
                }
            }

            `when`("잠금 상태에서 정상 비밀번호로 로그인 시도하면") {
                val (_, userEmail, _) = createUserAndAdmin()
                val wrongBody = HttpEntity(mapOf("email" to userEmail, "password" to wrongPassword), jsonHeaders())
                repeat(MAX_FAILED_ATTEMPTS + 1) {
                    restTemplate.postForEntity(baseUrl("/auth/login"), wrongBody, Map::class.java)
                }

                val response = restTemplate.postForEntity(
                    baseUrl("/auth/login"),
                    HttpEntity(mapOf("email" to userEmail, "password" to rawPassword), jsonHeaders()),
                    Map::class.java,
                )

                then("401 응답이 반환된다 (계정 잠금)") {
                    response.statusCode shouldBe HttpStatus.UNAUTHORIZED
                }
            }

            `when`("HR_MANAGER 권한으로 POST /auth/admin/users/{id}/unlock을 호출하면") {
                val (userAccountId, userEmail, adminAccessToken) = createUserAndAdmin()
                val wrongBody = HttpEntity(mapOf("email" to userEmail, "password" to wrongPassword), jsonHeaders())
                repeat(MAX_FAILED_ATTEMPTS + 1) {
                    restTemplate.postForEntity(baseUrl("/auth/login"), wrongBody, Map::class.java)
                }

                val authTopic = "event.hr.auth.v1"
                val consumer = buildKafkaConsumer()
                awaitPartitionAssignment(consumer, authTopic)

                val unlockResponse = restTemplate.exchange(
                    baseUrl("/auth/admin/users/$userAccountId/unlock"),
                    HttpMethod.POST,
                    HttpEntity<Void>(
                        HttpHeaders().apply {
                            set("Authorization", "Bearer $adminAccessToken")
                        },
                    ),
                    Map::class.java,
                )
                then("200 응답과 UserUnlockedEvent가 Kafka에 발행되고 계정이 ACTIVE가 된다") {
                    unlockResponse.statusCode shouldBe HttpStatus.OK

                    val unlockedAccount = userAccountRepository.findById(userAccountId)
                    unlockedAccount shouldNotBe null
                    requireNotNull(unlockedAccount).status shouldBe UserAccountStatus.ACTIVE

                    val unlockedEvents = pollMessagesByEventType(
                        consumer = consumer,
                        eventTypes = setOf("UserUnlocked"),
                        expectedCount = 1,
                    )
                    consumer.close()
                    unlockedEvents.size shouldBe 1
                    unlockedEvents.first()["eventType"] shouldBe "UserUnlocked"
                }
            }

            `when`("잠금 해제 후 정상 비밀번호로 로그인하면") {
                val (userAccountId, userEmail, adminAccessToken) = createUserAndAdmin()
                val wrongBody = HttpEntity(mapOf("email" to userEmail, "password" to wrongPassword), jsonHeaders())
                repeat(MAX_FAILED_ATTEMPTS + 1) {
                    restTemplate.postForEntity(baseUrl("/auth/login"), wrongBody, Map::class.java)
                }

                restTemplate.exchange(
                    baseUrl("/auth/admin/users/$userAccountId/unlock"),
                    HttpMethod.POST,
                    HttpEntity<Void>(
                        HttpHeaders().apply {
                            set("Authorization", "Bearer $adminAccessToken")
                        },
                    ),
                    Map::class.java,
                )

                val loginResponse = restTemplate.postForEntity(
                    baseUrl("/auth/login"),
                    HttpEntity(mapOf("email" to userEmail, "password" to rawPassword), jsonHeaders()),
                    Map::class.java,
                )

                then("200 응답이 반환된다") {
                    loginResponse.statusCode shouldBe HttpStatus.OK
                    requireNotNull(loginResponse.body)["accessToken"] shouldNotBe null
                }
            }
        }
    }
}
