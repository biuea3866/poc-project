package com.hrplatform.employee.infrastructure.audit

import org.springframework.data.domain.AuditorAware
import org.springframework.stereotype.Component
import java.util.Optional

/**
 * JPA Auditing 에서 createdBy / updatedBy 에 사용할 현재 auditor 를 제공한다.
 *
 * 현재는 stub — 시스템 액션으로 간주하여 Optional.empty() 반환.
 * BE-11a auth 도입 후 Spring SecurityContext 에서 employmentId 를 추출하도록 갱신 예정.
 */
@Component("auditorAware")
class SpringAuditorAware : AuditorAware<Long> {

    override fun getCurrentAuditor(): Optional<Long> = Optional.empty()
}
