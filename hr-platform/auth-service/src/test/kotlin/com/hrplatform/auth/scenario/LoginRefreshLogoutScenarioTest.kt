package com.hrplatform.auth.scenario

import com.hrplatform.auth.domain.account.EmailHashService
import com.hrplatform.auth.domain.account.UserAccount
import com.hrplatform.auth.domain.account.UserAccountRepository
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
 * 시나리오 1: 로그인 → 토큰 갱신 → 로그아웃 → 구 토큰 401 검증
 *
 * 1. UserAccount 신규 생성 (status=ACTIVE)
 * 2. POST /auth/login → 200 + access + refresh
 * 3. GET /auth/me with access token → 200 (본인 정보)
 * 4. POST /auth/refresh (refresh) → 새 access·refresh
 * 5. POST /auth/logout with new refresh token → 204 (new access token이 블랙리스트에 추가됨)
 * 6. GET /auth/me with new access token → 401 (jti blacklist)
 */
class LoginRefreshLogoutScenarioTest(
    environment: Environment,
    @Autowired private val userAccountRepository: UserAccountRepository,
    @Autowired private val passwordEncoder: PasswordEncoder,
    @Autowired private val emailHashService: EmailHashService,
) : BaseE2eTest(environment) {

    private fun jsonHeaders(): HttpHeaders = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
    }

    private fun createAccount(): Triple<Long, String, String> {
        val rawPassword = "TestPassword123!"
        val email = "login-scenario-${System.nanoTime()}@example.com"
        val emailHash = emailHashService.hash(email)
        val userAccount = userAccountRepository.save(
            UserAccount.create(
                employmentId = 10001L,
                companyId = 1L,
                email = email,
                emailHash = emailHash,
                passwordHash = passwordEncoder.encode(rawPassword),
            ),
        )
        return Triple(requireNotNull(userAccount.id), email, rawPassword)
    }

    init {
        given("ACTIVE 상태의 UserAccount가 존재할 때") {
            `when`("POST /auth/login으로 로그인하면") {
                val (userAccountId, email, rawPassword) = createAccount()
                val loginResponse = restTemplate.postForEntity(
                    baseUrl("/auth/login"),
                    HttpEntity(mapOf("email" to email, "password" to rawPassword), jsonHeaders()),
                    Map::class.java,
                )

                then("200 응답과 access/refresh token이 반환된다") {
                    loginResponse.statusCode shouldBe HttpStatus.OK
                    val body = requireNotNull(loginResponse.body)
                    body["accessToken"] shouldNotBe null
                    body["refreshToken"] shouldNotBe null
                    (body["requiresTwoFactor"] as Boolean) shouldBe false
                }
            }

            `when`("로그인 후 GET /auth/me에 access token을 담아 요청하면") {
                val (userAccountId, email, rawPassword) = createAccount()
                val loginResponse = restTemplate.postForEntity(
                    baseUrl("/auth/login"),
                    HttpEntity(mapOf("email" to email, "password" to rawPassword), jsonHeaders()),
                    Map::class.java,
                )
                val accessToken = requireNotNull(loginResponse.body)["accessToken"] as String

                val headers = HttpHeaders().apply {
                    set("Authorization", "Bearer $accessToken")
                }
                val meResponse = restTemplate.exchange(
                    baseUrl("/auth/me"),
                    HttpMethod.GET,
                    HttpEntity<Void>(headers),
                    Map::class.java,
                )

                then("200 응답과 본인 계정 정보가 반환된다") {
                    meResponse.statusCode shouldBe HttpStatus.OK
                    val meBody = requireNotNull(meResponse.body)
                    (meBody["userAccountId"] as Number).toLong() shouldBe userAccountId
                    meBody["status"] shouldBe "ACTIVE"
                }
            }

            `when`("로그인 후 POST /auth/refresh로 토큰을 갱신하면") {
                val (_, email, rawPassword) = createAccount()
                val loginResponse = restTemplate.postForEntity(
                    baseUrl("/auth/login"),
                    HttpEntity(mapOf("email" to email, "password" to rawPassword), jsonHeaders()),
                    Map::class.java,
                )
                val refreshToken = requireNotNull(loginResponse.body)["refreshToken"] as String

                val refreshResponse = restTemplate.postForEntity(
                    baseUrl("/auth/refresh"),
                    HttpEntity(mapOf("refreshToken" to refreshToken), jsonHeaders()),
                    Map::class.java,
                )

                then("200 응답과 새 access·refresh token이 반환된다") {
                    refreshResponse.statusCode shouldBe HttpStatus.OK
                    val refreshBody = requireNotNull(refreshResponse.body)
                    refreshBody["accessToken"] shouldNotBe null
                    refreshBody["refreshToken"] shouldNotBe null
                }
            }

            `when`("로그인 후 POST /auth/logout으로 로그아웃하면") {
                val (_, email, rawPassword) = createAccount()
                val loginResponse = restTemplate.postForEntity(
                    baseUrl("/auth/login"),
                    HttpEntity(mapOf("email" to email, "password" to rawPassword), jsonHeaders()),
                    Map::class.java,
                )
                val accessToken = requireNotNull(loginResponse.body)["accessToken"] as String
                val refreshToken = requireNotNull(loginResponse.body)["refreshToken"] as String

                val logoutHeaders = HttpHeaders().apply {
                    set("Authorization", "Bearer $accessToken")
                    contentType = MediaType.APPLICATION_JSON
                }
                val logoutResponse = restTemplate.postForEntity(
                    baseUrl("/auth/logout"),
                    HttpEntity(mapOf("refreshToken" to refreshToken), logoutHeaders),
                    Void::class.java,
                )

                then("204 응답이 반환된다") {
                    logoutResponse.statusCode shouldBe HttpStatus.NO_CONTENT
                }
            }

            `when`("로그인-갱신-로그아웃 후 새 access token으로 GET /auth/me를 요청하면") {
                val (_, email, rawPassword) = createAccount()

                val loginResponse = restTemplate.postForEntity(
                    baseUrl("/auth/login"),
                    HttpEntity(mapOf("email" to email, "password" to rawPassword), jsonHeaders()),
                    Map::class.java,
                )
                val firstRefreshToken = requireNotNull(loginResponse.body)["refreshToken"] as String

                val refreshResponse = restTemplate.postForEntity(
                    baseUrl("/auth/refresh"),
                    HttpEntity(mapOf("refreshToken" to firstRefreshToken), jsonHeaders()),
                    Map::class.java,
                )
                val newAccessToken = requireNotNull(refreshResponse.body)["accessToken"] as String
                val newRefreshToken = requireNotNull(refreshResponse.body)["refreshToken"] as String

                val logoutHeaders = HttpHeaders().apply {
                    set("Authorization", "Bearer $newAccessToken")
                    contentType = MediaType.APPLICATION_JSON
                }
                restTemplate.postForEntity(
                    baseUrl("/auth/logout"),
                    HttpEntity(mapOf("refreshToken" to newRefreshToken), logoutHeaders),
                    Void::class.java,
                )

                val meHeaders = HttpHeaders().apply {
                    set("Authorization", "Bearer $newAccessToken")
                }
                val meAfterLogout = restTemplate.exchange(
                    baseUrl("/auth/me"),
                    HttpMethod.GET,
                    HttpEntity<Void>(meHeaders),
                    Map::class.java,
                )

                then("401 응답이 반환된다 (jti blacklist)") {
                    meAfterLogout.statusCode shouldBe HttpStatus.UNAUTHORIZED
                }
            }
        }
    }
}
