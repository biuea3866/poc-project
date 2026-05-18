package com.hrplatform.auth.application.twofactor

data class TwoFactorEnrollmentResult(
    val qrCodeDataUri: String,
    val secret: String,
    val backupCodes: List<String>,
)
