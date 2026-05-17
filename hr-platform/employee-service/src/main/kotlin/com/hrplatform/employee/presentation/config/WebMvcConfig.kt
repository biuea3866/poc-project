package com.hrplatform.employee.presentation.config

import com.hrplatform.employee.presentation.auth.AuthEmploymentIdArgumentResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val authEmploymentIdArgumentResolver: AuthEmploymentIdArgumentResolver,
) : WebMvcConfigurer {

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(authEmploymentIdArgumentResolver)
    }
}
