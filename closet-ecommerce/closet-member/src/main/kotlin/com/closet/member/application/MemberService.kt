package com.closet.member.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.member.config.JwtTokenProvider
import com.closet.member.domain.Member
import com.closet.member.domain.repository.MemberRepository
import com.closet.member.presentation.dto.LoginRequest
import com.closet.member.presentation.dto.LoginResponse
import com.closet.member.presentation.dto.MemberResponse
import com.closet.member.presentation.dto.RegisterRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    /** 회원가입 */
    @Transactional
    fun register(request: RegisterRequest): MemberResponse {
        // 이메일 중복 검사
        if (memberRepository.existsByEmail(request.email)) {
            throw BusinessException(ErrorCode.DUPLICATE_ENTITY, "이미 사용 중인 이메일입니다")
        }

        val member = Member.register(
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password),
            name = request.name,
            phone = request.phone,
        )

        val saved = memberRepository.save(member)
        return MemberResponse.from(saved)
    }

    /** 로그인 */
    fun login(request: LoginRequest): LoginResponse {
        val member = memberRepository.findByEmail(request.email)
            ?: throw BusinessException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다")

        if (member.isDeleted()) {
            throw BusinessException(ErrorCode.UNAUTHORIZED, "탈퇴한 회원입니다")
        }

        if (!passwordEncoder.matches(request.password, member.passwordHash)) {
            throw BusinessException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다")
        }

        val accessToken = jwtTokenProvider.generateAccessToken(member.id, member.role)
        val refreshToken = jwtTokenProvider.generateRefreshToken(member.id, member.role)

        return LoginResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            memberId = member.id,
        )
    }

    /** 내 정보 조회 */
    fun findById(memberId: Long): MemberResponse {
        val member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
            ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "회원을 찾을 수 없습니다")

        return MemberResponse.from(member)
    }

    /** 회원 탈퇴 */
    @Transactional
    fun withdraw(memberId: Long) {
        val member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
            ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "회원을 찾을 수 없습니다")

        member.withdraw()
    }
}
