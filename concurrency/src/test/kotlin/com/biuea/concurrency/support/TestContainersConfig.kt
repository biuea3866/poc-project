package com.biuea.concurrency.support

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import org.testcontainers.containers.GenericContainer
import org.slf4j.LoggerFactory

@Testcontainers
@SpringBootTest(classes = [com.biuea.concurrency.ConcurrencyApplication::class])
@ContextConfiguration(initializers = [TestContainersConfig.Companion.Initializer::class])
@Import(TestContainersConfig.TestRedisConfig::class)
@TestInstance(Lifecycle.PER_CLASS)
@org.springframework.test.context.ActiveProfiles("test")  // 테스트 프로파일 활성화
open class TestContainersConfig {

    @Autowired(required = false)
    protected var jdbcTemplate: JdbcTemplate? = null

    @Autowired(required = false)
    protected var redisTemplate: RedisTemplate<String, Any>? = null

    companion object {
        private val log = LoggerFactory.getLogger(TestContainersConfig::class.java)

        @Container
        @JvmStatic
        val mysql: MySQLContainer<*> = MySQLContainer(DockerImageName.parse("mysql:8.0.36")).apply {
            withDatabaseName("waiting_number_db")
            withUsername("waiting_user")
            withPassword("waiting_password")
            withReuse(true)
            start()
        }

        @Container
        @JvmStatic
        val redis: GenericContainer<*> = GenericContainer(DockerImageName.parse("redis:7.2-alpine")).apply {
            withExposedPorts(6379)
            withReuse(true)
            start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { mysql.jdbcUrl }
            registry.add("spring.datasource.username") { mysql.username }
            registry.add("spring.datasource.password") { mysql.password }
            registry.add("spring.jpa.hibernate.ddl-auto") { "create" }

            // In case any component reads spring.data.redis.*
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.firstMappedPort }

            // Bean 오버라이딩 허용
            registry.add("spring.main.allow-bean-definition-overriding") { "true" }
        }

        class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
            override fun initialize(applicationContext: ConfigurableApplicationContext) {
                TestPropertyValues.of(
                    "spring.datasource.url=${mysql.jdbcUrl}",
                    "spring.datasource.username=${mysql.username}",
                    "spring.datasource.password=${mysql.password}",
                ).applyTo(applicationContext.environment)
            }
        }
    }

    @AfterEach
    fun cleanup() {
        // Clean DB tables
        jdbcTemplate?.execute("DELETE FROM waiting_numbers")
        jdbcTemplate?.execute("DELETE FROM product_waiting")
        // Clean Redis keys related to waiting numbers
        redisTemplate?.let { template ->
            val keys = template.keys("waiting:number:*")
            if (!keys.isNullOrEmpty()) {
                template.delete(keys)
            }
        }
    }

    @Configuration
    open class TestRedisConfig {
        @Bean
        @Primary
        open fun redisConnectionFactory(): RedisConnectionFactory {
            return LettuceConnectionFactory(redis.host, redis.firstMappedPort)
        }

        @Bean
        @Primary
        open fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
            return RedisTemplate<String, Any>().apply {
                setConnectionFactory(connectionFactory)
                keySerializer = StringRedisSerializer()
                valueSerializer = GenericJackson2JsonRedisSerializer()
                hashKeySerializer = StringRedisSerializer()
                hashValueSerializer = GenericJackson2JsonRedisSerializer()
                afterPropertiesSet()
            }
        }

        @Bean
        @Primary
        open fun redissonClient(): RedissonClient {
            val config = Config().apply {
                useSingleServer()
                    .setAddress("redis://" + redis.host + ":" + redis.firstMappedPort)
                    .setConnectionMinimumIdleSize(1)
                    .setConnectionPoolSize(5)
            }
            return Redisson.create(config)
        }
    }
}
