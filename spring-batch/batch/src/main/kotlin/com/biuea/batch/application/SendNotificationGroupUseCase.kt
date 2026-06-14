package com.biuea.batch.application

import com.biuea.batch.domain.NotificationDispatchService
import com.biuea.batch.domain.NotificationGroup
import org.springframework.stereotype.Component

/**
 * 발송 단위(묶음) 1건을 발송하는 UseCase. ItemProcessor 가 호출한다.
 *
 * ADR-0001: 트랜잭션은 Spring Batch 의 chunk 경계가 소유하므로 @Transactional 을 붙이지 않는다.
 * 발송 후 sent 갱신은 chunk 단위로 Writer 가 일괄 수행한다.
 * 발송에 성공한 group 을 그대로 반환해 Writer 가 갱신 대상으로 쓰게 한다.
 */
@Component
class SendNotificationGroupUseCase(
    private val dispatchService: NotificationDispatchService,
) {
    fun execute(group: NotificationGroup): NotificationGroup {
        dispatchService.dispatch(group)
        return group
    }
}
