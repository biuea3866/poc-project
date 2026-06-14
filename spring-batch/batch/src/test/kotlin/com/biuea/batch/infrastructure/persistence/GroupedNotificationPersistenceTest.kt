package com.biuea.batch.infrastructure.persistence

import com.biuea.batch.domain.NotificationGroup
import com.biuea.batch.domain.NotificationType
import com.biuea.batch.domain.ReservedNotificationRepository
import com.biuea.batch.support.IntegrationTestBase
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.time.ZonedDateTime

class GroupedNotificationPersistenceTest
    @Autowired
    constructor(
        private val jdbcTemplate: JdbcTemplate,
        private val bulkUpdater: NotificationSentBulkUpdater,
        private val repository: ReservedNotificationRepository,
    ) : IntegrationTestBase() {
        private val rowMapper = NotificationGroupRowMapper()

        @BeforeEach
        fun cleanUp() {
            jdbcTemplate.execute("TRUNCATE TABLE reserved_notification")
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

        private fun queryGroups(): List<NotificationGroup> =
            jdbcTemplate.query(GroupedNotificationSql.GROUP_BY_USER_RANGE, rowMapper, Long.MIN_VALUE, Long.MAX_VALUE)

        @Test
        fun `R-01 GROUP BY 는 (user, type, group_key) 별로 itemCount 를 정확히 집계한다`() {
            insert(1L, NotificationType.POST_COMMENT, "post-1", 3)
            insert(1L, NotificationType.POST_COMMENT, "post-2", 1)
            insert(2L, NotificationType.KEYWORD_INTEREST, "keyword-1", 2)

            val groups = queryGroups()

            groups shouldHaveSize 3
            val byKey = groups.associateBy { Triple(it.userId, it.type, it.groupKey) }
            byKey.getValue(Triple(1L, NotificationType.POST_COMMENT, "post-1")).itemCount shouldBe 3
            byKey.getValue(Triple(1L, NotificationType.POST_COMMENT, "post-2")).itemCount shouldBe 1
            byKey.getValue(Triple(2L, NotificationType.KEYWORD_INTEREST, "keyword-1")).itemCount shouldBe 2
        }

        @Test
        fun `R-02 type 필터 쿼리는 해당 유형 묶음만 반환한다`() {
            insert(1L, NotificationType.POST_COMMENT, "post-1", 2)
            insert(1L, NotificationType.KEYWORD_INTEREST, "keyword-1", 2)

            val groups =
                jdbcTemplate.query(
                    GroupedNotificationSql.GROUP_BY_TYPE,
                    rowMapper,
                    NotificationType.POST_COMMENT.name,
                )

            groups shouldHaveSize 1
            groups.first().type shouldBe NotificationType.POST_COMMENT
        }

        @Test
        fun `R-03 markSent 후 미발송 GROUP BY 결과에서 해당 묶음이 제외된다`() {
            insert(1L, NotificationType.POST_COMMENT, "post-1", 3)
            insert(1L, NotificationType.POST_COMMENT, "post-2", 1)

            val target = queryGroups().first { it.groupKey == "post-1" }
            val updated = bulkUpdater.markSent(listOf(target))

            updated shouldBe 1
            repository.countSent() shouldBe 3L // post-1 묶음의 raw 3건이 sent=1
            val remaining = queryGroups()
            remaining shouldHaveSize 1
            remaining.first().groupKey shouldBe "post-2"
        }

        @Test
        fun `R-04 resetSentFlags 는 모든 sent 를 false 로 복원하고 count·userIdRange 가 정확하다`() {
            insert(5L, NotificationType.TICKET_REMINDER, "ticket-1", 2)
            insert(9L, NotificationType.TICKET_REMINDER, "ticket-2", 1)
            bulkUpdater.markSent(queryGroups())

            repository.countSent() shouldBe 3L
            val reset = repository.resetSentFlags()

            reset shouldBe 3
            repository.countSent() shouldBe 0L
            repository.countTotal() shouldBe 3L
            val range = repository.userIdRange()
            range.shouldNotBeNull()
            range.min shouldBe 5L
            range.max shouldBe 9L
        }
    }
