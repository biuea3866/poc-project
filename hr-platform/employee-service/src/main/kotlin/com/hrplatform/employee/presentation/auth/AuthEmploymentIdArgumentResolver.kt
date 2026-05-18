package com.hrplatform.employee.presentation.auth

import com.hrplatform.core.exception.UnauthorizedException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/**
 * HTTP 헤더 `X-Employment-Id` 에서 Employment ID를 추출한다.
 * 파라미터 타입이 Long(non-null)이면 헤더 누락 시 UnauthorizedException을 던진다.
 * 파라미터 타입이 Long?(nullable)이면 헤더 누락 시 null을 반환한다 (시스템 액션 등 actor 불필요한 경우).
 * auth-service 도입 시 JWT subject로 교체.
 */
@Component
class AuthEmploymentIdArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(AuthEmploymentId::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Long? {
        val httpRequest = webRequest.getNativeRequest(HttpServletRequest::class.java)
        val employmentId = httpRequest?.getHeader("X-Employment-Id")?.toLongOrNull()

        // Kotlin `Long`(non-null)은 JVM에서 primitive `long`으로 컴파일되므로 isPrimitive로 구분
        val isNonNullable = parameter.parameterType.isPrimitive
        if (employmentId == null && isNonNullable) {
            throw UnauthorizedException(
                errorCode = "E_AUTH_REQUIRED",
                message = "X-Employment-Id 헤더가 필요합니다 — auth-service 도입 시 JWT에서 추출",
            )
        }

        return employmentId
    }
}
