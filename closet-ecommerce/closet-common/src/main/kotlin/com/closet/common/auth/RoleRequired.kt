package com.closet.common.auth

/**
 * API 레벨 역할 기반 인가 어노테이션.
 *
 * HandlerMethod에 이 어노테이션이 달려 있으면 RoleInterceptor가
 * X-Member-Role 헤더와 비교하여 접근 권한을 검증한다.
 *
 * @param roles 허용할 역할 목록 (OR 조건 — 하나라도 매칭되면 허용)
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class RoleRequired(
    vararg val roles: MemberRole,
)
