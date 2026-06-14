package com.biuea.batch.presentation.seed

import com.biuea.batch.domain.ReservedNotificationRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

/**
 * benchmark.mode=seed 일 때 예약 알림 데이터를 대량 시딩한다(1회). 벤치마크와 분리된 진입점.
 */
@Component
class SeedRunner(
    private val seeder: ReservedNotificationSeeder,
    private val repository: ReservedNotificationRepository,
    @param:Value("\${benchmark.mode:none}") private val mode: String,
    @param:Value("\${benchmark.total-count:1000000}") private val totalCount: Int,
    @param:Value("\${benchmark.user-count:10000}") private val userCount: Int,
    @param:Value("\${benchmark.key-pool:15}") private val keyPool: Int,
) : CommandLineRunner {
    private val log = LoggerFactory.getLogger(SeedRunner::class.java)

    override fun run(vararg args: String) {
        if (mode != "seed") return

        log.info("시딩 시작 — total {}건, user {}명, keyPool {}", totalCount, userCount, keyPool)
        val startNanos = System.nanoTime()
        seeder.clear()
        seeder.seed(totalCount, userCount, keyPool)
        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000
        log.info("시딩 완료 — {}건 적재, {}ms", repository.countTotal(), elapsedMs)
    }
}
