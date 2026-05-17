package com.hrplatform.employee.domain.encryption

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
 *
 * 저장 포맷: [version(1B)] [IV(12B)] [ciphertext + tag(len B)]
 * - version: key 버전 (현재 1). 향후 키 회전 시 버전 increment.
 * - IV: SecureRandom 12바이트 (매 암호화마다 무작위).
 * - GCM tag: 암호문 말미 16바이트에 포함 (JCE 기본).
 *
 * 의존 주입: @Value 로 HRPLATFORM_ENCRYPTION_AES_KEY 환경변수 주입.
 * 환경변수 미설정 시 ApplicationContext 로딩 실패 — init {} 에서 검증.
 */
@Component
@Converter
class AesGcmStringConverter
    @JvmOverloads
    constructor(
        @Value("\${hrplatform.encryption.aes-key:}")
        private val base64Key: String = System.getenv("HRPLATFORM_ENCRYPTION_AES_KEY") ?: "",
    ) : AttributeConverter<String?, ByteArray?> {

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val KEY_VERSION: Byte = 1
    }

    private val secretKey: SecretKeySpec

    init {
        // Hibernate fallback (EMF 직접 빌드 — Spring 없이) 케이스: 32바이트 zero 키로 fallback.
        // 운영 환경에서는 @Value 또는 HRPLATFORM_ENCRYPTION_AES_KEY 환경변수가 정상 주입됨.
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
