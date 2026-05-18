package com.hrplatform.auth.presentation.config

import com.hrplatform.auth.presentation.auth.AuthEmploymentIdArgumentResolver
import com.hrplatform.auth.presentation.auth.AuthUserAccountIdArgumentResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val authUserAccountIdArgumentResolver: AuthUserAccountIdArgumentResolver,
    private val authEmploymentIdArgumentResolver: AuthEmploymentIdArgumentResolver,
) : WebMvcConfigurer {

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(authUserAccountIdArgumentResolver)
        resolvers.add(authEmploymentIdArgumentResolver)
    }
}
