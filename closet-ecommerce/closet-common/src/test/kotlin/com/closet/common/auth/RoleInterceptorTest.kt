package com.closet.common.auth

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.method.HandlerMethod

class RoleInterceptorTest : BehaviorSpec({

    // 테스트용 컨트롤러 메서드를 시뮬레이션하기 위한 핸들러
    fun createHandlerMethod(roleRequired: RoleRequired?): HandlerMethod {
        val handler = mockk<HandlerMethod>()
        every { handler.getMethodAnnotation(RoleRequired::class.java) } returns roleRequired
        return handler
    }

    fun createRequest(role: String? = null): HttpServletRequest {
        val request = mockk<HttpServletRequest>()
        every { request.getHeader(RoleInterceptor.HEADER_MEMBER_ROLE) } returns role
        every { request.requestURI } returns "/api/v1/test"
        return request
    }

    val response = mockk<HttpServletResponse>()

    Given("ROLE_AUTHORIZATION_ENABLED=false (Feature Flag OFF)") {
        val interceptor = RoleInterceptor(roleAuthorizationEnabled = false)

        When("어떤 요청이 오더라도") {
            val handler = createHandlerMethod(RoleRequired(MemberRole.SELLER))
            val request = createRequest(role = "BUYER")

            Then("인가 체크를 스킵하고 통과시킨다") {
                val result = interceptor.preHandle(request, response, handler)
                result shouldBe true
            }
        }
    }

    Given("ROLE_AUTHORIZATION_ENABLED=true (Feature Flag ON)") {
        val interceptor = RoleInterceptor(roleAuthorizationEnabled = true)

        When("@RoleRequired 어노테이션이 없는 핸들러이면") {
            val handler = createHandlerMethod(roleRequired = null)
            val request = createRequest(role = "BUYER")

            Then("통과시킨다") {
                val result = interceptor.preHandle(request, response, handler)
                result shouldBe true
            }
        }

        When("SELLER 권한 API에 SELLER로 접근하면") {
            val handler = createHandlerMethod(RoleRequired(MemberRole.SELLER))
            val request = createRequest(role = "SELLER")

            Then("통과시킨다") {
                val result = interceptor.preHandle(request, response, handler)
                result shouldBe true
            }
        }

        When("SELLER 권한 API에 BUYER로 접근하면") {
            val handler = createHandlerMethod(RoleRequired(MemberRole.SELLER))
            val request = createRequest(role = "BUYER")

            Then("FORBIDDEN 예외가 발생한다") {
                val exception =
                    shouldThrow<BusinessException> {
                        interceptor.preHandle(request, response, handler)
                    }
                exception.errorCode shouldBe ErrorCode.FORBIDDEN
            }
        }

        When("SELLER 권한 API에 ADMIN으로 접근하면") {
            val handler = createHandlerMethod(RoleRequired(MemberRole.SELLER))
            val request = createRequest(role = "ADMIN")

            Then("ADMIN은 모든 권한을 가지므로 통과시킨다") {
                val result = interceptor.preHandle(request, response, handler)
                result shouldBe true
            }
        }

        When("복수 역할(BUYER, SELLER) 허용 API에 BUYER로 접근하면") {
            val handler = createHandlerMethod(RoleRequired(MemberRole.BUYER, MemberRole.SELLER))
            val request = createRequest(role = "BUYER")

            Then("OR 조건으로 통과시킨다") {
                val result = interceptor.preHandle(request, response, handler)
                result shouldBe true
            }
        }

        When("role 헤더가 없으면 (레거시 호환)") {
            val handler = createHandlerMethod(RoleRequired(MemberRole.BUYER))
            val request = createRequest(role = null)

            Then("기본값 BUYER로 처리하여 통과시킨다") {
                shouldNotThrowAny {
                    interceptor.preHandle(request, response, handler)
                }
            }
        }

        When("핸들러가 HandlerMethod가 아니면") {
            val nonHandlerMethod = "not-a-handler-method"
            val request = createRequest(role = "BUYER")

            Then("통과시킨다") {
                val result = interceptor.preHandle(request, response, nonHandlerMethod)
                result shouldBe true
            }
        }
    }
})
