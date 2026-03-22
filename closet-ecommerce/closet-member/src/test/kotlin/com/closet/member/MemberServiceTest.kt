package com.closet.member

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.member.application.MemberService
import com.closet.member.config.JwtTokenProvider
import com.closet.member.domain.Member
import com.closet.member.domain.MemberGrade
import com.closet.member.domain.MemberStatus
import com.closet.member.domain.repository.MemberRepository
import com.closet.member.presentation.dto.LoginRequest
import com.closet.member.presentation.dto.RegisterRequest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime

class MemberServiceTest : BehaviorSpec({
    val memberRepository = mockk<MemberRepository>()
    val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()
    val jwtTokenProvider = mockk<JwtTokenProvider>()

    val memberService = MemberService(memberRepository, passwordEncoder, jwtTokenProvider)

    Given("회원가입 요청이 주어졌을 때") {
        val request = RegisterRequest(
            email = "test@closet.com",
            password = "password123",
            name = "테스트",
            phone = "010-1234-5678",
        )

        When("중복되지 않은 이메일이면") {
            every { memberRepository.existsByEmail(request.email) } returns false

            val memberSlot = slot<Member>()
            every { memberRepository.save(capture(memberSlot)) } answers {
                memberSlot.captured.apply {
                    createdAt = LocalDateTime.now()
                    updatedAt = LocalDateTime.now()
                }
            }

            val result = memberService.register(request)

            Then("회원이 정상적으로 생성된다") {
                result.email shouldBe request.email
                result.name shouldBe request.name
                result.grade shouldBe MemberGrade.NORMAL
                result.status shouldBe MemberStatus.ACTIVE
                result.pointBalance shouldBe 0
            }
        }

        When("이미 존재하는 이메일이면") {
            every { memberRepository.existsByEmail(request.email) } returns true

            Then("DUPLICATE_ENTITY 예외가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    memberService.register(request)
                }
                exception.errorCode shouldBe ErrorCode.DUPLICATE_ENTITY
            }
        }
    }

    Given("로그인 요청이 주어졌을 때") {
        val rawPassword = "password123"
        val encodedPassword = passwordEncoder.encode(rawPassword)
        val loginRequest = LoginRequest(
            email = "test@closet.com",
            password = rawPassword,
        )

        When("올바른 이메일과 비밀번호이면") {
            val member = Member.register(
                email = loginRequest.email,
                passwordHash = encodedPassword,
                name = "테스트",
            )

            every { memberRepository.findByEmail(loginRequest.email) } returns member
            every { jwtTokenProvider.generateAccessToken(any()) } returns "access-token"
            every { jwtTokenProvider.generateRefreshToken(any()) } returns "refresh-token"

            val result = memberService.login(loginRequest)

            Then("JWT 토큰이 발급된다") {
                result.accessToken shouldNotBe null
                result.refreshToken shouldNotBe null
                result.accessToken shouldBe "access-token"
                result.refreshToken shouldBe "refresh-token"
            }
        }

        When("존재하지 않는 이메일이면") {
            every { memberRepository.findByEmail("wrong@closet.com") } returns null

            val wrongRequest = LoginRequest(email = "wrong@closet.com", password = rawPassword)

            Then("UNAUTHORIZED 예외가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    memberService.login(wrongRequest)
                }
                exception.errorCode shouldBe ErrorCode.UNAUTHORIZED
            }
        }
    }
})
