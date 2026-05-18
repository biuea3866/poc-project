package com.hrplatform.auth.presentation.controller

import com.hrplatform.auth.domain.auth.service.LoginResult
import com.hrplatform.auth.domain.auth.service.TokenPair
import com.hrplatform.auth.domain.twofactor.service.TwoFactorEnrollmentResult
import java.time.ZonedDateTime

data class LoginResponse(
    val userAccountId: Long,
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: ZonedDateTime,
    val requiresTwoFactor: Boolean,
) {
    companion object {
        fun of(result: LoginResult): LoginResponse = LoginResponse(
            userAccountId = result.userAccountId,
            accessToken = result.accessToken,
            refreshToken = result.refreshToken,
            expiresAt = result.expiresAt,
            requiresTwoFactor = result.requiresTwoFactor,
        )
    }
}

data class TokenPairResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: ZonedDateTime,
) {
    companion object {
        fun of(pair: TokenPair): TokenPairResponse = TokenPairResponse(
            accessToken = pair.accessToken,
            refreshToken = pair.refreshToken,
            expiresAt = pair.refreshTokenExpiresAt,
        )
    }
}

data class TwoFactorEnrollResponse(
    val qrCodeDataUri: String,
    val backupCodes: List<String>,
) {
    companion object {
        fun of(result: TwoFactorEnrollmentResult): TwoFactorEnrollResponse = TwoFactorEnrollResponse(
            qrCodeDataUri = result.qrCodeDataUri,
            backupCodes = result.backupCodes,
        )
    }
}

data class TwoFactorVerifyResponse(
    val verified: Boolean,
)
