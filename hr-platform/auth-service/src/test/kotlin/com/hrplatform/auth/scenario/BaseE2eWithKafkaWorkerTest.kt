package com.hrplatform.auth.scenario

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.core.spec.style.BehaviorSpec
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.core.env.Environment
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.Base64
import java.util.Properties
import java.util.UUID

/**
 * Kafka Consumer Worker가 활성화된 E2E 테스트 Base.
 * EmployeeEventWorker가 실제로 Kafka 메시지를 수신하는 시나리오에 사용.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test-integration")
@Import(E2eKafkaConfig::class, E2eEmployeeEventWorkerConfig::class)
abstract class BaseE2eWithKafkaWorkerTest(
    private val environment: Environment,
) : BehaviorSpec() {

    protected val restTemplate: TestRestTemplate = TestRestTemplate()
    protected val objectMapper: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .also { it.findAndRegisterModules() }

    protected fun serverPort(): Int =
        environment.getProperty("local.server.port")?.toInt()
            ?: error("서버 포트를 확인할 수 없습니다")

    protected fun baseUrl(path: String): String = "http://localhost:${serverPort()}$path"

    protected fun buildKafkaConsumer(groupIdSuffix: String = UUID.randomUUID().toString()): KafkaConsumer<String, String> {
        val properties = Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers)
            put(ConsumerConfig.GROUP_ID_CONFIG, "e2e-test-$groupIdSuffix")
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "500")
        }
        return KafkaConsumer(properties)
    }

    protected fun awaitPartitionAssignment(consumer: KafkaConsumer<String, String>, topic: String) {
        consumer.subscribe(listOf(topic))
        val assignDeadline = System.currentTimeMillis() + 10_000L
        while (consumer.assignment().isEmpty() && System.currentTimeMillis() < assignDeadline) {
            consumer.poll(Duration.ofMillis(200))
        }
        val partitions = consumer.assignment()
        if (partitions.isNotEmpty()) {
            consumer.seekToEnd(partitions)
            partitions.forEach { consumer.position(it) }
        }
    }

    protected fun pollMessagesByEventType(
        consumer: KafkaConsumer<String, String>,
        eventTypes: Set<String>,
        expectedCount: Int,
        timeoutMs: Long = 30_000L,
    ): List<Map<*, *>> {
        val received = mutableListOf<Map<*, *>>()
        val deadline = System.currentTimeMillis() + timeoutMs
        while (received.size < expectedCount && System.currentTimeMillis() < deadline) {
            val records = consumer.poll(Duration.ofMillis(500))
            records.forEach { record ->
                val parsed = objectMapper.readValue(record.value(), Map::class.java)
                val eventType = parsed["eventType"] as? String
                if (eventType != null && eventTypes.contains(eventType)) {
                    received.add(parsed)
                }
            }
        }
        return received
    }

    companion object {
        val mysql: MySQLContainer<*> = MySQLContainer("mysql:8.0")
            .withDatabaseName("auth_db")
            .withUsername("test")
            .withPassword("test")
            .withCommand(
                "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_unicode_ci",
            )
            .withReuse(true)

        val kafka: KafkaContainer = KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.0"),
        ).withReuse(true)

        @Suppress("MagicNumber")
        val redis: GenericContainer<*> = GenericContainer("redis:7-alpine")
            .withExposedPorts(6379)
            .withReuse(true)

        init {
            mysql.start()
            kafka.start()
            redis.start()
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
            registry.add("spring.kafka.bootstrap-servers") { kafka.bootstrapServers }
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }
            registry.add("hrplatform.encryption.aes-key") {
                Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })
            }
            registry.add("hrplatform.auth.email-hash-secret") { "e2e-test-email-hash-secret" }
            registry.add("hrplatform.jwt.secret") {
                Base64.getEncoder().encodeToString(ByteArray(64) { 1 })
            }
            registry.add("hrplatform.jwt.issuer") { "hr-platform-test" }
            registry.add("hrplatform.jwt.audience") { "hr-platform-users-test" }
            registry.add("hrplatform.kafka.topics.employee") { "event.hr.employee.v1" }
            registry.add("hrplatform.kafka.topics.auth") { "event.hr.auth.v1" }
        }
    }
}
