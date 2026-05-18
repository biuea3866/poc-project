package com.hrplatform.auth.infrastructure.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

/**
 * JPA Auditing 활성화. test profile (JPA autoconfigure exclude) 에서는 비활성.
 * test-integration profile (실제 JPA + Testcontainers) 에서는 활성.
 */
@Configuration
@Profile("!test")
@EnableJpaAuditing(
    auditorAwareRef = "authAuditorAware",
    dateTimeProviderRef = "utcDateTimeProvider",
)
class JpaAuditingConfig
