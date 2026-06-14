package com.biuea.batch.infrastructure.persistence

import com.biuea.batch.domain.NotificationGroup
import com.biuea.batch.domain.NotificationType
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet

/**
 * GROUP BY 집계 행 → 도메인 NotificationGroup 매핑.
 * 배치 Reader 와 tasklet 이 공유한다.
 */
class NotificationGroupRowMapper : RowMapper<NotificationGroup> {
    override fun mapRow(rs: ResultSet, rowNum: Int): NotificationGroup =
        NotificationGroup(
            userId = rs.getLong("user_id"),
            type = NotificationType.valueOf(rs.getString("notification_type")),
            groupKey = rs.getString("group_key"),
            itemCount = rs.getInt("cnt"),
            samplePayload = rs.getString("sample_payload"),
        )
}
