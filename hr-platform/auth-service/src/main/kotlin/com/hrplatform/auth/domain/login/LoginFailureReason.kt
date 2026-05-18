package com.hrplatform.auth.domain.login

enum class LoginFailureReason {
    BAD_PASSWORD,
    ACCOUNT_LOCKED,
    ACCOUNT_SUSPENDED,
    ACCOUNT_DEACTIVATED,
    INVALID_2FA,
    EMAIL_NOT_FOUND,
}
