package com.hrplatform.employee.infrastructure.encryption

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * JPA AttributeConverter — String 컬럼을 AES-256-GCM으로 암복호화.
 *
 * DB 저장 형식: Base64(IV[12바이트] + CipherText + AuthTag[16바이트])
 * 키 소스: `hrplatform.encryption.aes-key` (Base64 인코딩된 32바이트)
 * 운영 환경에서는 반드시 환경변수(HRPLATFORM_ENCRYPTION_AES_KEY)로 주입해야 한다.
 * dev profile: application.yml의 placeholder 32바이트 키 사용.
 */
@Component
@Converter
class AesGcmStringConverter(
    @Value("\${hrplatform.encryption.aes-key}") aesKeyBase64: String,
) : AttributeConverter<String?, String?> {

    private val secretKey: SecretKey

    init {
        val keyBytes = Base64.getDecoder().decode(aesKeyBase64)
        require(keyBytes.size == AES_KEY_BYTES) {
            "AES 키는 32바이트(256비트)여야 합니다. 현재: ${keyBytes.size}바이트"
        }
        secretKey = SecretKeySpec(keyBytes, ALGORITHM)
    }

    override fun convertToDatabaseColumn(attribute: String?): String? {
        attribute ?: return null

        val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        val cipherText = cipher.doFinal(attribute.toByteArray(Charsets.UTF_8))
        val combined = iv + cipherText
        return Base64.getEncoder().encodeToString(combined)
    }

    override fun convertToEntityAttribute(dbData: String?): String? {
        dbData ?: return null

        val combined = Base64.getDecoder().decode(dbData)
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val cipherText = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }

    companion object {
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_BITS = 128
        private const val AES_KEY_BYTES = 32

        /**
         * Spring 컨텍스트 없이 테스트에서 직접 인스턴스를 생성할 때 사용.
         */
        fun create(aesKeyBase64: String): AesGcmStringConverter =
            AesGcmStringConverter(aesKeyBase64)
    }
}
