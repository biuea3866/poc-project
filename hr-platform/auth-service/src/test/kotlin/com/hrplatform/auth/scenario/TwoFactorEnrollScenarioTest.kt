package com.hrplatform.auth.scenario

import com.hrplatform.auth.domain.account.EmailHashService
import com.hrplatform.auth.domain.account.UserAccount
import com.hrplatform.auth.domain.account.UserAccountRepository
import com.hrplatform.auth.infrastructure.crypto.AesGcmStringConverter
import dev.samstevens.totp.code.DefaultCodeGenerator
import dev.samstevens.totp.code.HashingAlgorithm
import dev.samstevens.totp.time.SystemTimeProvider
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

/**
 * 시나리오 3: 2FA 등록 → OTP 검증 → 2FA 로그인
 *
 * 1. UserAccount 생성 + 로그인 → access token
 * 2. POST /auth/2fa/enroll → qrCodeDataUri + backupCodes (secret은 응답 미포함)
 * 3. DB에서 AES-GCM 암호화된 secret 조회 → 복호화 → 현재 OTP 생성
 * 4. POST /auth/2fa/verify (otp) → 200 + UserTwoFactorEnrolledEvent
 * 5. POST /auth/login → 200 + requiresTwoFactor: true
 */
class TwoFactorEnrollScenarioTest(
    environment: Environment,
    @Autowired private val userAccountRepository: UserAccountRepository,
    @Autowired private val passwordEncoder: PasswordEncoder,
    @Autowired private val emailHashService: EmailHashService,
    @Autowired private val aesConverter: AesGcmStringConverter,
) : BaseE2eTest(environment) {

    private fun jsonHeaders(): HttpHeaders = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
    }

    private fun createAccountAndLogin(): Triple<String, String, String> {
        val uniqueSuffix = System.nanoTime()
        val rawPassword = "TestPassword123!"
        val email = "twofactor-scenario-$uniqueSuffix@example.com"
        val emailHash = emailHashService.hash(email)

        userAccountRepository.save(
            UserAccount.create(
                employmentId = 30001L,
                companyId = 1L,
                email = email,
                emailHash = emailHash,
                passwordHash = passwordEncoder.encode(rawPassword),
            ),
        )

        val loginResponse = restTemplate.postForEntity(
            baseUrl("/auth/login"),
            HttpEntity(mapOf("email" to email, "password" to rawPassword), jsonHeaders()),
            Map::class.java,
        )
        val accessToken = requireNotNull(loginResponse.body)["accessToken"] as String
        return Triple(email, emailHash, accessToken)
    }

    init {
        given("ACTIVE 상태의 UserAccount가 존재할 때") {
            `when`("POST /auth/2fa/enroll을 호출하면") {
                val (_, _, accessToken) = createAccountAndLogin()
                val authHeaders = HttpHeaders().apply {
                    set("Authorization", "Bearer $accessToken")
                }
                val enrollResponse = restTemplate.exchange(
                    baseUrl("/auth/2fa/enroll"),
                    HttpMethod.POST,
                    HttpEntity<Void>(authHeaders),
                    Map::class.java,
                )

                then("200 응답과 qrCodeDataUri, 5개의 backupCodes가 반환되며 secret은 포함되지 않는다") {
                    enrollResponse.statusCode shouldBe HttpStatus.OK
                    val body = requireNotNull(enrollResponse.body)
                    body["qrCodeDataUri"] shouldNotBe null
                    body["backupCodes"] shouldNotBe null
                    body.containsKey("secret") shouldBe false

                    val backupCodes = body["backupCodes"] as List<*>
                    backupCodes.size shouldBe 5
                }
            }

            `when`("2FA 등록 시 Kafka에 UserTwoFactorEnrolledEvent가 발행된다") {
                val (_, emailHash, accessToken) = createAccountAndLogin()
                val authHeaders = HttpHeaders().apply {
                    set("Authorization", "Bearer $accessToken")
                }

                val authTopic = "event.hr.auth.v1"
                val consumer = buildKafkaConsumer()
                awaitPartitionAssignment(consumer, authTopic)

                // enroll 호출 → UserTwoFactorEnrolledEvent 발행
                restTemplate.exchange(
                    baseUrl("/auth/2fa/enroll"),
                    HttpMethod.POST,
                    HttpEntity<Void>(authHeaders),
                    Map::class.java,
                )

                val savedAccount = userAccountRepository.findByEmailHash(emailHash)
                val decryptedSecret = savedAccount?.twoFactorSecret

                val verifyResponse = if (decryptedSecret != null) {
                    val currentOtp = generateCurrentOtp(decryptedSecret)
                    val verifyHeaders = HttpHeaders().apply {
                        set("Authorization", "Bearer $accessToken")
                        contentType = MediaType.APPLICATION_JSON
                    }
                    restTemplate.exchange(
                        baseUrl("/auth/2fa/verify"),
                        HttpMethod.POST,
                        HttpEntity(mapOf("otp" to currentOtp), verifyHeaders),
                        Map::class.java,
                    )
                } else {
                    null
                }

                then("2FA enroll 시 UserTwoFactorEnrolledEvent가 발행되고 verify는 200 + verified: true") {
                    val enrolledEvents = pollMessagesByEventType(
                        consumer = consumer,
                        eventTypes = setOf("UserTwoFactorEnrolled"),
                        expectedCount = 1,
                    )
                    consumer.close()
                    enrolledEvents.size shouldBe 1
                    enrolledEvents.first()["eventType"] shouldBe "UserTwoFactorEnrolled"

                    verifyResponse shouldNotBe null
                    val confirmedVerifyResponse = requireNotNull(verifyResponse)
                    confirmedVerifyResponse.statusCode shouldBe HttpStatus.OK
                    (requireNotNull(confirmedVerifyResponse.body)["verified"] as Boolean) shouldBe true
                }
            }

            `when`("2FA가 활성화된 계정으로 일반 POST /auth/login을 호출하면") {
                val (email, emailHash, accessToken) = createAccountAndLogin()
                val authHeaders = HttpHeaders().apply {
                    set("Authorization", "Bearer $accessToken")
                }

                restTemplate.exchange(
                    baseUrl("/auth/2fa/enroll"),
                    HttpMethod.POST,
                    HttpEntity<Void>(authHeaders),
                    Map::class.java,
                )

                val savedAccount = userAccountRepository.findByEmailHash(emailHash)
                val decryptedSecret = savedAccount?.twoFactorSecret
                if (decryptedSecret != null) {
                    val otp = generateCurrentOtp(decryptedSecret)
                    val verifyHeaders = HttpHeaders().apply {
                        set("Authorization", "Bearer $accessToken")
                        contentType = MediaType.APPLICATION_JSON
                    }
                    restTemplate.exchange(
                        baseUrl("/auth/2fa/verify"),
                        HttpMethod.POST,
                        HttpEntity(mapOf("otp" to otp), verifyHeaders),
                        Map::class.java,
                    )
                }

                val rawPassword = "TestPassword123!"
                val loginWithTwoFactorResponse = restTemplate.postForEntity(
                    baseUrl("/auth/login"),
                    HttpEntity(mapOf("email" to email, "password" to rawPassword), jsonHeaders()),
                    Map::class.java,
                )

                then("200 응답 + requiresTwoFactor: true가 반환되고 accessToken은 빈 문자열이다") {
                    loginWithTwoFactorResponse.statusCode shouldBe HttpStatus.OK
                    val body = requireNotNull(loginWithTwoFactorResponse.body)
                    (body["requiresTwoFactor"] as Boolean) shouldBe true
                    (body["accessToken"] as String) shouldBe ""
                }
            }
        }
    }

    /**
     * JPA가 @Convert(AesGcmStringConverter)로 자동 복호화한 평문 TOTP secret으로
     * 현재 시각 기준 OTP를 생성한다.
     */
    private fun generateCurrentOtp(decryptedSecret: String): String {
        val codeGenerator = DefaultCodeGenerator(HashingAlgorithm.SHA1)
        val timeProvider = SystemTimeProvider()
        val timeSlot = Math.floorDiv(timeProvider.time, 30L)
        return codeGenerator.generate(decryptedSecret, timeSlot)
    }
}
