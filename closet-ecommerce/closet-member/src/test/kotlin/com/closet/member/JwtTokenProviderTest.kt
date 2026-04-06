package com.closet.member

import com.closet.common.auth.MemberRole
import com.closet.member.config.JwtTokenProvider
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class JwtTokenProviderTest : BehaviorSpec({

    val secret = "closet-member-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256"
    val accessTokenExpiryMs = 1800000L // 30분
    val refreshTokenExpiryMs = 604800000L // 7일

    val jwtTokenProvider =
        JwtTokenProvider(
            secret = secret,
            accessTokenExpiryMs = accessTokenExpiryMs,
            refreshTokenExpiryMs = refreshTokenExpiryMs,
        )

    Given("Access Token 생성") {

        When("role=BUYER로 토큰을 생성하면") {
            val token = jwtTokenProvider.generateAccessToken(1L, MemberRole.BUYER)

            Then("토큰에서 memberId와 role을 추출할 수 있다") {
                token shouldNotBe null
                jwtTokenProvider.extractMemberId(token) shouldBe 1L
                jwtTokenProvider.extractRole(token) shouldBe MemberRole.BUYER
                jwtTokenProvider.validate(token) shouldBe true
            }
        }

        When("role=SELLER로 토큰을 생성하면") {
            val token = jwtTokenProvider.generateAccessToken(2L, MemberRole.SELLER)

            Then("role=SELLER이 포함된다") {
                jwtTokenProvider.extractRole(token) shouldBe MemberRole.SELLER
                jwtTokenProvider.extractMemberId(token) shouldBe 2L
            }
        }

        When("role=ADMIN으로 토큰을 생성하면") {
            val token = jwtTokenProvider.generateAccessToken(3L, MemberRole.ADMIN)

            Then("role=ADMIN이 포함된다") {
                jwtTokenProvider.extractRole(token) shouldBe MemberRole.ADMIN
            }
        }

        When("role 파라미터 없이 (기본값) 토큰을 생성하면") {
            val token = jwtTokenProvider.generateAccessToken(4L)

            Then("기본값 BUYER가 적용된다") {
                jwtTokenProvider.extractRole(token) shouldBe MemberRole.BUYER
            }
        }
    }

    Given("Refresh Token 생성") {

        When("role=SELLER로 Refresh Token을 생성하면") {
            val token = jwtTokenProvider.generateRefreshToken(5L, MemberRole.SELLER)

            Then("토큰에서 role을 추출할 수 있다") {
                jwtTokenProvider.extractRole(token) shouldBe MemberRole.SELLER
                jwtTokenProvider.validate(token) shouldBe true
            }
        }
    }

    Given("레거시 JWT 하위 호환") {

        When("만료된 토큰을 검증하면") {
            val expiredProvider =
                JwtTokenProvider(
                    secret = secret,
                    // 이미 만료
                    accessTokenExpiryMs = -1000L,
                    refreshTokenExpiryMs = refreshTokenExpiryMs,
                )
            val token = expiredProvider.generateAccessToken(1L, MemberRole.BUYER)

            Then("validate가 false를 반환한다") {
                jwtTokenProvider.validate(token) shouldBe false
            }
        }
    }
})
