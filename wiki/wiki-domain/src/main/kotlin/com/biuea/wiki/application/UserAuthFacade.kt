package com.biuea.wiki.application

import com.biuea.wiki.domain.user.DeleteUserCommand
import com.biuea.wiki.domain.user.LoginUserCommand
import com.biuea.wiki.domain.user.SignUpUserCommand
import com.biuea.wiki.domain.user.UserService
import com.biuea.wiki.infrastructure.security.AuthenticatedUser
import com.biuea.wiki.infrastructure.security.JwtTokenBlacklist
import com.biuea.wiki.infrastructure.security.JwtTokenProvider
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserAuthFacade(
    private val userService: UserService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtTokenBlacklist: JwtTokenBlacklist,
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

    @Transactional(readOnly = true)
    fun login(input: LoginUserInput): LoginOutput {
        val user = userService.login(
            LoginUserCommand(
                email = input.email,
                password = input.password,
            )
        )
        val userOutput = UserOutput.of(user)
        val accessToken = jwtTokenProvider.createAccessToken(
            AuthenticatedUser(
                id = userOutput.id,
                email = userOutput.email,
                name = userOutput.name,
            )
        )

        return LoginOutput(
            accessToken = accessToken,
            tokenType = "Bearer",
            user = userOutput,
        )
    }

    @Transactional(readOnly = true)
    fun logout(input: LogoutUserInput) {
        jwtTokenProvider.resolveToken(input.authorizationHeader)
            ?.let { token ->
                jwtTokenProvider.getExpirationTimeMillis(token)
                    ?.let { expiresAtMillis ->
                        jwtTokenBlacklist.blacklist(token, expiresAtMillis)
                    }
            }
    }

    @Transactional
    fun delete(input: DeleteUserInput) {
        userService.delete(DeleteUserCommand(userId = input.userId))
    }
}

data class SignUpUserInput(
    val email: String,
    val password: String,
    val name: String,
)

data class LoginUserInput(
    val email: String,
    val password: String,
)

data class DeleteUserInput(
    val userId: Long,
)

data class LogoutUserInput(
    val authorizationHeader: String?,
)

data class UserOutput(
    val id: Long,
    val email: String,
    val name: String,
) {
    companion object {
        fun of(user: com.biuea.wiki.domain.user.User): UserOutput {
            return UserOutput(
                id = user.id,
                email = user.email,
                name = user.name,
            )
        }
    }
}

data class LoginOutput(
    val accessToken: String,
    val tokenType: String,
    val user: UserOutput,
)
