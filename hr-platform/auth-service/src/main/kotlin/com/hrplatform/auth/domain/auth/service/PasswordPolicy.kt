package com.hrplatform.auth.domain.auth.service

import com.hrplatform.core.exception.BusinessException

/**
 * 비밀번호 정책 value object.
 * ADR-003 §9: 최소 10자, 영문+숫자+특수문자 포함.
 */
object PasswordPolicy {

    private const val MIN_LENGTH = 10
    private val LETTER_REGEX = Regex("[a-zA-Z]")
    private val DIGIT_REGEX = Regex("[0-9]")
    private val SPECIAL_REGEX = Regex("[^a-zA-Z0-9]")

    fun validate(rawPassword: String) {
        if (rawPassword.length < MIN_LENGTH) {
            throw BusinessException(errorCode = "WEAK_PASSWORD", message = "비밀번호는 최소 ${MIN_LENGTH}자 이상이어야 합니다")
        }
        if (!LETTER_REGEX.containsMatchIn(rawPassword)) {
            throw BusinessException(errorCode = "WEAK_PASSWORD", message = "비밀번호에 영문자가 포함되어야 합니다")
        }
        if (!DIGIT_REGEX.containsMatchIn(rawPassword)) {
            throw BusinessException(errorCode = "WEAK_PASSWORD", message = "비밀번호에 숫자가 포함되어야 합니다")
        }
        if (!SPECIAL_REGEX.containsMatchIn(rawPassword)) {
            throw BusinessException(errorCode = "WEAK_PASSWORD", message = "비밀번호에 특수문자가 포함되어야 합니다")
        }
    }
}
