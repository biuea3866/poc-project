package com.hrplatform.auth.presentation.auth

import com.hrplatform.auth.domain.auth.service.JwtClaims
import com.hrplatform.auth.domain.auth.service.JwtTokenService
import com.hrplatform.auth.domain.role.Role
import com.hrplatform.auth.domain.role.service.RoleDomainService
import com.hrplatform.auth.domain.token.JtiBlacklist
import io.jsonwebtoken.JwtException
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder

class JwtAuthenticationFilterTest : BehaviorSpec({

    val jwtTokenService = mockk<JwtTokenService>()
    val jtiBlacklist = mockk<JtiBlacklist>()
    val roleDomainService = mockk<RoleDomainService>()

    val filter = JwtAuthenticationFilter(
        jwtTokenService = jwtTokenService,
        jtiBlacklist = jtiBlacklist,
        roleDomainService = roleDomainService,
    )

    beforeEach { SecurityContextHolder.clearContext() }
    afterEach { SecurityContextHolder.clearContext() }

    given("Authorization 헤더 없음") {
        then("필터 체인을 통과하고 SecurityContext는 비어있다") {
            val filterChain = mockk<FilterChain>(relaxed = true)
            val request = MockHttpServletRequest()
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            verify { filterChain.doFilter(request, response) }
            SecurityContextHolder.getContext().authentication shouldBe null
            response.status shouldBe 200
        }
    }

    given("유효한 JWT — SecurityContext에 authorities와 함께 적재") {
        then("SecurityContext에 JwtAuthenticationToken이 적재되고 필터 체인을 통과한다") {
            val filterChain = mockk<FilterChain>(relaxed = true)
            val request = MockHttpServletRequest()
            request.addHeader("Authorization", "Bearer valid-token")
            val response = MockHttpServletResponse()

            val claims = JwtClaims(userAccountId = 42L, jti = "jti-uuid-1", employmentId = 100L)
            val role = mockk<Role>()
            every { role.code } returns "HR_MANAGER"
            every { jwtTokenService.verifyAccessToken("valid-token") } returns claims
            every { jtiBlacklist.contains("jti-uuid-1") } returns false
            every { roleDomainService.findUserRoles(42L) } returns listOf(role)

            filter.doFilter(request, response, filterChain)

            verify { filterChain.doFilter(request, response) }
            val authentication = SecurityContextHolder.getContext().authentication
            authentication shouldNotBe null
            val jwtAuthentication = authentication as JwtAuthenticationToken
            jwtAuthentication.userAccountId shouldBe 42L
            jwtAuthentication.employmentId shouldBe 100L
            jwtAuthentication.authorities.map { it.authority } shouldBe listOf("ROLE_HR_MANAGER")
        }
    }

    given("blacklist에 등록된 jti") {
        then("401 JSON body 반환, 필터 체인 미통과") {
            val filterChain = mockk<FilterChain>(relaxed = true)
            val request = MockHttpServletRequest()
            request.addHeader("Authorization", "Bearer blacklisted-token")
            val response = MockHttpServletResponse()

            val claims = JwtClaims(userAccountId = 42L, jti = "blacklisted-jti", employmentId = null)
            every { jwtTokenService.verifyAccessToken("blacklisted-token") } returns claims
            every { jtiBlacklist.contains("blacklisted-jti") } returns true

            filter.doFilter(request, response, filterChain)

            verify(exactly = 0) { filterChain.doFilter(any(), any()) }
            response.status shouldBe 401
            response.contentType shouldBe "application/json;charset=UTF-8"
            response.contentAsString.contains("E_AUTH_BLACKLISTED") shouldBe true
        }
    }

    given("만료된 JWT (JwtException 발생)") {
        then("401 JSON body 반환, 필터 체인 미통과") {
            val filterChain = mockk<FilterChain>(relaxed = true)
            val request = MockHttpServletRequest()
            request.addHeader("Authorization", "Bearer expired-token")
            val response = MockHttpServletResponse()

            every { jwtTokenService.verifyAccessToken("expired-token") } throws JwtException("토큰이 만료되었습니다")

            filter.doFilter(request, response, filterChain)

            verify(exactly = 0) { filterChain.doFilter(any(), any()) }
            response.status shouldBe 401
            response.contentType shouldBe "application/json;charset=UTF-8"
            response.contentAsString.contains("E_AUTH_INVALID") shouldBe true
        }
    }

    given("jti 클레임 누락 (JwtException 발생)") {
        then("401 JSON body 반환, 필터 체인 미통과") {
            val filterChain = mockk<FilterChain>(relaxed = true)
            val request = MockHttpServletRequest()
            request.addHeader("Authorization", "Bearer no-jti-token")
            val response = MockHttpServletResponse()

            every { jwtTokenService.verifyAccessToken("no-jti-token") } throws JwtException("jti 클레임 누락")

            filter.doFilter(request, response, filterChain)

            verify(exactly = 0) { filterChain.doFilter(any(), any()) }
            response.status shouldBe 401
            response.contentAsString.contains("E_AUTH_INVALID") shouldBe true
        }
    }
})
