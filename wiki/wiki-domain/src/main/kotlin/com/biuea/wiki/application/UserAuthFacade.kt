package com.biuea.wiki.application

import com.biuea.wiki.domain.auth.AuthenticatedUser
import com.biuea.wiki.domain.auth.JwtTokenProvider
import com.biuea.wiki.domain.auth.RefreshToken
import com.biuea.wiki.domain.auth.exception.InvalidRefreshTokenException
import com.biuea.wiki.domain.user.DeleteUserCommand
import com.biuea.wiki.domain.user.LoginUserCommand
import com.biuea.wiki.domain.user.SignUpUserCommand
import com.biuea.wiki.domain.user.UserService
import com.biuea.wiki.infrastructure.auth.RedisAuthTokenStore
import com.biuea.wiki.infrastructure.auth.RefreshTokenRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneId
import java.util.UUID

@Component
class UserAuthFacade(
    private val userService: UserService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val redisAuthTokenStore: RedisAuthTokenStore,
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    @Transactional
    fun signUp(input: SignUpUserInput): UserOutput {
        val user = userService.signUp(
            SignUpUserCommand(
                email = input.email,
                password = input.password,
                name = input.name,
            )
        )
        return UserOutput.of(user)
    }

    @Transactional
    fun login(input: LoginUserInput): LoginOutput {
        val user = userService.login(LoginUserCommand(email = input.email, password = input.password))
        val userOutput = UserOutput.of(user)

        val familyId = UUID.randomUUID().toString()
        val accessToken = jwtTokenProvider.createAccessToken(
            AuthenticatedUser(id = userOutput.id, email = userOutput.email, name = userOutput.name)
        )
        val refreshToken = jwtTokenProvider.createRefreshToken(userOutput.id)
        val refreshTokenExpiration = requireNotNull(jwtTokenProvider.getExpirationTime(refreshToken))

        // DB에 refresh token 저장 (rotation 정책)
        refreshTokenRepository.save(
            RefreshToken(
                tokenHash = RefreshToken.hashToken(refreshToken),
                userId = userOutput.id,
                familyId = familyId,
                expiresAt = refreshTokenExpiration,
            )
        )

        return LoginOutput(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = "Bearer",
            user = userOutput,
        )
    }

    @Transactional
    fun refresh(input: RefreshUserInput): RefreshOutput {
        if (!jwtTokenProvider.validateToken(input.refreshToken) || !jwtTokenProvider.isRefreshToken(input.refreshToken)) {
            throw InvalidRefreshTokenException()
        }

        val tokenHash = RefreshToken.hashToken(input.refreshToken)
        val storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
            ?: throw InvalidRefreshTokenException()

        // 탈취 감지: revoked 토큰 재사용 시 동일 family 전체 무효화
        if (storedToken.isRevoked) {
            refreshTokenRepository.revokeAllByFamilyId(storedToken.familyId)
            throw InvalidRefreshTokenException()
        }

        if (storedToken.isExpired()) {
            storedToken.revoke()
            throw InvalidRefreshTokenException()
        }

        val userId = jwtTokenProvider.getUserId(input.refreshToken) ?: throw InvalidRefreshTokenException()
        val user = userService.findById(userId)
        val userOutput = UserOutput.of(user)

        // 기존 토큰 revoke
        storedToken.revoke()

        // 새 토큰 발급 (같은 family_id 유지 — rotation)
        val newRefreshToken = jwtTokenProvider.createRefreshToken(userOutput.id)
        val newRefreshTokenExpiration = requireNotNull(jwtTokenProvider.getExpirationTime(newRefreshToken))

        refreshTokenRepository.save(
            RefreshToken(
                tokenHash = RefreshToken.hashToken(newRefreshToken),
                userId = userOutput.id,
                familyId = storedToken.familyId,
                expiresAt = newRefreshTokenExpiration,
            )
        )

        val accessToken = jwtTokenProvider.createAccessToken(
            AuthenticatedUser(id = userOutput.id, email = userOutput.email, name = userOutput.name)
        )

        return RefreshOutput(
            accessToken = accessToken,
            refreshToken = newRefreshToken,
            tokenType = "Bearer",
        )
    }

    @Transactional
    fun logout(input: LogoutUserInput) {
        jwtTokenProvider.resolveToken(input.authorizationHeader)
            ?.let { accessToken ->
                jwtTokenProvider.getExpirationTime(accessToken)
                    ?.let { expiresAt ->
                        redisAuthTokenStore.blacklistAccessToken(accessToken, expiresAt)
                    }
            }
        input.refreshToken?.let { refreshToken ->
            val tokenHash = RefreshToken.hashToken(refreshToken)
            refreshTokenRepository.findByTokenHash(tokenHash)?.revoke()
        }
    }

    @Transactional
    fun delete(input: DeleteUserInput) {
        userService.delete(DeleteUserCommand(userId = input.userId))
        refreshTokenRepository.revokeAllByUserId(input.userId)
    }
}

data class SignUpUserInput(val email: String, val password: String, val name: String)
data class LoginUserInput(val email: String, val password: String)
data class RefreshUserInput(val refreshToken: String)
data class DeleteUserInput(val userId: Long)
data class LogoutUserInput(val authorizationHeader: String?, val refreshToken: String?)

data class UserOutput(val id: Long, val email: String, val name: String) {
    companion object {
        fun of(user: com.biuea.wiki.domain.user.User): UserOutput =
            UserOutput(id = user.id, email = user.email, name = user.name)
    }
}

data class LoginOutput(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val user: UserOutput,
)

data class RefreshOutput(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
)
