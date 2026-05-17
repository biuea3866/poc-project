package com.hrplatform.employee.infrastructure.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.hrplatform.employee.infrastructure.kafka.KafkaDomainEventPublisherUnitTest.Companion.EVENT_TYPES
import com.hrplatform.employee.infrastructure.kafka.KafkaDomainEventPublisherUnitTest.Companion.buildDomainEvent
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
 * Testcontainers Kafka를 사용하는 KafkaDomainEventPublisher 통합 테스트.
 * Spring Boot 컨텍스트 없이 순수 Kafka 클라이언트로 구성해 JPA 의존성을 제거합니다.
 * 각 테스트는 고유한 runId를 action.details에 포함시켜 메시지를 식별합니다.
 */
class KafkaDomainEventPublisherIntegrationTest : BehaviorSpec({

    val bootstrapServers = kafka.bootstrapServers
    val topic = "event.hr.employee.v1"

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
    val publisher = KafkaDomainEventPublisher(
        kafkaTemplate = kafkaTemplate,
        objectMapper = objectMapper,
        employeeTopic = topic,
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

    /**
     * consumer를 subscribe하고 빈 poll을 1회 수행해 파티션 할당을 기다린 뒤 반환한다.
     * 이후 발행된 메시지만 수신(latest offset)하기 위한 준비 단계.
     */
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

    given("13종 DomainEvent 각각 publishAll로 발행") {
        then("컨슈머가 13건을 수신하고 eventType 13종이 모두 포함된다") {
            val consumer = buildConsumer()
            awaitAssignment(consumer)

            val events = EVENT_TYPES.mapIndexed { index, eventType ->
                buildDomainEvent(aggregateId = (index + 1).toLong(), eventType = eventType)
            }
            publisher.publishAll(events)

            val received = pollMessages(consumer, 13)
            consumer.close()

            received.size shouldBe 13
            received.map { it["eventType"] as String }.toSet() shouldBe EVENT_TYPES.toSet()
        }
    }

    given("aggregateId가 같은 이벤트 3건 발행") {
        then("수신된 3건은 모두 같은 파티션이다") {
            val sameAggregateId = 77777L
            val consumer = buildConsumer()
            awaitAssignment(consumer)

            repeat(3) {
                publisher.publish(buildDomainEvent(aggregateId = sameAggregateId, eventType = "EmployeeHired"))
            }

            val partitions = mutableSetOf<Int>()
            val deadline = System.currentTimeMillis() + 15_000L
            var count = 0
            while (count < 3 && System.currentTimeMillis() < deadline) {
                val records = consumer.poll(Duration.ofMillis(500))
                records.forEach { record ->
                    partitions.add(record.partition())
                    count++
                }
            }
            consumer.close()

            count shouldBe 3
            partitions.size shouldBe 1
        }
    }

    given("DomainEventEnvelope 페이로드 구조 검증") {
        then("수신된 JSON에 DomainEventEnvelope 필수 필드가 모두 존재한다") {
            val aggregateId = 99999L
            val consumer = buildConsumer()
            awaitAssignment(consumer)

            publisher.publish(buildDomainEvent(aggregateId = aggregateId, eventType = "EmployeeHired", companyId = 7L))

            val received = pollMessages(consumer, 1)
            consumer.close()

            val envelope = received.firstOrNull()
            envelope shouldNotBe null

            val keys = envelope!!.keys.map { it.toString() }.toSet()
            (keys.contains("eventId")) shouldBe true
            (keys.contains("eventType")) shouldBe true
            (keys.contains("eventVersion")) shouldBe true
            (keys.contains("occurredAt")) shouldBe true
            (keys.contains("aggregateType")) shouldBe true
            (keys.contains("aggregateId")) shouldBe true
            (keys.contains("companyId")) shouldBe true
            (keys.contains("action")) shouldBe true
            (keys.contains("state")) shouldBe true

            envelope["eventType"] shouldBe "EmployeeHired"
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
