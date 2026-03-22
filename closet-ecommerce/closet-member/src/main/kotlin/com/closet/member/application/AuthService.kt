package com.closet.member.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.member.config.JwtTokenProvider
import com.closet.member.domain.repository.MemberRepository
import com.closet.member.presentation.dto.LoginResponse
import com.closet.member.presentation.dto.RefreshTokenRequest
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val jwtTokenProvider: JwtTokenProvider,
    private val memberRepository: MemberRepository,
) {
    /** Refresh Token으로 새 Access Token 발급 */
    fun refresh(request: RefreshTokenRequest): LoginResponse {
        if (!jwtTokenProvider.validate(request.refreshToken)) {
            throw BusinessException(ErrorCode.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다")
        }

        val memberId = jwtTokenProvider.extractMemberId(request.refreshToken)

        // 회원 존재 여부 확인
        val member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
            ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "회원을 찾을 수 없습니다")

        val accessToken = jwtTokenProvider.generateAccessToken(member.id)
        val refreshToken = jwtTokenProvider.generateRefreshToken(member.id)

        return LoginResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            memberId = member.id,
        )
    }
}
