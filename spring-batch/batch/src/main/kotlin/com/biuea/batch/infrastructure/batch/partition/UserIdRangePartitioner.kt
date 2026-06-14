package com.biuea.batch.infrastructure.batch.partition

import com.biuea.batch.domain.ReservedNotificationRepository
import org.springframework.batch.core.partition.Partitioner
import org.springframework.batch.infrastructure.item.ExecutionContext
import org.springframework.stereotype.Component

/**
 * user_id 의 [min, max] 범위를 gridSize 등분해 각 파티션에 [userIdFrom, userIdTo] 를 부여한다.
 * 파티션 간 데이터가 겹치지 않으므로 워커 Reader 동기화가 불필요하다.
 */
@Component
class UserIdRangePartitioner(
    private val repository: ReservedNotificationRepository,
) : Partitioner {
    override fun partition(gridSize: Int): MutableMap<String, ExecutionContext> {
        val range = repository.userIdRange() ?: return mutableMapOf()
        val partitionSize = ((range.max - range.min) / gridSize) + 1

        val partitions = LinkedHashMap<String, ExecutionContext>()
        var start = range.min
        var index = 0
        while (start <= range.max) {
            val end = minOf(start + partitionSize - 1, range.max)
            partitions["partition$index"] =
                ExecutionContext().apply {
                    putLong("userIdFrom", start)
                    putLong("userIdTo", end)
                }
            start = end + 1
            index++
        }
        return partitions
    }
}
