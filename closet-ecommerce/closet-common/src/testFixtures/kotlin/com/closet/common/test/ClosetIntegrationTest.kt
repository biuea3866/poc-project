package com.closet.common.test

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer

/**
 * Closet 통합 테스트 인프라 (testFixtures).
 *
 * Testcontainers 싱글턴 패턴으로 MySQL 8.0 + Redis 7.0 컨테이너를 관리한다.
 * 모든 모듈의 통합 테스트에서 이 companion object의 @DynamicPropertySource를 상속받아 사용한다.
 *
 * 사용법 (Kotest BehaviorSpec):
 * ```
 * @SpringBootTest
 * @ActiveProfiles("test")
 * class MyServiceIntegrationTest : BehaviorSpec() {
 *     companion object {
 *         @JvmStatic
 *         @DynamicPropertySource
 *         fun properties(registry: DynamicPropertyRegistry) {
 *             ClosetIntegrationTest.overrideProperties(registry)
 *         }
 *     }
 *
 *     // ...
 * }
 * ```
 */
object ClosetIntegrationTest {
    private const val MYSQL_IMAGE = "mysql:8.0"
    private const val REDIS_IMAGE = "redis:7.0-alpine"
    private const val REDIS_PORT = 6379

    val mysqlContainer: MySQLContainer<*> =
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

    val redisContainer: GenericContainer<*> =
        GenericContainer(REDIS_IMAGE)
            .withExposedPorts(REDIS_PORT)
            .apply { start() }

    /**
     * DynamicPropertySource에서 호출하여 datasource, JPA, Flyway, Redis 프로퍼티를 오버라이드한다.
     */
    fun overrideProperties(registry: DynamicPropertyRegistry) {
        // MySQL
        registry.add("spring.datasource.url") { mysqlContainer.jdbcUrl }
        registry.add("spring.datasource.username") { mysqlContainer.username }
        registry.add("spring.datasource.password") { mysqlContainer.password }
        registry.add("spring.datasource.driver-class-name") { "com.mysql.cj.jdbc.Driver" }

        // JPA (Flyway 대신 ddl-auto로 스키마 생성)
        registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
        registry.add("spring.jpa.properties.hibernate.dialect") { "org.hibernate.dialect.MySQLDialect" }

        // Flyway (통합 테스트에서는 비활성화)
        registry.add("spring.flyway.enabled") { "false" }

        // Redis
        registry.add("spring.data.redis.host") { redisContainer.host }
        registry.add("spring.data.redis.port") { redisContainer.getMappedPort(REDIS_PORT) }
    }
}
