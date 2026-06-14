package com.biuea.batch.support

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer

/**
 * Testcontainers MySQL 기반 통합테스트 베이스.
 *
 * 싱글톤 컨테이너 패턴 — companion init 에서 1회 start 하고 중지하지 않는다(JVM 종료 시 Ryuk 가 정리).
 * @Testcontainers/@Container 를 쓰면 클래스마다 컨테이너를 stop 하는데, Spring 컨텍스트는 캐시되어
 * 다음 클래스가 죽은 컨테이너를 가리키는 문제가 생기므로 쓰지 않는다.
 *
 * 비즈니스 테이블은 withInitScript 로, BATCH_* 메타테이블은 앱의 initialize-schema=always 로 생성된다.
 */
@SpringBootTest
abstract class IntegrationTestBase {
    companion object {
        @JvmStatic
        val mysql: MySQLContainer<*> =
            MySQLContainer("mysql:8.0")
                .withDatabaseName("batch_perf")
                .withUsername("batch_user")
                .withPassword("batch_password")
                .withInitScript("schema/01-schema.sql")
                .also { it.start() }

        @DynamicPropertySource
        @JvmStatic
        fun datasourceProps(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") {
                "${mysql.jdbcUrl}?rewriteBatchedStatements=true"
            }
            registry.add("spring.datasource.username", mysql::getUsername)
            registry.add("spring.datasource.password", mysql::getPassword)
            registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName)
        }
    }
}
