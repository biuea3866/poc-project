package com.biuea.chat.support

import org.testcontainers.containers.GenericContainer
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
