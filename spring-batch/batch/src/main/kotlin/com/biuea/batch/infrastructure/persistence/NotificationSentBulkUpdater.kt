package com.biuea.batch.infrastructure.persistence

import com.biuea.batch.domain.NotificationGroup
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.PreparedStatement

/**
 * 발송 완료된 묶음들의 sent 플래그를 chunk 단위로 일괄 갱신한다.
 * stateless 하므로 멀티스레드 Writer 에서 공유 안전하다.
 */
@Component
class NotificationSentBulkUpdater(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun markSent(groups: List<NotificationGroup>): Int {
        if (groups.isEmpty()) return 0
        jdbcTemplate.batchUpdate(
            GroupedNotificationSql.MARK_SENT,
            object : BatchPreparedStatementSetter {
                override fun setValues(ps: PreparedStatement, i: Int) {
                    val group = groups[i]
                    ps.setLong(1, group.userId)
                    ps.setString(2, group.type.name)
                    ps.setString(3, group.groupKey)
                }

                override fun getBatchSize(): Int = groups.size
            },
        )
        return groups.size
    }
}
