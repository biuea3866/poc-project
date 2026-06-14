package com.biuea.batch.infrastructure.batch.single

import com.biuea.batch.application.SendNotificationGroupUseCase
import com.biuea.batch.domain.NotificationGroup
import com.biuea.batch.infrastructure.batch.common.BatchConstants
import com.biuea.batch.infrastructure.persistence.GroupedNotificationSql
import com.biuea.batch.infrastructure.persistence.NotificationGroupRowMapper
import com.biuea.batch.infrastructure.persistence.NotificationSentBulkUpdater
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementCreator
import org.springframework.jdbc.core.RowCallbackHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.sql.Connection
import java.sql.ResultSet

/**
 * 전략 2 — 단일 스레드 tasklet.
 * GROUP BY 결과를 MySQL 스트리밍으로 한 건씩 읽어 발송하고, 1000건마다 독립 트랜잭션으로 sent 를 커밋한다.
 * 단일 거대 트랜잭션(undo log 폭증)을 피하기 위한 "내부 수동 chunking"(ADR-0001).
 */
@Component
class SendAllGroupsTasklet(
    private val jdbcTemplate: JdbcTemplate,
    private val sendNotificationGroupUseCase: SendNotificationGroupUseCase,
    private val bulkUpdater: NotificationSentBulkUpdater,
    transactionManager: PlatformTransactionManager,
) : Tasklet {
    private val transactionTemplate = TransactionTemplate(transactionManager)
    private val rowMapper = NotificationGroupRowMapper()

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val buffer = ArrayList<NotificationGroup>(BatchConstants.TASKLET_FLUSH_SIZE)
        var processed = 0

        val statementCreator =
            PreparedStatementCreator { connection: Connection ->
                connection.prepareStatement(
                    GroupedNotificationSql.GROUP_BY_USER_RANGE,
                    ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY,
                ).apply {
                    fetchSize = Integer.MIN_VALUE // MySQL 스트리밍
                    setLong(1, BatchConstants.USER_ID_FROM_ALL)
                    setLong(2, BatchConstants.USER_ID_TO_ALL)
                }
            }

        jdbcTemplate.query(
            statementCreator,
            RowCallbackHandler { rs ->
                val group = rowMapper.mapRow(rs, 0)
                sendNotificationGroupUseCase.execute(group)
                buffer.add(group)
                if (buffer.size >= BatchConstants.TASKLET_FLUSH_SIZE) {
                    flush(buffer)
                    processed += buffer.size
                    buffer.clear()
                }
            },
        )
        if (buffer.isNotEmpty()) {
            flush(buffer)
            processed += buffer.size
        }

        contribution.incrementWriteCount(processed.toLong())
        return RepeatStatus.FINISHED
    }

    private fun flush(groups: List<NotificationGroup>) {
        transactionTemplate.executeWithoutResult {
            bulkUpdater.markSent(groups)
        }
    }
}
