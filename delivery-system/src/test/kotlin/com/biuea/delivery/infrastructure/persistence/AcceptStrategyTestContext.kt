package com.biuea.delivery.infrastructure.persistence

import com.biuea.delivery.domain.order.DeliveryAcceptStrategy
import com.biuea.delivery.domain.order.DeliveryOrder
import com.biuea.delivery.domain.order.DeliveryOrderRepository
import com.biuea.delivery.infrastructure.lock.DistributedLockAcceptStrategy
import com.biuea.delivery.infrastructure.lock.RedisDistributedLock
import com.biuea.delivery.support.MySqlContainer
import com.biuea.delivery.support.RedisContainer
import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

/**
 * 수락 동시성 제어 검증용 Spring 컨텍스트.
 *
 * 왜 @SpringBootTest 가 아닌가: 이 레포의 테스트는 Kotest 로 작성하는데(컨벤션), kotest-extensions-spring 이
 * 의존성에 없어 Kotest 스펙에서는 @SpringBootTest/@DynamicPropertySource 가 동작하지 않는다.
 * build.gradle.kts 는 수정 대상이 아니므로 컨텍스트를 직접 띄우고 컨테이너 좌표를 커맨드라인 인자로 주입한다
 * (커맨드라인 프로퍼티가 application.yml 보다 우선순위가 높다).
 *
 * 스캔 범위를 order 수락 경로로 한정해, 동시에 작성 중인 geo/rider 패키지 빈에 영향받지 않게 한다.
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan(
    basePackages = [
        "com.biuea.delivery.infrastructure.persistence",
        "com.biuea.delivery.infrastructure.lock",
    ],
)
@EnableJpaRepositories(basePackages = ["com.biuea.delivery.infrastructure.persistence"])
@EntityScan(basePackages = ["com.biuea.delivery.infrastructure.persistence"])
class AcceptStrategyTestConfiguration {

    @Bean
    fun connectionHoldTimeRecorder(): ConnectionHoldTimeRecorder = ConnectionHoldTimeRecorder()

    @Bean
    fun dataSource(
        dataSourceProperties: DataSourceProperties,
        connectionHoldTimeRecorder: ConnectionHoldTimeRecorder,
    ): DataSource = dataSourceProperties.initializeDataSourceBuilder()
        .type(HikariDataSource::class.java)
        .build()
        .apply {
            poolName = "delivery-accept-pool"
            // 풀 크기 = 동시에 DB 락을 붙잡을 수 있는 스레드 수. 실제 서비스처럼 작게 고정해야
            // "라이더 200명이 몰릴 때 커넥션이 마르는가"를 관측할 수 있다.
            maximumPoolSize = CONNECTION_POOL_SIZE
            metricsTrackerFactory = connectionHoldTimeRecorder
        }

    companion object {
        const val CONNECTION_POOL_SIZE = 20
    }
}

object AcceptStrategyTestContext {

    private val context: ConfigurableApplicationContext by lazy {
        SpringApplicationBuilder(AcceptStrategyTestConfiguration::class.java)
            .web(WebApplicationType.NONE)
            .run(
                "--spring.profiles.active=test",
                "--spring.datasource.url=${MySqlContainer.jdbcUrl}",
                "--spring.datasource.username=${MySqlContainer.username}",
                "--spring.datasource.password=${MySqlContainer.password}",
                "--spring.data.redis.host=${RedisContainer.host}",
                "--spring.data.redis.port=${RedisContainer.port}",
            )
    }

    val pessimisticLockAcceptStrategy: PessimisticLockAcceptStrategy
        get() = context.getBean(PessimisticLockAcceptStrategy::class.java)

    val optimisticLockAcceptStrategy: OptimisticLockAcceptStrategy
        get() = context.getBean(OptimisticLockAcceptStrategy::class.java)

    val distributedLockAcceptStrategy: DistributedLockAcceptStrategy
        get() = context.getBean(DistributedLockAcceptStrategy::class.java)

    val redisDistributedLock: RedisDistributedLock
        get() = context.getBean(RedisDistributedLock::class.java)

    val connectionHoldTimeRecorder: ConnectionHoldTimeRecorder
        get() = context.getBean(ConnectionHoldTimeRecorder::class.java)

    private val deliveryOrderRepository: DeliveryOrderRepository
        get() = context.getBean(DeliveryOrderRepository::class.java)

    /** 전략 3종을 같은 계약으로 검증·측정하기 위한 목록. */
    fun strategies(): List<Pair<String, DeliveryAcceptStrategy>> = listOf(
        "비관적 락(FOR UPDATE)" to pessimisticLockAcceptStrategy,
        "낙관적 락(@Version)" to optimisticLockAcceptStrategy,
        "Redis 분산락" to distributedLockAcceptStrategy,
    )

    fun clearOrders() = deliveryOrderRepository.deleteAll()

    fun seedWaitingRiderOrder(storeId: Long = DEFAULT_STORE_ID): Long =
        requireNotNull(deliveryOrderRepository.save(DeliveryOrder.waitingRider(storeId)).id)

    fun seedWaitingRiderOrders(orderCount: Int, storeId: Long = DEFAULT_STORE_ID): List<Long> =
        deliveryOrderRepository
            .saveAll((1..orderCount).map { DeliveryOrder.waitingRider(storeId) })
            .map { requireNotNull(it.id) }

    fun findOrderBy(orderId: Long): DeliveryOrder? = deliveryOrderRepository.findBy(orderId)

    /**
     * 모든 스레드를 시작 게이트에서 대기시켰다가 한 번에 풀어 "동시에 수락 버튼을 누른" 상황을 만든다.
     * 순차 실행이면 경합이 발생하지 않아 단일 승자 보장이 검증되지 않는다.
     */
    fun <T> runConcurrently(threadCount: Int, task: (Int) -> T): List<T> {
        val executor = Executors.newFixedThreadPool(threadCount)
        val startGate = CountDownLatch(1)
        return try {
            val submittedTasks = (0 until threadCount).map { taskIndex ->
                executor.submit<T> {
                    startGate.await()
                    task(taskIndex)
                }
            }
            startGate.countDown()
            submittedTasks.map { it.get(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
    }

    private const val DEFAULT_STORE_ID = 1_000L
    private const val TASK_TIMEOUT_SECONDS = 120L
}
