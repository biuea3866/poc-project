package com.hrplatform.attendance.infrastructure.config

import org.springframework.data.domain.AuditorAware
import org.springframework.stereotype.Component
import java.util.Optional

/**
 * JPA Auditing 에서 createdBy / updatedBy 에 사용할 현재 auditor 를 제공한다.
 *
 * MVP: SecurityContext 미연동 시 Optional.of(0L) 시스템 사용자 반환.
 * AT-24 완료 후: SecurityContextHolder 에서 employmentId 추출 예정.
 *
 * employee-service/auth-service 의 auditorAware/authAuditorAware 와 빈명이 달라야 하므로
 * attendanceAuditorAware 로 등록.
 */
@Component("attendanceAuditorAware")
class SpringAuditorAware : AuditorAware<Long> {

    override fun getCurrentAuditor(): Optional<Long> = Optional.of(0L)
}
