package com.closet.product.infrastructure.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Outbox 폴링 활성화 설정.
 * outbox.polling.enabled=true 일 때 @EnableScheduling을 활성화하여
 * OutboxPoller의 @Scheduled 메서드가 실행되도록 한다.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
    name = ["outbox.polling.enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class OutboxConfig
