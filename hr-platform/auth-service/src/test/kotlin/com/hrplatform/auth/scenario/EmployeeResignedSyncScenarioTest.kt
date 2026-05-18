package com.hrplatform.auth.scenario

import com.fasterxml.jackson.databind.ObjectMapper
import com.hrplatform.auth.domain.account.EmailHashService
import com.hrplatform.auth.domain.account.UserAccount
import com.hrplatform.auth.domain.account.UserAccountRepository
import com.hrplatform.auth.domain.account.UserAccountStatus
import com.hrplatform.auth.domain.token.RefreshTokenRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

/**
 * 시나리오 4: EmployeeResigned Kafka 이벤트 수신 → UserAccount 비활성화 동기화
 *
 * 1. UserAccount 생성 + 로그인 → access token + refresh token
 * 2. employee-service의 EmployeeResignedEvent를 event.hr.employee.v1 토픽으로 직접 발행
 * 3. auth-service의 E2eEmployeeEventWorker가 수신 → UserAccountSyncService.handleResigned 호출
 * 4. UserAccount.status = DEACTIVATED 확인
 * 5. 모든 RefreshToken revoked 확인
 * 6. UserDeactivatedEvent가 event.hr.auth.v1 토픽에 발행 검증
 * 7. 구 access token으로 /auth/me 호출 → 401 (계정 비활성화)
 */
class EmployeeResignedSyncScenarioTest(
    environment: Environment,
    @Autowired private val userAccountRepository: UserAccountRepository,
    @Autowired private val refreshTokenRepository: RefreshTokenRepository,
    @Autowired private val passwordEncoder: PasswordEncoder,
    @Autowired private val emailHashService: EmailHashService,
    @Autowired private val kafkaTemplate: KafkaTemplate<String, String>,
) : BaseE2eWithKafkaWorkerTest(environment) {

    private val eventMapper: ObjectMapper = ObjectMapper().also { it.findAndRegisterModules() }

    private fun jsonHeaders(): HttpHeaders = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
    }

    init {
        given("ACTIVE 상태의 UserAccount가 존재하고 로그인되어 있을 때") {
            val uniqueSuffix = System.nanoTime()
            val rawPassword = "TestPassword123!"
            val email = "resigned-scenario-$uniqueSuffix@example.com"
            val emailHash = emailHashService.hash(email)
            val employmentId = (UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE)

            userAccountRepository.save(
                UserAccount.create(
                    employmentId = employmentId,
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
            loginResponse.statusCode shouldBe HttpStatus.OK
            val accessToken = requireNotNull(loginResponse.body)["accessToken"] as String

            `when`("event.hr.employee.v1 토픽에 EmployeeResignedEvent를 발행하면") {
                val authTopic = "event.hr.auth.v1"
                val authConsumer = buildKafkaConsumer()
                awaitPartitionAssignment(authConsumer, authTopic)

                val envelopePayload = buildResignedEventPayload(employmentId, companyId = 1L)
                kafkaTemplate.send("event.hr.employee.v1", employmentId.toString(), envelopePayload).get()

                // E2eEmployeeEventWorker가 메시지를 수신하여 처리할 시간 polling 대기
                val maxWaitMs = 30_000L
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < maxWaitMs) {
                    val polledAccount = userAccountRepository.findByEmailHash(emailHash)
                    if (polledAccount?.status == UserAccountStatus.DEACTIVATED) break
                    Thread.sleep(200)
                }

                then("UserAccount.status가 DEACTIVATED로 변경된다") {
                    val deactivatedAccount = userAccountRepository.findByEmailHash(emailHash)
                    deactivatedAccount shouldNotBe null
                    requireNotNull(deactivatedAccount).status shouldBe UserAccountStatus.DEACTIVATED
                }

                then("모든 RefreshToken이 revoked된다") {
                    val savedAccount = userAccountRepository.findByEmailHash(emailHash)
                    savedAccount shouldNotBe null
                    val accountId = requireNotNull(savedAccount).id
                    requireNotNull(accountId)

                    val activeTokens = refreshTokenRepository.findActiveByUserAccountId(accountId)
                    activeTokens shouldBe emptyList()
                }

                then("event.hr.auth.v1 토픽에 UserDeactivatedEvent가 발행된다") {
                    val deactivatedEvents = pollMessagesByEventType(
                        consumer = authConsumer,
                        eventTypes = setOf("UserDeactivated"),
                        expectedCount = 1,
                        timeoutMs = 20_000L,
                    )
                    authConsumer.close()
                    deactivatedEvents.size shouldBe 1
                    deactivatedEvents.first()["eventType"] shouldBe "UserDeactivated"
                }

                then("구 access token으로 GET /auth/me를 호출하면 401이 반환된다") {
                    val headers = HttpHeaders().apply {
                        set("Authorization", "Bearer $accessToken")
                    }
                    val meResponse = restTemplate.exchange(
                        baseUrl("/auth/me"),
                        HttpMethod.GET,
                        HttpEntity<Void>(headers),
                        Map::class.java,
                    )
                    meResponse.statusCode shouldBe HttpStatus.UNAUTHORIZED
                }
            }
        }
    }

    private fun buildResignedEventPayload(employmentId: Long, companyId: Long): String {
        val envelope = mapOf(
            "eventId" to UUID.randomUUID().toString(),
            "eventType" to "EmployeeResigned",
            "eventVersion" to 1,
            "occurredAt" to ZonedDateTime.now(ZoneOffset.UTC).toString(),
            "aggregateType" to "Employment",
            "aggregateId" to employmentId,
            "companyId" to companyId,
            "actorEmploymentId" to null,
            "action" to mapOf(
                "type" to "EmployeeResigned",
                "details" to mapOf("reason" to "voluntary_resignation"),
            ),
            "state" to mapOf(
                "status" to "RESIGNED",
                "snapshot" to emptyMap<String, Any?>(),
            ),
        )
        return eventMapper.writeValueAsString(envelope)
    }
}
