package com.hrplatform.auth.infrastructure.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.hrplatform.auth.infrastructure.kafka.AuthKafkaDomainEventPublisherUnitTest.Companion.AUTH_EVENT_TYPES
import com.hrplatform.auth.infrastructure.kafka.AuthKafkaDomainEventPublisherUnitTest.Companion.buildAuthEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.Properties
import java.util.UUID

/**
 * Testcontainers Kafka를 이용한 AuthKafkaDomainEventPublisher 통합 테스트.
 * Spring Boot 컨텍스트 없이 순수 Kafka 클라이언트로 구성.
 */
class AuthKafkaDomainEventPublisherIntegrationTest : BehaviorSpec({

    val bootstrapServers = kafka.bootstrapServers
    val topic = "event.hr.auth.v1"

    val producerFactory = DefaultKafkaProducerFactory<String, String>(
        mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "all",
        ),
    )
    val kafkaTemplate = KafkaTemplate(producerFactory)
    val objectMapper = ObjectMapper().registerKotlinModule().also { it.findAndRegisterModules() }
    val publisher = AuthKafkaDomainEventPublisher(
        kafkaTemplate = kafkaTemplate,
        objectMapper = objectMapper,
        authTopic = topic,
    )

    fun buildConsumer(): KafkaConsumer<String, String> {
        val properties = Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-${UUID.randomUUID()}")
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
            put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100")
        }
        return KafkaConsumer(properties)
    }

    fun awaitAssignment(consumer: KafkaConsumer<String, String>) {
        consumer.subscribe(listOf(topic))
        val deadline = System.currentTimeMillis() + 10_000L
        while (consumer.assignment().isEmpty() && System.currentTimeMillis() < deadline) {
            consumer.poll(Duration.ofMillis(200))
        }
    }

    fun pollMessages(
        consumer: KafkaConsumer<String, String>,
        expectedCount: Int,
        timeoutMs: Long = 15_000L,
    ): List<Map<*, *>> {
        val received = mutableListOf<Map<*, *>>()
        val deadline = System.currentTimeMillis() + timeoutMs
        while (received.size < expectedCount && System.currentTimeMillis() < deadline) {
            val records = consumer.poll(Duration.ofMillis(500))
            records.forEach { record ->
                received.add(objectMapper.readValue(record.value(), Map::class.java))
            }
        }
        return received
    }

    given("11종 auth DomainEvent 각각 publishAll로 발행") {
        then("컨슈머가 11건을 수신하고 eventType 11종이 모두 포함된다") {
            val consumer = buildConsumer()
            awaitAssignment(consumer)

            val events = AUTH_EVENT_TYPES.mapIndexed { index, eventType ->
                buildAuthEvent(aggregateId = (index + 1).toLong(), eventType = eventType)
            }
            publisher.publishAll(events)

            val received = pollMessages(consumer, 11)
            consumer.close()

            received.size shouldBe 11
            received.map { it["eventType"] as String }.toSet() shouldBe AUTH_EVENT_TYPES.toSet()
        }
    }

    given("aggregateId가 같은 이벤트 3건 발행") {
        then("수신된 3건은 모두 같은 파티션이다") {
            val sameAggregateId = 77777L
            val partitions = mutableSetOf<Int>()
            var count = 0

            // rawConsumer 먼저 assignment 후 발행 → latest 수신 보장
            val rawConsumer = KafkaConsumer<String, String>(
                Properties().apply {
                    put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
                    put(ConsumerConfig.GROUP_ID_CONFIG, "test-partition-${UUID.randomUUID()}")
                    put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                    put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                    put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
                    put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100")
                },
            )
            rawConsumer.subscribe(listOf(topic))
            while (rawConsumer.assignment().isEmpty()) rawConsumer.poll(Duration.ofMillis(200))

            repeat(3) {
                publisher.publish(buildAuthEvent(aggregateId = sameAggregateId, eventType = "UserLocked"))
            }

            val deadline = System.currentTimeMillis() + 20_000L
            while (count < 3 && System.currentTimeMillis() < deadline) {
                val records = rawConsumer.poll(Duration.ofMillis(500))
                records.forEach { record ->
                    partitions.add(record.partition())
                    count++
                }
            }
            rawConsumer.close()

            count shouldBe 3
            partitions.size shouldBe 1
        }
    }

    given("DomainEventEnvelope 페이로드 구조 검증") {
        then("수신된 JSON에 DomainEventEnvelope 필수 필드가 모두 존재한다") {
            val aggregateId = 12345L
            val consumer = buildConsumer()
            awaitAssignment(consumer)

            publisher.publish(buildAuthEvent(aggregateId = aggregateId, eventType = "UserCreated", companyId = 7L))

            val received = pollMessages(consumer, 1)
            consumer.close()

            val envelope = received.firstOrNull()
            envelope shouldNotBe null

            val keys = envelope!!.keys.map { it.toString() }.toSet()
            keys.contains("eventId") shouldBe true
            keys.contains("eventType") shouldBe true
            keys.contains("eventVersion") shouldBe true
            keys.contains("occurredAt") shouldBe true
            keys.contains("aggregateType") shouldBe true
            keys.contains("aggregateId") shouldBe true
            keys.contains("companyId") shouldBe true
            keys.contains("action") shouldBe true
            keys.contains("state") shouldBe true

            envelope["eventType"] shouldBe "UserCreated"
            envelope["aggregateId"] shouldBe aggregateId.toInt()
            envelope["companyId"] shouldBe 7
        }
    }
}) {
    companion object {
        val kafka: KafkaContainer = KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.0"),
        ).withReuse(true).also { it.start() }
    }
}
