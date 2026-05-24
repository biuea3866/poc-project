package com.biuea.springai.security

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

/**
 * 보안 필터 체인.
 *
 * 공개 경로:
 *   - POST /auth/login    : 토큰 발급
 *   - /actuator/health    : 헬스체크
 *
 * 보호 경로:
 *   - `/sse`, `/mcp/...`  : MCP SSE 핸드셰이크 + 메시지 (JWT 필수)
 *   - `/api/...`          : 인앱 REST API
 *
 * 필터 순서: JwtAuthenticationFilter → RateLimitFilter → 기본 인가 평가
 */
@Configuration
@EnableConfigurationProperties(JwtProperties::class, ClientCatalogProperties::class, RateLimitProperties::class)
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtFilter: JwtAuthenticationFilter,
        rateLimitFilter: RateLimitFilter,
        objectMapper: ObjectMapper,
    ): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                it.requestMatchers("/actuator/health").permitAll()
                // UI 정적 리소스 (브라우저에서 직접 로드)
                it.requestMatchers(HttpMethod.GET, "/", "/index.html", "/favicon.ico", "/static/**").permitAll()
                // Spring Boot 의 /error 페이지로 forward 시 헤더가 사라져 401 이 되는 것을 방지
                it.requestMatchers("/error").permitAll()
                it.anyRequest().authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint { _, response, _ ->
                    response.status = 401
                    response.contentType = MediaType.APPLICATION_JSON_VALUE
                    response.characterEncoding = "UTF-8"
                    response.writer.write(
                        objectMapper.writeValueAsString(
                            mapOf("error" to "unauthorized", "message" to "missing or invalid bearer token"),
                        ),
                    )
                }
                it.accessDeniedHandler { _, response, _ ->
                    response.status = 403
                    response.contentType = MediaType.APPLICATION_JSON_VALUE
                    response.characterEncoding = "UTF-8"
                    response.writer.write(
                        objectMapper.writeValueAsString(
                            mapOf("error" to "forbidden", "message" to "insufficient scope"),
                        ),
                    )
                }
            }
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterAfter(rateLimitFilter, JwtAuthenticationFilter::class.java)
        return http.build()
    }
}
