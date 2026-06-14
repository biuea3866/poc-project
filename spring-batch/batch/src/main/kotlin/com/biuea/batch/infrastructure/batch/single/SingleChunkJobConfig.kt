package com.biuea.batch.infrastructure.batch.single

import com.biuea.batch.domain.NotificationGroup
import com.biuea.batch.infrastructure.batch.common.BatchConstants
import com.biuea.batch.infrastructure.batch.common.GroupedNotificationReaders
import com.biuea.batch.infrastructure.batch.common.NotificationGroupItemProcessor
import com.biuea.batch.infrastructure.batch.common.NotificationGroupItemWriter
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

/**
 * 전략 1 — 단일 스레드 chunk 기반(기준선).
 * chunk=1000, 스레드 1. 가장 단순하며 다른 전략의 성능 비교 기준이 된다.
 */
@Configuration
class SingleChunkJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val readers: GroupedNotificationReaders,
    private val processor: NotificationGroupItemProcessor,
    private val writer: NotificationGroupItemWriter,
) {
    @Bean
    fun singleChunkJob(): Job =
        JobBuilder(BatchConstants.JOB_SINGLE_CHUNK, jobRepository)
            .start(singleChunkStep())
            .build()

    @Bean
    fun singleChunkStep() =
        StepBuilder("singleChunkStep", jobRepository)
            .chunk<NotificationGroup, NotificationGroup>(BatchConstants.CHUNK_SINGLE)
            .reader(readers.fullRangeReader("singleChunkReader"))
            .processor(processor)
            .writer(writer)
            .transactionManager(transactionManager)
            .build()
}
