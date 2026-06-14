package com.biuea.batch.infrastructure.batch.partition

import com.biuea.batch.domain.NotificationGroup
import com.biuea.batch.infrastructure.batch.common.BatchConstants
import com.biuea.batch.infrastructure.batch.common.GroupedNotificationReaders
import com.biuea.batch.infrastructure.batch.common.NotificationGroupItemProcessor
import com.biuea.batch.infrastructure.batch.common.NotificationGroupItemWriter
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.item.database.JdbcCursorItemReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.transaction.PlatformTransactionManager

/**
 * 전략 6 — Partitioning.
 * 매니저 step 이 user_id 범위를 gridSize(코어 수) 등분하고, 각 파티션을 워커 step 인스턴스가 병렬 처리한다.
 * 워커 Reader 는 @StepScope 로 파티션별 [userIdFrom, userIdTo] 를 주입받아 서로 다른 범위를 읽으므로
 * 동기화가 불필요하다(데이터 범위 분리).
 */
@Configuration
class PartitionJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val readers: GroupedNotificationReaders,
    private val processor: NotificationGroupItemProcessor,
    private val writer: NotificationGroupItemWriter,
    private val partitioner: UserIdRangePartitioner,
) {
    private val gridSize = BatchConstants.CPU_CORES

    @Bean
    @StepScope
    fun partitionReader(
        @Value("#{stepExecutionContext['userIdFrom']}") userIdFrom: Long,
        @Value("#{stepExecutionContext['userIdTo']}") userIdTo: Long,
    ): JdbcCursorItemReader<NotificationGroup> =
        readers.userRangeReader("partitionReader", userIdFrom, userIdTo)

    @Bean
    fun partitionWorkerStep(): Step =
        StepBuilder("partitionWorkerStep", jobRepository)
            .chunk<NotificationGroup, NotificationGroup>(BatchConstants.CHUNK_PARALLEL)
            .reader(partitionReader(0, 0))
            .processor(processor)
            .writer(writer)
            .transactionManager(transactionManager)
            .build()

    @Bean
    fun partitionTaskExecutor(): ThreadPoolTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = gridSize
            maxPoolSize = gridSize
            setThreadNamePrefix("partition-")
            initialize()
        }

    @Bean
    fun partitionManagerStep(): Step =
        StepBuilder(BatchConstants.PARTITION_MANAGER_STEP, jobRepository)
            .partitioner("partitionWorkerStep", partitioner)
            .step(partitionWorkerStep())
            .gridSize(gridSize)
            .taskExecutor(partitionTaskExecutor())
            .build()

    @Bean
    fun partitionJob(): Job =
        JobBuilder(BatchConstants.JOB_PARTITION, jobRepository)
            .start(partitionManagerStep())
            .build()
}
