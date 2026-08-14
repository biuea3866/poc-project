package com.biuea.delivery.support

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * 통합 테스트용 Redis 컨테이너. JVM 당 한 번 기동해 재사용한다 (Ryuk 가 종료 시 정리).
 */
object RedisContainer {
    private val container: GenericContainer<*> =
        GenericContainer(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .also { it.start() }

    val host: String get() = container.host
    val port: Int get() = container.getMappedPort(6379)
}

/**
 * 통합 테스트용 MySQL 8.0 컨테이너. 비관적 락·낙관적 락 검증에 실제 InnoDB 가 필요하다.
 * 커넥션 부족 상황을 재현하려면 max_connections 를 낮게 잡을 수 있어야 하므로 커맨드로 노출한다.
 */
object MySqlContainer {
    private val container: MySQLContainer<*> =
        MySQLContainer(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("delivery")
            .withUsername("delivery")
            .withPassword("delivery")
            .withCommand("--max_connections=200", "--innodb_lock_wait_timeout=5")
            .also { it.start() }

    val jdbcUrl: String get() = container.jdbcUrl
    val username: String get() = container.username
    val password: String get() = container.password
}
