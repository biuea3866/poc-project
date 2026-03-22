package com.closet.member.presentation

import com.closet.common.response.ApiResponse
import com.closet.member.application.AuthService
import com.closet.member.application.MemberService
import com.closet.member.config.JwtAuthenticationFilter
import com.closet.member.presentation.dto.LoginRequest
import com.closet.member.presentation.dto.LoginResponse
import com.closet.member.presentation.dto.MemberResponse
import com.closet.member.presentation.dto.RefreshTokenRequest
import com.closet.member.presentation.dto.RegisterRequest
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/members")
class MemberController(
    private val memberService: MemberService,
    private val authService: AuthService,
) {
    /** 회원가입 */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest): ApiResponse<MemberResponse> {
        return ApiResponse.created(memberService.register(request))
    }

    /** 로그인 */
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ApiResponse<LoginResponse> {
        return ApiResponse.ok(memberService.login(request))
    }

    /** 내 정보 조회 */
    @GetMapping("/me")
    fun getMe(request: HttpServletRequest): ApiResponse<MemberResponse> {
        val memberId = request.getAttribute(JwtAuthenticationFilter.MEMBER_ID_ATTRIBUTE) as Long
        return ApiResponse.ok(memberService.findById(memberId))
    }

    /** 회원 탈퇴 */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun withdraw(request: HttpServletRequest) {
        val memberId = request.getAttribute(JwtAuthenticationFilter.MEMBER_ID_ATTRIBUTE) as Long
        memberService.withdraw(memberId)
    }

    /** 토큰 갱신 */
    @PostMapping("/auth/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ApiResponse<LoginResponse> {
        return ApiResponse.ok(authService.refresh(request))
    }
}
