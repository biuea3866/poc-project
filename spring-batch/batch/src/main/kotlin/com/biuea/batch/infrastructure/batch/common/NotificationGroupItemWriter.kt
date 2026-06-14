package com.biuea.batch.infrastructure.batch.common

import com.biuea.batch.domain.NotificationGroup
import com.biuea.batch.infrastructure.persistence.NotificationSentBulkUpdater
import org.springframework.batch.infrastructure.item.Chunk
import org.springframework.batch.infrastructure.item.ItemWriter
import org.springframework.stereotype.Component

/**
 * 6개 전략 공통 Writer. chunk 단위로 발송 완료된 묶음들의 sent 플래그를 bulk update 한다.
 * stateless — 멀티스레드 Writer 에서 공유 안전(서로 다른 행을 갱신).
 */
@Component
class NotificationGroupItemWriter(
    private val bulkUpdater: NotificationSentBulkUpdater,
) : ItemWriter<NotificationGroup> {
    override fun write(chunk: Chunk<out NotificationGroup>) {
        bulkUpdater.markSent(chunk.items.toList())
    }
}
