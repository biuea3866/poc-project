package com.closet.common.test

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * 통합 테스트 베이스 클래스.
 *
 * Testcontainers 싱글턴 패턴으로 MySQL + Redis 컨테이너를 관리한다.
 * 모든 통합 테스트는 이 클래스를 상속하여 컨테이너를 공유한다.
 *
 * 사용법:
 * ```
 * @SpringBootTest
 * class MyRepositoryTest : BaseIntegrationTest() {
 *     // MySQL, Redis 자동 연결
 * }
 * ```
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
abstract class BaseIntegrationTest {
    companion object {
        private const val MYSQL_IMAGE = "mysql:8.0"
        private const val REDIS_IMAGE = "redis:7.0-alpine"
        private const val REDIS_PORT = 6379

        @JvmStatic
        private val mysqlContainer: MySQLContainer<*> =
            MySQLContainer(MYSQL_IMAGE)
                .withDatabaseName("closet_test")
                .withUsername("test")
                .withPassword("test")
                .withCommand(
                    "--character-set-server=utf8mb4",
                    "--collation-server=utf8mb4_unicode_ci",
                    "--default-time-zone=+09:00",
                )
                .apply { start() }

        @JvmStatic
        private val redisContainer: GenericContainer<*> =
            GenericContainer(REDIS_IMAGE)
                .withExposedPorts(REDIS_PORT)
                .apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun overrideProperties(registry: DynamicPropertyRegistry) {
            // MySQL
            registry.add("spring.datasource.url") { mysqlContainer.jdbcUrl }
            registry.add("spring.datasource.username") { mysqlContainer.username }
            registry.add("spring.datasource.password") { mysqlContainer.password }
            registry.add("spring.datasource.driver-class-name") { "com.mysql.cj.jdbc.Driver" }

            // JPA
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
            registry.add("spring.jpa.properties.hibernate.dialect") { "org.hibernate.dialect.MySQLDialect" }

            // Flyway (통합 테스트에서는 비활성화, ddl-auto 사용)
            registry.add("spring.flyway.enabled") { "false" }

            // Redis
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(REDIS_PORT) }
        }
    }
}
