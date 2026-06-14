package com.biuea.batch.application

import com.biuea.batch.domain.ReservedNotificationRepository
import org.springframework.stereotype.Component

/**
 * 벤치마크 반복 사이에 sent 플래그를 false 로 복원해 매 회차 동일한 작업량을 보장한다.
 * 측정 구간(JobLauncher 실행) 밖에서 호출된다.
 */
@Component
class ResetBatchStateUseCase(
    private val repository: ReservedNotificationRepository,
) {
    fun execute(): Int = repository.resetSentFlags()
}
