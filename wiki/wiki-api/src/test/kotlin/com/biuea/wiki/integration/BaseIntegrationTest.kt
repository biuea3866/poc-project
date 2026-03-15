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
import java.sql.DriverManager

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
            DockerImageName.parse("ankane/pgvector").asCompatibleSubstituteFor("postgres")
        )
            .withDatabaseName("wiki")
            .withUsername("wiki_vector_user")
            .withPassword("wiki_vector_password")

        private fun initPostgresSchema() {
            Class.forName("org.postgresql.Driver")
            DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.execute("CREATE EXTENSION IF NOT EXISTS vector")
                    stmt.execute("""
                        CREATE TABLE IF NOT EXISTS document_embeddings (
                            id BIGSERIAL PRIMARY KEY,
                            document_id BIGINT NOT NULL,
                            document_revision_id BIGINT NOT NULL,
                            embedding vector(1536),
                            chunk_content TEXT NOT NULL,
                            token_count INT,
                            metadata JSONB
                        )
                    """)
                }
            }
        }

        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            // Initialize pgvector schema
            initPostgresSchema()

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
