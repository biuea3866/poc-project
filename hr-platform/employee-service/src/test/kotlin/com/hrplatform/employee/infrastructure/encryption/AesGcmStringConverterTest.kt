package com.hrplatform.employee.infrastructure.encryption

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class AesGcmStringConverterTest : BehaviorSpec({

    val validBase64Key = "dGVzdGtleXRlc3RrZXl0ZXN0a2V5dGVzdGtleXRlczE=" // 32바이트 base64

    given("유효한 32바이트 AES 키로 Converter를 초기화하면") {
        val converter = AesGcmStringConverter.create(validBase64Key)

        `when`("평문 문자열을 암호화하면") {
            val plainText = "test@example.com"
            val encrypted = converter.convertToDatabaseColumn(plainText)

            then("암호문은 평문과 다르다") {
                encrypted shouldNotBe plainText
            }

            then("암호문은 null이 아니다") {
                encrypted shouldNotBe null
            }
        }

        `when`("암호화 후 복호화하면 원본 평문이 복원된다 (round-trip)") {
            val plainText = "test@example.com"
            val encrypted = converter.convertToDatabaseColumn(plainText)
            val decrypted = converter.convertToEntityAttribute(encrypted)

            then("복호화 결과가 원본 평문과 동일하다") {
                decrypted shouldBe plainText
            }
        }

        `when`("한글 이메일 주소를 암호화/복호화하면") {
            val plainText = "가나다라@example.co.kr"
            val encrypted = converter.convertToDatabaseColumn(plainText)
            val decrypted = converter.convertToEntityAttribute(encrypted)

            then("round-trip이 성공한다") {
                decrypted shouldBe plainText
            }
        }

        `when`("같은 평문을 두 번 암호화하면") {
            val plainText = "test@example.com"
            val encrypted1 = converter.convertToDatabaseColumn(plainText)
            val encrypted2 = converter.convertToDatabaseColumn(plainText)

            then("GCM IV 무작위성으로 암호문이 서로 다르다") {
                encrypted1 shouldNotBe encrypted2
            }

            then("두 암호문을 각각 복호화하면 같은 평문이 나온다") {
                converter.convertToEntityAttribute(encrypted1) shouldBe plainText
                converter.convertToEntityAttribute(encrypted2) shouldBe plainText
            }
        }

        `when`("convertToDatabaseColumn에 null을 전달하면") {
            val result = converter.convertToDatabaseColumn(null)
            then("null을 반환한다") {
                result shouldBe null
            }
        }

        `when`("convertToEntityAttribute에 null을 전달하면") {
            val result = converter.convertToEntityAttribute(null)
            then("null을 반환한다") {
                result shouldBe null
            }
        }
    }

    given("키 길이가 32바이트가 아닌 경우") {
        `when`("24바이트 키(AES-192)로 Converter를 생성하면") {
            val shortKey = "dGVzdGtleXRlc3RrZXl0ZXM=" // 18바이트 decoded — 잘못된 키

            then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    AesGcmStringConverter.create(shortKey)
                }
            }
        }
    }
})
