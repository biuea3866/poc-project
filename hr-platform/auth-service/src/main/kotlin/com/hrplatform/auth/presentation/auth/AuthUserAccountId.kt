package com.hrplatform.auth.presentation.auth

/**
 * SecurityContext에서 현재 인증된 사용자의 userAccountId를 주입받는 어노테이션.
 * Controller 메서드 파라미터에 사용.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class AuthUserAccountId
