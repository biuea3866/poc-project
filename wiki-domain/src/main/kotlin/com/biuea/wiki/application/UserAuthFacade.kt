package com.biuea.wiki.application

import com.biuea.wiki.domain.auth.AuthenticatedUser
import com.biuea.wiki.domain.auth.JwtTokenProvider
import com.biuea.wiki.domain.auth.exception.InvalidRefreshTokenException
import com.biuea.wiki.domain.user.DeleteUserCommand
import com.biuea.wiki.domain.user.LoginUserCommand
import com.biuea.wiki.domain.user.SignUpUserCommand
import com.biuea.wiki.domain.user.UserService
import com.biuea.wiki.infrastructure.auth.RedisAuthTokenStore
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserAuthFacade(
    private val userService: UserService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val redisAuthTokenStore: RedisAuthTokenStore,
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

        val accessToken = jwtTokenProvider.createAccessToken(
            AuthenticatedUser(id = userOutput.id, email = userOutput.email, name = userOutput.name)
        )
        val refreshToken = jwtTokenProvider.createRefreshToken(userOutput.id)
        val refreshTokenExpiration = requireNotNull(jwtTokenProvider.getExpirationTime(refreshToken))

        redisAuthTokenStore.saveRefreshToken(
            userId = userOutput.id,
            refreshToken = refreshToken,
            expiresAt = refreshTokenExpiration,
        )

        return LoginOutput(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = "Bearer",
            user = userOutput,
        )
    }

    @Transactional(readOnly = true)
    fun refresh(input: RefreshUserInput): RefreshOutput {
        if (!jwtTokenProvider.validateToken(input.refreshToken) || !jwtTokenProvider.isRefreshToken(input.refreshToken)) {
            throw InvalidRefreshTokenException()
        }
        if (!redisAuthTokenStore.validateRefreshToken(input.refreshToken)) {
            throw InvalidRefreshTokenException()
        }

        val userId = jwtTokenProvider.getUserId(input.refreshToken) ?: throw InvalidRefreshTokenException()
        val user = userService.findById(userId)
        val userOutput = UserOutput.of(user)

        val accessToken = jwtTokenProvider.createAccessToken(
            AuthenticatedUser(id = userOutput.id, email = userOutput.email, name = userOutput.name)
        )

        return RefreshOutput(accessToken = accessToken, tokenType = "Bearer")
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
        input.refreshToken?.let { redisAuthTokenStore.revokeRefreshToken(it) }
    }

    @Transactional
    fun delete(input: DeleteUserInput) {
        userService.delete(DeleteUserCommand(userId = input.userId))
        redisAuthTokenStore.revokeAllRefreshTokensByUserId(input.userId)
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
    val tokenType: String,
)
