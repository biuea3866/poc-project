package com.biuea.batch.presentation.seed

import com.biuea.batch.domain.NotificationType
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.time.ZonedDateTime
import kotlin.random.Random

/**
 * 예약 알림 데이터를 대량 시딩한다. JPA 영속성 컨텍스트를 쓰지 않고 JDBC batch insert 로 메모리 폭주를 피한다.
 * 100만건은 1000건씩 끊어서 insert 한다. group_key 는 (user, type)별 한정된 풀에서 뽑아
 * 자연스럽게 묶음/단건 분포가 생기도록 한다.
 */
@Component
class ReservedNotificationSeeder(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun clear() {
        jdbcTemplate.execute("TRUNCATE TABLE reserved_notification")
    }

    /**
     * @param totalCount 총 예약 알림 건수 (예: 1_000_000)
     * @param userCount  유저 수 (예: 10_000)
     * @param keyPoolPerType 유저·유형당 group_key 풀 크기 — 작을수록 묶음 비율↑
     */
    fun seed(
        totalCount: Int,
        userCount: Int,
        keyPoolPerType: Int = 15,
    ) {
        val random = Random(SEED)
        val createdAt = Timestamp.from(ZonedDateTime.now().toInstant())
        val types = NotificationType.entries

        var remaining = totalCount
        while (remaining > 0) {
            val batchSize = minOf(BATCH_SIZE, remaining)
            val rows = ArrayList<Row>(batchSize)
            repeat(batchSize) {
                val userId = random.nextLong(1, userCount + 1L)
                val type = types[random.nextInt(types.size)]
                val keyIndex = random.nextInt(keyPoolPerType)
                val groupKey = "${type.keyPrefix()}-$keyIndex"
                val payload = "${type.name} payload #${random.nextInt(1_000_000)}"
                rows.add(Row(userId, type.name, groupKey, payload, createdAt))
            }
            insertBatch(rows)
            remaining -= batchSize
        }
    }

    private fun insertBatch(rows: List<Row>) {
        jdbcTemplate.batchUpdate(
            INSERT_SQL,
            object : BatchPreparedStatementSetter {
                override fun setValues(ps: PreparedStatement, i: Int) {
                    val row = rows[i]
                    ps.setLong(1, row.userId)
                    ps.setString(2, row.type)
                    ps.setString(3, row.groupKey)
                    ps.setString(4, row.payload)
                    ps.setTimestamp(5, row.createdAt)
                }

                override fun getBatchSize(): Int = rows.size
            },
        )
    }

    private fun NotificationType.keyPrefix(): String =
        when (this) {
            NotificationType.POST_COMMENT -> "post"
            NotificationType.KEYWORD_INTEREST -> "keyword"
            NotificationType.TICKET_REMINDER -> "ticket"
        }

    private data class Row(
        val userId: Long,
        val type: String,
        val groupKey: String,
        val payload: String,
        val createdAt: Timestamp,
    )

    companion object {
        private const val BATCH_SIZE = 1000
        private const val SEED = 20260613L // 재현 가능한 시딩
        private const val INSERT_SQL =
            "INSERT INTO reserved_notification " +
                "(user_id, notification_type, group_key, payload, sent, created_at) " +
                "VALUES (?, ?, ?, ?, 0, ?)"
    }
}
