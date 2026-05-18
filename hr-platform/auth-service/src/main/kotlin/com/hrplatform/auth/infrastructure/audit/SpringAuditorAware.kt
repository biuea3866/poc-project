package com.hrplatform.auth.infrastructure.audit

import org.springframework.data.domain.AuditorAware
import org.springframework.stereotype.Component
import java.util.Optional

/**
 * JPA Auditing 에서 createdBy / updatedBy 에 사용할 현재 auditor 를 제공한다.
 *
 * MVP: SecurityContext 미연동 시 Optional.of(0L) 시스템 사용자 반환.
 * AT-CTRL 완료 후: SecurityContextHolder 에서 AuthPrincipal.userAccountId 추출 예정.
 *
 * employee-service 의 SpringAuditorAware(@Component("auditorAware")) 와 빈명이 달라야 하므로
 * authAuditorAware 로 등록 (ADR-003 §7).
 */
@Component("authAuditorAware")
class SpringAuditorAware : AuditorAware<Long> {

    override fun getCurrentAuditor(): Optional<Long> = Optional.of(0L)
}
