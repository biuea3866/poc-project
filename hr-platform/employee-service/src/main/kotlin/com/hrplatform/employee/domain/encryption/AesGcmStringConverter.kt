package com.hrplatform.employee.domain.encryption

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.slf4j.LoggerFactory
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
 *
 * Hibernate가 Spring 없이 EMF를 직접 빌드하는 테스트 환경에서는
 * no-arg constructor 경로로 키를 조회하며,
 * test profile(SPRING_PROFILES_ACTIVE 또는 spring.profiles.active)에서만
 * zero-key fallback을 허용한다 (보안 경고 로그 출력).
 * 운영 환경(non-test profile)에서 키가 비어 있으면 즉시 fail-loud.
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

        private val log = LoggerFactory.getLogger(AesGcmStringConverter::class.java)

        private fun isTestProfile(): Boolean {
            val envProfile = System.getenv("SPRING_PROFILES_ACTIVE") ?: ""
            val propProfile = System.getProperty("spring.profiles.active", "")
            return envProfile.contains("test") || propProfile.contains("test")
        }
    }

    private val secretKey: SecretKeySpec

    init {
        if (base64Key.isBlank()) {
            require(isTestProfile()) {
                "HRPLATFORM_ENCRYPTION_AES_KEY 환경변수 필수 (운영 시작 시 fail-loud). " +
                    "spring.profiles.active 또는 SPRING_PROFILES_ACTIVE 에 'test' 포함 시에만 zero-key fallback 허용."
            }
            log.warn(
                "[SECURITY] AesGcmStringConverter: 암호화 키 미설정 — test profile zero-key fallback 활성화. " +
                    "운영 배포 전 HRPLATFORM_ENCRYPTION_AES_KEY 반드시 설정 필요.",
            )
            secretKey = SecretKeySpec(ByteArray(32), "AES")
        } else {
            val keyBytes = Base64.getDecoder().decode(base64Key)
            require(keyBytes.size == 32) { "AES-256 키는 32바이트여야 합니다. 현재: ${keyBytes.size}바이트" }
            secretKey = SecretKeySpec(keyBytes, "AES")
        }
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
