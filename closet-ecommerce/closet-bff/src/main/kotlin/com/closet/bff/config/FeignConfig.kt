package com.closet.bff.config

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import feign.codec.ErrorDecoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FeignConfig {
    @Bean
    fun errorDecoder(): ErrorDecoder =
        ErrorDecoder { methodKey, response ->
            when (response.status()) {
                404 -> BusinessException(ErrorCode.ENTITY_NOT_FOUND)
                409 -> BusinessException(ErrorCode.DUPLICATE_ENTITY)
                else -> ErrorDecoder.Default().decode(methodKey, response)
            }
        }
}
