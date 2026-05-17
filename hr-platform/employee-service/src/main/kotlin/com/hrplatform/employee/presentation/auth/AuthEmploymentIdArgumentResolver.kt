package com.hrplatform.employee.presentation.auth

import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/**
 * HTTP 헤더 `X-Employment-Id` 에서 Employment ID를 추출한다.
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
        return httpRequest?.getHeader("X-Employment-Id")?.toLongOrNull()
    }
}
