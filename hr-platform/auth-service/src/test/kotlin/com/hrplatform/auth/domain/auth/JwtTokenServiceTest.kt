package com.hrplatform.auth.domain.auth

import com.hrplatform.auth.domain.auth.service.JwtTokenService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Base64

class JwtTokenServiceTest : BehaviorSpec({

    val base64Secret = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })

    fun buildService(): JwtTokenService = JwtTokenService(
        base64Secret = base64Secret,
        issuer = "hr-platform",
        audience = "hr-platform-users",
        accessTokenExpiryMinutes = 30L,
        refreshTokenExpiryDays = 14L,
    )

    given("did claim 인코딩/디코딩 — departmentId 존재") {
        val service = buildService()
        val now = ZonedDateTime.now(ZoneOffset.UTC)

        `when`("issueTokenPair에 departmentId=42를 전달하면") {
            val tokenPair = service.issueTokenPair(
                userAccountId = 1L,
                employmentId = 100L,
                departmentId = 42L,
                now = now,
            )

            then("verifyAccessToken으로 추출한 JwtClaims.departmentId가 42이다") {
                val claims = service.verifyAccessToken(tokenPair.accessToken)
                claims.departmentId shouldBe 42L
            }

            then("verifyAccessToken으로 추출한 userAccountId와 employmentId도 정확하다") {
                val claims = service.verifyAccessToken(tokenPair.accessToken)
                claims.userAccountId shouldBe 1L
                claims.employmentId shouldBe 100L
            }
        }
    }

    given("did claim 인코딩/디코딩 — departmentId null (부서 미배치 직원)") {
        val service = buildService()
        val now = ZonedDateTime.now(ZoneOffset.UTC)

        `when`("issueTokenPair에 departmentId=null을 전달하면") {
            val tokenPair = service.issueTokenPair(
                userAccountId = 1L,
                employmentId = 100L,
                departmentId = null,
                now = now,
            )

            then("verifyAccessToken으로 추출한 JwtClaims.departmentId가 null이다") {
                val claims = service.verifyAccessToken(tokenPair.accessToken)
                claims.departmentId shouldBe null
            }
        }
    }

    given("did claim 인코딩/디코딩 — employmentId null (비직원 계정)") {
        val service = buildService()
        val now = ZonedDateTime.now(ZoneOffset.UTC)

        `when`("issueTokenPair에 employmentId=null, departmentId=10을 전달하면") {
            val tokenPair = service.issueTokenPair(
                userAccountId = 1L,
                employmentId = null,
                departmentId = 10L,
                now = now,
            )

            then("did claim이 없어야 하므로 JwtClaims.departmentId가 null이다") {
                val claims = service.verifyAccessToken(tokenPair.accessToken)
                claims.departmentId shouldBe null
            }
        }
    }

    given("기존 토큰 하위 호환 — did 없는 구형 토큰") {
        val service = buildService()
        val now = ZonedDateTime.now(ZoneOffset.UTC)

        `when`("departmentId 없이 발급된 구형 토큰을 검증하면") {
            // eid만 있고 did가 없는 구형 토큰 시뮬레이션 — departmentId=null로 발급
            val tokenPair = service.issueTokenPair(
                userAccountId = 2L,
                employmentId = 200L,
                departmentId = null,
                now = now,
            )

            then("JwtClaims.departmentId가 null이고 예외가 발생하지 않는다") {
                val claims = service.verifyAccessToken(tokenPair.accessToken)
                claims.departmentId shouldBe null
                claims.userAccountId shouldBe 2L
                claims shouldNotBe null
            }
        }
    }
})
