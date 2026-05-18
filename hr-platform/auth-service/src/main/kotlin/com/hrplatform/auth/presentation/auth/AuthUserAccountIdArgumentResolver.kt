package com.hrplatform.auth.presentation.auth

import com.hrplatform.core.exception.UnauthorizedException
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class AuthUserAccountIdArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(AuthUserAccountId::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Long {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw UnauthorizedException(errorCode = "E_AUTH_REQUIRED", message = "JWT 인증이 필요합니다")
        return (authentication as JwtAuthenticationToken).userAccountId
    }
}
