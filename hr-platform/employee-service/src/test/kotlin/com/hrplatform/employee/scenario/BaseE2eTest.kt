package com.hrplatform.employee.scenario

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
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.Properties
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test-integration")
@Import(E2eKafkaConfig::class)
abstract class BaseE2eTest(
    private val environment: Environment,
) : BehaviorSpec() {

    protected val restTemplate: TestRestTemplate = TestRestTemplate()
    protected val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()

    protected fun serverPort(): Int =
        environment.getProperty("local.server.port")?.toInt()
            ?: error("서버 포트를 확인할 수 없습니다")

    protected fun baseUrl(path: String): String = "http://localhost:${serverPort()}$path"

    /**
     * 고유 groupId로 consumer를 생성한다. groupId가 새로 할당된 경우 offset이 없으므로
     * auto.offset.reset=earliest 설정에 의해 topic 처음부터 읽는다.
     * 각 테스트는 고유 aggregateId(employmentId)로 메시지를 필터링하므로 이전 메시지와 충돌하지 않는다.
     */
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

    /**
     * 파티션 할당 완료 후 토픽의 최신 위치(end)로 seek하여
     * 이후 HTTP 호출로 발행되는 메시지만 수신한다.
     * 이전 테스트 실행의 적재된 메시지를 건너뛰어 타임아웃 낭비를 방지한다.
     */
    protected fun awaitPartitionAssignment(consumer: KafkaConsumer<String, String>, topic: String) {
        consumer.subscribe(listOf(topic))
        val assignDeadline = System.currentTimeMillis() + 10_000L
        while (consumer.assignment().isEmpty() && System.currentTimeMillis() < assignDeadline) {
            consumer.poll(Duration.ofMillis(200))
        }
        // 파티션 끝으로 seek — 이후 발행될 메시지만 수신
        val partitions = consumer.assignment()
        if (partitions.isNotEmpty()) {
            consumer.seekToEnd(partitions)
            // seekToEnd는 lazy — 실제 위치 확정을 위해 position() 호출
            partitions.forEach { consumer.position(it) }
        }
    }

    protected fun pollMessages(
        consumer: KafkaConsumer<String, String>,
        expectedCount: Int,
        timeoutMs: Long = 20_000L,
    ): List<String> {
        val received = mutableListOf<String>()
        val deadline = System.currentTimeMillis() + timeoutMs
        while (received.size < expectedCount && System.currentTimeMillis() < deadline) {
            val records = consumer.poll(Duration.ofMillis(500))
            records.forEach { record -> received.add(record.value()) }
        }
        return received
    }

    /**
     * topic에서 특정 companyId를 가진 메시지를 expectedCount개 수집한다.
     * X4 일괄 등록처럼 aggregateId를 미리 알 수 없는 경우에 사용한다.
     */
    protected fun pollMessagesByCompanyId(
        consumer: KafkaConsumer<String, String>,
        companyId: Long,
        expectedCount: Int,
        timeoutMs: Long = 40_000L,
    ): List<Map<*, *>> {
        val received = mutableListOf<Map<*, *>>()
        val deadline = System.currentTimeMillis() + timeoutMs
        while (received.size < expectedCount && System.currentTimeMillis() < deadline) {
            val records = consumer.poll(Duration.ofMillis(500))
            records.forEach { record ->
                val parsed = objectMapper.readValue(record.value(), Map::class.java)
                val msgCompanyId = (parsed["companyId"] as? Number)?.toLong()
                if (msgCompanyId != null && msgCompanyId == companyId) {
                    received.add(parsed)
                }
            }
        }
        return received
    }

    /**
     * topic에서 특정 aggregateId를 가진 메시지를 expectedCount개 수집한다.
     * 다른 테스트의 메시지를 건너뛰어 격리성을 보장한다.
     */
    protected fun pollMessagesByAggregateId(
        consumer: KafkaConsumer<String, String>,
        aggregateIds: Set<Long>,
        expectedCount: Int,
        timeoutMs: Long = 40_000L,
    ): List<Map<*, *>> {
        val received = mutableListOf<Map<*, *>>()
        val deadline = System.currentTimeMillis() + timeoutMs
        while (received.size < expectedCount && System.currentTimeMillis() < deadline) {
            val records = consumer.poll(Duration.ofMillis(500))
            records.forEach { record ->
                val parsed = objectMapper.readValue(record.value(), Map::class.java)
                val msgAggregateId = (parsed["aggregateId"] as? Number)?.toLong()
                if (msgAggregateId != null && aggregateIds.contains(msgAggregateId)) {
                    received.add(parsed)
                }
            }
        }
        return received
    }

    companion object {
        val mysql: MySQLContainer<*> = MySQLContainer("mysql:8.0")
            .withDatabaseName("employee_db")
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

        init {
            mysql.start()
            kafka.start()
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
            registry.add("hrplatform.kafka.topics.employee") { "event.hr.employee.v1" }
            registry.add("hrplatform.encryption.aes-key") {
                java.util.Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })
            }
        }
    }
}
