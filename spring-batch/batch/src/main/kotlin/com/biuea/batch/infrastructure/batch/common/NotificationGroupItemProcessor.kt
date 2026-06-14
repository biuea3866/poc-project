package com.biuea.batch.infrastructure.batch.common

import com.biuea.batch.application.SendNotificationGroupUseCase
import com.biuea.batch.domain.NotificationGroup
import org.springframework.batch.infrastructure.item.ItemProcessor
import org.springframework.stereotype.Component

/**
 * 6개 전략 공통 Processor. 발송 단위(묶음) 1건을 UseCase 로 발송하고 그대로 통과시킨다.
 * stateless — 멀티스레드/async 에서 공유 안전하다.
 */
@Component
class NotificationGroupItemProcessor(
    private val sendNotificationGroupUseCase: SendNotificationGroupUseCase,
) : ItemProcessor<NotificationGroup, NotificationGroup> {
    override fun process(item: NotificationGroup): NotificationGroup =
        sendNotificationGroupUseCase.execute(item)
}
