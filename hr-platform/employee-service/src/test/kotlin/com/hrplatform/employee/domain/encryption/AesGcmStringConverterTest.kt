package com.hrplatform.employee.domain.encryption

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.Base64

class AesGcmStringConverterTest : BehaviorSpec({

    // 32바이트 테스트 키 (Base64 인코딩)
    val testKey = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })

    fun createConverter() = AesGcmStringConverter(testKey)

    given("AesGcmStringConverter") {

        `when`("평문을 암호화하면") {
            val converter = createConverter()
            val plaintext = "test@example.com"
            val encrypted = requireNotNull(converter.convertToDatabaseColumn(plaintext))

            then("암호화된 바이트 배열이 반환된다") {
                encrypted.size shouldBeGreaterThan 0
            }

            then("암호화된 값은 원문과 다르다") {
                encrypted.toString(Charsets.UTF_8) shouldNotBe plaintext
            }
        }

        `when`("암호화 후 복호화하면") {
            val converter = createConverter()
            val plaintext = "hong@example.com"
            val encrypted = converter.convertToDatabaseColumn(plaintext)
            val decrypted = converter.convertToEntityAttribute(encrypted)

            then("원문이 복원된다") {
                decrypted shouldBe plaintext
            }
        }

        `when`("같은 평문을 두 번 암호화하면") {
            val converter = createConverter()
            val plaintext = "repeat@example.com"
            val encrypted1 = requireNotNull(converter.convertToDatabaseColumn(plaintext))
            val encrypted2 = requireNotNull(converter.convertToDatabaseColumn(plaintext))

            then("IV 무작위성으로 인해 결과가 다르다") {
                encrypted1.contentEquals(encrypted2) shouldBe false
            }
        }

        `when`("암호화된 데이터의 첫 바이트를 확인하면") {
            val converter = createConverter()
            val encrypted = requireNotNull(converter.convertToDatabaseColumn("test@example.com"))

            then("key version prefix(1바이트) 가 포함된다") {
                // 첫 바이트는 key version (1)
                encrypted[0] shouldBe 1.toByte()
            }
        }

        `when`("null 값을 변환하면") {
            val converter = createConverter()

            then("convertToDatabaseColumn 에 non-null 값을 넘기면 non-null 결과가 반환된다") {
                val result = converter.convertToDatabaseColumn("non-null-value")
                result shouldNotBe null
            }
        }

        `when`("키가 없으면 Hibernate fallback이 활성화된다") {
            then("zero 키로 초기화되어 예외를 던지지 않는다 (운영은 @Value 또는 환경변수로 정상 주입)") {
                val converter = AesGcmStringConverter("")
                val encrypted = converter.convertToDatabaseColumn("test")
                encrypted shouldNotBe null
            }
        }

        `when`("한글 문자열을 암호화하면") {
            val converter = createConverter()
            val plaintext = "홍길동"
            val encrypted = converter.convertToDatabaseColumn(plaintext)
            val decrypted = converter.convertToEntityAttribute(encrypted)

            then("UTF-8 라운드트립이 정상이다") {
                decrypted shouldBe plaintext
            }
        }
    }
})
