package com.biuea.batch.infrastructure.batch.parallel

import com.biuea.batch.domain.NotificationGroup
import com.biuea.batch.domain.NotificationType
import com.biuea.batch.infrastructure.batch.common.BatchConstants
import com.biuea.batch.infrastructure.batch.common.GroupedNotificationReaders
import com.biuea.batch.infrastructure.batch.common.NotificationGroupItemProcessor
import com.biuea.batch.infrastructure.batch.common.NotificationGroupItemWriter
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.FlowBuilder
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.flow.Flow
import org.springframework.batch.core.job.flow.support.SimpleFlow
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.transaction.PlatformTransactionManager

/**
 * 전략 5 — Parallel step (split flow).
 * 알림 유형 3종을 각각 독립 flow(step)로 만들어 동시에 실행한다.
 * 각 step 은 자기 유형만 읽으므로 Reader 공유가 없어 동기화가 불필요하다(데이터 자연 분리).
 * flow 가 정확히 3개이므로 스레드도 3개면 충분하다.
 */
@Configuration
class ParallelStepJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val readers: GroupedNotificationReaders,
    private val processor: NotificationGroupItemProcessor,
    private val writer: NotificationGroupItemWriter,
) {
    private fun typeStep(
        stepName: String,
        type: NotificationType,
    ): Step =
        StepBuilder(stepName, jobRepository)
            .chunk<NotificationGroup, NotificationGroup>(BatchConstants.CHUNK_PARALLEL)
            .reader(readers.typeReader("${stepName}Reader", type))
            .processor(processor)
            .writer(writer)
            .transactionManager(transactionManager)
            .build()

    private fun typeFlow(
        flowName: String,
        stepName: String,
        type: NotificationType,
    ): Flow =
        FlowBuilder<SimpleFlow>(flowName)
            .start(typeStep(stepName, type))
            .build()

    @Bean
    fun parallelTaskExecutor(): TaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = NotificationType.entries.size
            maxPoolSize = NotificationType.entries.size
            setThreadNamePrefix("parallel-flow-")
            initialize()
        }

    @Bean
    fun parallelSplitFlow(): Flow =
        FlowBuilder<SimpleFlow>("parallelSplitFlow")
            .split(parallelTaskExecutor())
            .add(
                typeFlow("postCommentFlow", "postCommentStep", NotificationType.POST_COMMENT),
                typeFlow("keywordFlow", "keywordStep", NotificationType.KEYWORD_INTEREST),
                typeFlow("ticketFlow", "ticketStep", NotificationType.TICKET_REMINDER),
            )
            .build()

    @Bean
    fun parallelStepJob(): Job =
        JobBuilder(BatchConstants.JOB_PARALLEL, jobRepository)
            .start(parallelSplitFlow())
            .end()
            .build()
}
