package com.hrplatform.auth.infrastructure.crypto

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM 컬럼 암호화 컨버터.
 * employee-service의 동일 클래스 복제 (ADR-003 §7 — auth 전용 패키지).
 *
 * 저장 포맷: [version(1B)] [IV(12B)] [ciphertext + tag(len B)]
 */
@Component
@Converter
class AesGcmStringConverter
    @JvmOverloads
    constructor(
        @Value("\${hrplatform.encryption.aes-key:}")
        private val base64Key: String = System.getenv("HRPLATFORM_AUTH_AES_KEY") ?: "",
    ) : AttributeConverter<String?, ByteArray?> {

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val KEY_VERSION: Byte = 1
    }

    private val secretKey: SecretKeySpec

    init {
        val effectiveKey = if (base64Key.isBlank()) {
            Base64.getEncoder().encodeToString(ByteArray(32))
        } else {
            base64Key
        }
        val keyBytes = Base64.getDecoder().decode(effectiveKey)
        require(keyBytes.size == 32) { "AES-256 키는 32바이트여야 합니다. 현재: ${keyBytes.size}바이트" }
        secretKey = SecretKeySpec(keyBytes, "AES")
    }

    override fun convertToDatabaseColumn(attribute: String?): ByteArray? {
        attribute ?: return null
        val iv = ByteArray(IV_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val ciphertextWithTag = cipher.doFinal(attribute.toByteArray(Charsets.UTF_8))
        return byteArrayOf(KEY_VERSION) + iv + ciphertextWithTag
    }

    override fun convertToEntityAttribute(dbData: ByteArray?): String? {
        dbData ?: return null
        val iv = dbData.copyOfRange(1, 1 + IV_LENGTH_BYTES)
        val ciphertextWithTag = dbData.copyOfRange(1 + IV_LENGTH_BYTES, dbData.size)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return String(cipher.doFinal(ciphertextWithTag), Charsets.UTF_8)
    }
}
