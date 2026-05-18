package com.hrplatform.auth.domain.twofactor.service

import dev.samstevens.totp.code.CodeGenerator
import dev.samstevens.totp.code.CodeVerifier
import dev.samstevens.totp.code.DefaultCodeGenerator
import dev.samstevens.totp.code.DefaultCodeVerifier
import dev.samstevens.totp.code.HashingAlgorithm
import dev.samstevens.totp.qr.QrData
import dev.samstevens.totp.qr.QrGenerator
import dev.samstevens.totp.qr.ZxingPngQrGenerator
import dev.samstevens.totp.secret.DefaultSecretGenerator
import dev.samstevens.totp.time.SystemTimeProvider
import dev.samstevens.totp.util.Utils
import org.springframework.stereotype.Component

data class TotpSetup(
    val secret: String,
    val qrCodeDataUri: String,
)

@Component
class TotpService {

    private val secretGenerator = DefaultSecretGenerator()
    private val codeGenerator: CodeGenerator = DefaultCodeGenerator(HashingAlgorithm.SHA1)
    private val timeProvider = SystemTimeProvider()
    private val codeVerifier: CodeVerifier = DefaultCodeVerifier(codeGenerator, timeProvider)
    private val qrGenerator: QrGenerator = ZxingPngQrGenerator()

    fun generateSecret(): String = secretGenerator.generate()

    fun buildQrCode(email: String, secret: String, issuer: String = "HR Platform"): TotpSetup {
        val qrData = QrData.Builder()
            .label(email)
            .secret(secret)
            .issuer(issuer)
            .algorithm(HashingAlgorithm.SHA1)
            .digits(6)
            .period(30)
            .build()
        val imageData = qrGenerator.generate(qrData)
        val dataUri = Utils.getDataUriForImage(imageData, qrGenerator.imageMimeType)
        return TotpSetup(secret = secret, qrCodeDataUri = dataUri)
    }

    fun verify(secret: String, otp: String): Boolean =
        codeVerifier.isValidCode(secret, otp)
}
