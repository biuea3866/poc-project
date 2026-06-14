package com.biuea.batch.infrastructure.batch.multithread

import com.biuea.batch.domain.NotificationGroup
import com.biuea.batch.infrastructure.batch.common.BatchConstants
import com.biuea.batch.infrastructure.batch.common.GroupedNotificationReaders
import com.biuea.batch.infrastructure.batch.common.NotificationGroupItemProcessor
import com.biuea.batch.infrastructure.batch.common.NotificationGroupItemWriter
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.item.support.SynchronizedItemStreamReader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.transaction.PlatformTransactionManager

/**
 * 전략 4 — Multi-thread step.
 * 하나의 chunk step 을 코어 수만큼의 스레드로 병렬 처리한다.
 * Reader 를 여러 스레드가 공유하므로 SynchronizedItemStreamReader 로 read() 호출을 직렬화한다(필수).
 * cursor reader 는 saveState=false, verifyCursorPosition=false 로 멀티스레드 안전성을 확보(restart 미지원 트레이드오프).
 */
@Configuration
class MultiThreadStepJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val readers: GroupedNotificationReaders,
    private val processor: NotificationGroupItemProcessor,
    private val writer: NotificationGroupItemWriter,
) {
    @Bean
    fun multiThreadTaskExecutor(): ThreadPoolTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = BatchConstants.CPU_CORES
            maxPoolSize = BatchConstants.CPU_CORES
            setThreadNamePrefix("mt-step-")
            initialize()
        }

    @Bean
    fun multiThreadReader(): SynchronizedItemStreamReader<NotificationGroup> =
        SynchronizedItemStreamReader(readers.fullRangeReader("multiThreadReader"))

    @Bean
    fun multiThreadStepJob(): Job =
        JobBuilder(BatchConstants.JOB_MULTI_THREAD, jobRepository)
            .start(multiThreadStep())
            .build()

    @Bean
    fun multiThreadStep() =
        StepBuilder("multiThreadStep", jobRepository)
            .chunk<NotificationGroup, NotificationGroup>(BatchConstants.CHUNK_PARALLEL)
            .reader(multiThreadReader())
            .processor(processor)
            .writer(writer)
            .transactionManager(transactionManager)
            .taskExecutor(multiThreadTaskExecutor())
            .build()
}
