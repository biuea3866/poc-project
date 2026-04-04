package com.closet.common.config

import com.closet.common.auth.RoleInterceptor
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * WebMvc 설정.
 *
 * RoleInterceptor가 빈으로 등록되어 있으면 인터셉터로 추가한다.
 * Gateway 모듈(WebFlux 기반)에서는 WebMvcConfigurer가 자동으로 비활성화된다.
 */
@Configuration
@ConditionalOnBean(RoleInterceptor::class)
class WebMvcConfig(
    private val roleInterceptor: RoleInterceptor,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(roleInterceptor)
            .addPathPatterns("/api/**")
    }
}
