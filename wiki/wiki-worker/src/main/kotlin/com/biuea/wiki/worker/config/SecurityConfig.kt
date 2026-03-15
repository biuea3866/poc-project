package com.biuea.wiki.worker.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class SecurityConfig {

    // wiki-domain의 UserService가 PasswordEncoder를 의존하므로 빈 제공 필요
    // wiki-worker에서는 실제로 사용하지 않음
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
