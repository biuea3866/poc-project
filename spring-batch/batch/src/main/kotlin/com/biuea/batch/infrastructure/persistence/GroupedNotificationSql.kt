package com.biuea.batch.infrastructure.persistence

/**
 * 발송 단위(묶음) 집계 쿼리 모음. 배치 Reader(JdbcCursorItemReader) 와 tasklet 이 공유한다.
 *
 * 멀티스레드 안전 Reader 가 raw SQL 을 요구하는 것은 Spring Batch 프레임워크 특성이며,
 * @Query 어노테이션 금지 규칙(JPA 메서드 쿼리)과는 무관하다.
 */
object GroupedNotificationSql {

    /** user_id 범위 내 미발송(sent=0) 알림을 (user_id, type, group_key) 로 집계한다. */
    const val GROUP_BY_USER_RANGE: String = """
        SELECT user_id, notification_type, group_key,
               COUNT(*) AS cnt, MIN(payload) AS sample_payload
        FROM reserved_notification
        WHERE sent = 0
          AND user_id BETWEEN ? AND ?
        GROUP BY user_id, notification_type, group_key
        ORDER BY user_id, notification_type, group_key
    """

    /** 특정 notification_type 만 집계한다(parallel split flow 용). user_id 전체 범위. */
    const val GROUP_BY_TYPE: String = """
        SELECT user_id, notification_type, group_key,
               COUNT(*) AS cnt, MIN(payload) AS sample_payload
        FROM reserved_notification
        WHERE sent = 0
          AND notification_type = ?
        GROUP BY user_id, notification_type, group_key
        ORDER BY user_id, notification_type, group_key
    """

    /** 발송 후 해당 묶음의 미발송 행을 sent=1 로 갱신한다(chunk bulk update). */
    const val MARK_SENT: String = """
        UPDATE reserved_notification
        SET sent = 1
        WHERE sent = 0
          AND user_id = ?
          AND notification_type = ?
          AND group_key = ?
    """
}
