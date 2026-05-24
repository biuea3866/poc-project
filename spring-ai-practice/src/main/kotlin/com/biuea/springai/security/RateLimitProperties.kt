package com.biuea.springai.security

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Rate limit 설정. application.yml `security.rate-limit.*` 에 바인딩.
 *
 * Resilience4j RateLimiter 의미:
 *   - limitForPeriod: 한 주기 동안 허용되는 호출 수
 *   - refreshPeriodSeconds: 카운터가 리셋되는 주기
 *   - timeoutMillis: 슬롯이 비기를 기다리는 최대 시간 (0 = 즉시 거부)
 *
 * 예) limitForPeriod=30, refreshPeriodSeconds=60 → 분당 30회.
 */
@ConfigurationProperties(prefix = "security.rate-limit")
data class RateLimitProperties(
    val limitForPeriod: Int = 30,
    val refreshPeriodSeconds: Long = 60,
    val timeoutMillis: Long = 0,
)
