package com.biuea.batch.infrastructure.batch.single

import com.biuea.batch.infrastructure.batch.common.BatchConstants
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 전략 2 — 단일 스레드 tasklet.
 * step 트랜잭션은 ResourcelessTransactionManager(DB 무관)로 두고, 실제 sent 커밋은 tasklet 내부에서
 * 1000건 단위 독립 트랜잭션으로 수행한다 → 단일 거대 트랜잭션 회피.
 */
@Configuration
class SingleTaskletJobConfig(
    private val jobRepository: JobRepository,
    private val sendAllGroupsTasklet: SendAllGroupsTasklet,
) {
    @Bean
    fun singleTaskletJob(): Job =
        JobBuilder(BatchConstants.JOB_SINGLE_TASKLET, jobRepository)
            .start(singleTaskletStep())
            .build()

    @Bean
    fun singleTaskletStep() =
        StepBuilder("singleTaskletStep", jobRepository)
            .tasklet(sendAllGroupsTasklet, ResourcelessTransactionManager())
            .build()
}
