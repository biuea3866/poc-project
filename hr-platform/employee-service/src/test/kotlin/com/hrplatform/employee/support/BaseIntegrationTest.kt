package com.hrplatform.employee.support

import io.kotest.core.spec.style.BehaviorSpec
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container

@SpringBootTest
@ActiveProfiles("test-integration")
@Import(TestDomainEventPublisherConfig::class)
abstract class BaseIntegrationTest : BehaviorSpec() {

    companion object {
        @Container
        val mysql: MySQLContainer<*> = MySQLContainer("mysql:8.0")
            .withDatabaseName("employee_db")
            .withUsername("test")
            .withPassword("test")
            .withCommand(
                "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_unicode_ci",
            )
            .withReuse(true)

        init {
            mysql.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") {
                "${mysql.jdbcUrl}?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8"
            }
            registry.add("spring.datasource.username") { mysql.username }
            registry.add("spring.datasource.password") { mysql.password }
            registry.add("spring.jpa.properties.hibernate.jdbc.time_zone") { "UTC" }
            registry.add("spring.jpa.properties.hibernate.timezone.default_storage") { "NORMALIZE" }
            registry.add("hrplatform.encryption.aes-key") {
                // 테스트용 32바이트 Base64 키
                java.util.Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })
            }
        }
    }
}
