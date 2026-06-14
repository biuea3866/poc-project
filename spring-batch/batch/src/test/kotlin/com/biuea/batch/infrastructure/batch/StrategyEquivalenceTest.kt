package com.biuea.batch.infrastructure.batch

import com.biuea.batch.application.ResetBatchStateUseCase
import com.biuea.batch.domain.NotificationType
import com.biuea.batch.domain.ReservedNotificationRepository
import com.biuea.batch.infrastructure.batch.common.BatchConstants
import com.biuea.batch.support.IntegrationTestBase
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.time.ZonedDateTime
import java.util.UUID

/**
 * S-01 동일출력 검증: 6개 전략이 같은 데이터셋을 처리하면 동일한 결과(모든 raw 행 sent=1)에 수렴한다.
 * 전략마다 reset → run → 전수 발송 검증을 반복한다.
 */
class StrategyEquivalenceTest
    @Autowired
    constructor(
        private val jobs: Map<String, Job>,
        private val jobOperator: JobOperator,
        private val jdbcTemplate: JdbcTemplate,
        private val repository: ReservedNotificationRepository,
        private val resetBatchStateUseCase: ResetBatchStateUseCase,
    ) : IntegrationTestBase() {
        private fun seedFixedDataset() {
            jdbcTemplate.execute("TRUNCATE TABLE reserved_notification")
            // user1: POST post-1×3, post-2×1, KW kw-1×2, TICKET t-1×1
            insert(1L, NotificationType.POST_COMMENT, "post-1", 3)
            insert(1L, NotificationType.POST_COMMENT, "post-2", 1)
            insert(1L, NotificationType.KEYWORD_INTEREST, "kw-1", 2)
            insert(1L, NotificationType.TICKET_REMINDER, "t-1", 1)
            // user2: POST post-1×1, KW kw-1×4, TICKET t-2×1
            insert(2L, NotificationType.POST_COMMENT, "post-1", 1)
            insert(2L, NotificationType.KEYWORD_INTEREST, "kw-1", 4)
            insert(2L, NotificationType.TICKET_REMINDER, "t-2", 1)
            // user3: TICKET t-3×1
            insert(3L, NotificationType.TICKET_REMINDER, "t-3", 1)
        }

        private fun insert(userId: Long, type: NotificationType, groupKey: String, count: Int) {
            val now = Timestamp.from(ZonedDateTime.now().toInstant())
            repeat(count) { i ->
                jdbcTemplate.update(
                    "INSERT INTO reserved_notification (user_id, notification_type, group_key, payload, sent, created_at) " +
                        "VALUES (?, ?, ?, ?, 0, ?)",
                    userId, type.name, groupKey, "payload-$i", now,
                )
            }
        }

        private val allJobNames =
            listOf(
                BatchConstants.JOB_SINGLE_CHUNK,
                BatchConstants.JOB_SINGLE_TASKLET,
                BatchConstants.JOB_ASYNC,
                BatchConstants.JOB_MULTI_THREAD,
                BatchConstants.JOB_PARALLEL,
                BatchConstants.JOB_PARTITION,
            )

        @Test
        fun `S-01 6개 전략 모두 같은 데이터셋을 전수 발송(sent=14)으로 동일 수렴한다`() {
            seedFixedDataset()
            val totalRaw = repository.countTotal()
            totalRaw shouldBe 14L

            allJobNames.forEach { jobName ->
                resetBatchStateUseCase.execute()
                repository.countSent() shouldBe 0L

                val job = jobs.getValue(jobName)
                val params =
                    JobParametersBuilder()
                        .addString("run.id", UUID.randomUUID().toString())
                        .toJobParameters()
                val execution = jobOperator.start(job, params)

                withClue(jobName) {
                    execution.status shouldBe BatchStatus.COMPLETED
                    repository.countSent() shouldBe totalRaw
                }
            }
        }

        private fun <T> withClue(clue: Any, block: () -> T): T {
            try {
                return block()
            } catch (e: AssertionError) {
                throw AssertionError("[$clue] ${e.message}", e)
            }
        }
    }
