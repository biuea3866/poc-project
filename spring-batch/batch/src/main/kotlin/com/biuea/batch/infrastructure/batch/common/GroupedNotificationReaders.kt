package com.biuea.batch.infrastructure.batch.common

import com.biuea.batch.domain.NotificationGroup
import com.biuea.batch.domain.NotificationType
import com.biuea.batch.infrastructure.persistence.GroupedNotificationSql
import com.biuea.batch.infrastructure.persistence.NotificationGroupRowMapper
import org.springframework.batch.infrastructure.item.database.JdbcCursorItemReader
import org.springframework.batch.infrastructure.item.database.builder.JdbcCursorItemReaderBuilder
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.stereotype.Component
import javax.sql.DataSource

/**
 * 발송 단위(묶음) 스트리밍 Reader 팩토리. JdbcCursorItemReader + MySQL 스트리밍(fetchSize=MIN_VALUE)으로
 * GROUP BY 결과를 클라이언트 메모리에 모두 적재하지 않고 한 건씩 흘려보낸다 → 메모리 비교 공정성 확보.
 *
 * verifyCursorPosition=false 는 멀티스레드(SynchronizedItemStreamReader) 래핑 시 필수.
 * saveState=false — 벤치마크는 restart 를 쓰지 않는다.
 */
@Component
class GroupedNotificationReaders(
    private val dataSource: DataSource,
) {
    fun userRangeReader(
        name: String,
        userIdFrom: Long,
        userIdTo: Long,
    ): JdbcCursorItemReader<NotificationGroup> =
        build(name, GroupedNotificationSql.GROUP_BY_USER_RANGE) { ps ->
            ps.setLong(1, userIdFrom)
            ps.setLong(2, userIdTo)
        }

    fun fullRangeReader(name: String): JdbcCursorItemReader<NotificationGroup> =
        userRangeReader(name, BatchConstants.USER_ID_FROM_ALL, BatchConstants.USER_ID_TO_ALL)

    fun typeReader(
        name: String,
        type: NotificationType,
    ): JdbcCursorItemReader<NotificationGroup> =
        build(name, GroupedNotificationSql.GROUP_BY_TYPE) { ps ->
            ps.setString(1, type.name)
        }

    private fun build(
        name: String,
        sql: String,
        setter: PreparedStatementSetter,
    ): JdbcCursorItemReader<NotificationGroup> =
        JdbcCursorItemReaderBuilder<NotificationGroup>()
            .name(name)
            .dataSource(dataSource)
            .sql(sql)
            .rowMapper(NotificationGroupRowMapper())
            .preparedStatementSetter(setter)
            .fetchSize(Integer.MIN_VALUE)
            .verifyCursorPosition(false)
            .saveState(false)
            .build()
}
