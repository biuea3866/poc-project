package com.biuea.wiki.integration

import com.biuea.wiki.WikiApiApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = ["event.document", "event.ai.failed", "queue.ai.tagging", "queue.ai.embedding"])
@SpringBootTest(
    classes = [WikiApiApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
abstract class BaseIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        private val mysql: MySQLContainer<*> = MySQLContainer("mysql:8.0.36")
            .withDatabaseName("wiki")
            .withUsername("wiki_user")
            .withPassword("wiki_password")

        @Container
        @JvmStatic
        private val redis: GenericContainer<*> = GenericContainer(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379)

        @Container
        @JvmStatic
        private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres")
        )
            .withDatabaseName("wiki")
            .withUsername("wiki_vector_user")
            .withPassword("wiki_vector_password")
            .withInitScript("init-vector.sql")

        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            // MySQL
            registry.add("spring.datasource.url", mysql::getJdbcUrl)
            registry.add("spring.datasource.username", mysql::getUsername)
            registry.add("spring.datasource.password", mysql::getPassword)
            registry.add("spring.jpa.hibernate.ddl-auto") { "create" }

            // Redis
            registry.add("spring.data.redis.host", redis::getHost)
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }

            // PostgreSQL (vector)
            registry.add("datasource-vector.url", postgres::getJdbcUrl)
            registry.add("datasource-vector.username", postgres::getUsername)
            registry.add("datasource-vector.password", postgres::getPassword)

            // JWT
            registry.add("security.jwt.secret") { "this-is-an-integration-test-jwt-secret-key-at-least-32-bytes" }
            registry.add("security.jwt.access-token-expiration-ms") { "1800000" }
            registry.add("security.jwt.refresh-token-expiration-ms") { "3600000" }
        }
    }
}
