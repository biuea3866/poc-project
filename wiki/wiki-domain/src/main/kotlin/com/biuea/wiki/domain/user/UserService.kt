package com.biuea.wiki.domain.user

import com.biuea.wiki.domain.user.exception.InvalidCredentialsException
import com.biuea.wiki.domain.user.exception.UserAlreadyExistsException
import com.biuea.wiki.domain.user.exception.UserNotFoundException
import com.biuea.wiki.infrastructure.user.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    @Transactional
    fun signUp(command: SignUpUserCommand): User {
        val normalizedEmail = command.email.trim().lowercase()
        if (userRepository.findByEmail(normalizedEmail) != null) {
            throw UserAlreadyExistsException(normalizedEmail)
        }
        val encodedPassword = requireNotNull(passwordEncoder.encode(command.password))

        val user = User(
            email = normalizedEmail,
            password = encodedPassword,
            name = command.name.trim(),
        )

        return userRepository.save(user)
    }

    @Transactional(readOnly = true)
    fun login(command: LoginUserCommand): User {
        val normalizedEmail = command.email.trim().lowercase()
        val user = userRepository.findByEmailAndDeletedAtIsNull(normalizedEmail)
            ?: throw InvalidCredentialsException()

        if (!passwordEncoder.matches(command.password, user.password)) {
            throw InvalidCredentialsException()
        }

        return user
    }

    @Transactional(readOnly = true)
    fun findById(userId: Long): User {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
            ?: throw UserNotFoundException(userId)
    }

    @Transactional
    fun delete(command: DeleteUserCommand) {
        val user = userRepository.findByIdAndDeletedAtIsNull(command.userId)
            ?: throw UserNotFoundException(command.userId)

        user.softDelete()
    }
}
