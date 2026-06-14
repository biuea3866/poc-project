package com.biuea.batch.infrastructure.batch.async

import com.biuea.batch.domain.NotificationGroup
import com.biuea.batch.infrastructure.batch.common.BatchConstants
import com.biuea.batch.infrastructure.batch.common.GroupedNotificationReaders
import com.biuea.batch.infrastructure.batch.common.NotificationGroupItemProcessor
import com.biuea.batch.infrastructure.batch.common.NotificationGroupItemWriter
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.integration.async.AsyncItemProcessor
import org.springframework.batch.integration.async.AsyncItemWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.transaction.PlatformTransactionManager
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor

/**
 * 전략 3 — AsyncItemProcessor / AsyncItemWriter.
 * Reader 는 단일 스레드로 한 건씩 읽고, Processor(발송 시뮬)만 스레드풀에서 병렬 처리한다.
 * Writer 는 Future 를 unwrap 해 chunk 단위로 단일 스레드 bulk update.
 * latency 가 섞인 발송 부하라 코어 수보다 약간 많은 스레드(max=코어×2)가 유리하다.
 */
@Configuration
class AsyncJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val readers: GroupedNotificationReaders,
    private val processor: NotificationGroupItemProcessor,
    private val writer: NotificationGroupItemWriter,
) {
    @Bean
    fun asyncTaskExecutor(): ThreadPoolTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = BatchConstants.CPU_CORES
            maxPoolSize = BatchConstants.CPU_CORES * 2
            queueCapacity = 200 // 바운드 큐 + CallerRuns 로 reader 백프레셔
            setThreadNamePrefix("async-send-")
            setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
            initialize()
        }

    @Bean
    fun asyncItemProcessor(): AsyncItemProcessor<NotificationGroup, NotificationGroup> =
        AsyncItemProcessor(processor).apply {
            setTaskExecutor(asyncTaskExecutor())
        }

    @Bean
    fun asyncItemWriter(): AsyncItemWriter<NotificationGroup> =
        AsyncItemWriter(writer)

    @Bean
    fun asyncJob(): Job =
        JobBuilder(BatchConstants.JOB_ASYNC, jobRepository)
            .start(asyncStep())
            .build()

    @Bean
    fun asyncStep() =
        StepBuilder("asyncStep", jobRepository)
            .chunk<NotificationGroup, Future<NotificationGroup>>(BatchConstants.CHUNK_PARALLEL)
            .reader(readers.fullRangeReader("asyncReader"))
            .processor(asyncItemProcessor())
            .writer(asyncItemWriter())
            .transactionManager(transactionManager)
            .build()
}
