package com.closet.member.presentation.dto

import com.closet.member.domain.Member
import com.closet.member.domain.MemberGrade
import com.closet.member.domain.MemberStatus
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.ZonedDateTime

/** 회원가입 요청 */
data class RegisterRequest(
    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "이메일 형식이 올바르지 않습니다")
    val email: String,
    @field:NotBlank(message = "비밀번호는 필수입니다")
    @field:Size(min = 8, max = 50, message = "비밀번호는 8~50자여야 합니다")
    val password: String,
    @field:NotBlank(message = "이름은 필수입니다")
    @field:Size(min = 2, max = 50, message = "이름은 2~50자여야 합니다")
    val name: String,
    val phone: String? = null,
)

/** 로그인 요청 */
data class LoginRequest(
    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "이메일 형식이 올바르지 않습니다")
    val email: String,
    @field:NotBlank(message = "비밀번호는 필수입니다")
    val password: String,
)

/** 로그인 응답 */
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val memberId: Long,
)

/** 토큰 갱신 요청 */
data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh Token은 필수입니다")
    val refreshToken: String,
)

/** 회원 정보 응답 */
data class MemberResponse(
    val id: Long,
    val email: String,
    val name: String,
    val phone: String?,
    val grade: MemberGrade,
    val pointBalance: Int,
    val status: MemberStatus,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(member: Member): MemberResponse =
            MemberResponse(
                id = member.id,
                email = member.email,
                name = member.name,
                phone = member.phone,
                grade = member.grade,
                pointBalance = member.pointBalance,
                status = member.status,
                createdAt = member.createdAt,
            )
    }
}
