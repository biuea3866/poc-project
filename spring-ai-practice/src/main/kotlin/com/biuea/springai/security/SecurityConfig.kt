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
 * JWT 의 의무 사용 경로는 외부 LLM / MCP 클라이언트가 진입하는 곳에 한정한다.
 *
 * 공개 경로 (permitAll):
 *   - `POST /auth/token`           : MCP 클라이언트 토큰 발급 (client_credentials → JWT)
 *   - `GET  /`, `/index.html`, `/static/...`, `/error`, `/actuator/health`
 *   - `POST /chat`, `/chat/stream` : 브라우저 UI 채팅 — 인증 없음(anonymous 가 catalog:read / order:read 기본 보유)
 *
 * 보호 경로 (authenticated, Bearer JWT 필수):
 *   - `GET  /sse`                  : MCP SSE 핸드셰이크
 *   - `POST /mcp/...`              : MCP JSONRPC 메시지
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
            // anonymous 사용자(UI 채팅 등 permitAll 경로)에게 기본 조회 스코프 부여.
            // → ToolGuardAspect 가 catalog:read / order:read 도구 호출을 통과시킨다.
            // 쓰기 스코프(catalog:write / order:write / shipment:write) 는 일부러 제외 — 외부 인증된 클라이언트만.
            .anonymous {
                it.principal("ui-user").authorities("SCOPE_catalog:read", "SCOPE_order:read")
            }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.POST, "/auth/token").permitAll()
                it.requestMatchers("/actuator/health").permitAll()
                // UI 정적 리소스 (브라우저에서 직접 로드)
                it.requestMatchers(HttpMethod.GET, "/", "/index.html", "/favicon.ico", "/static/**").permitAll()
                // Spring Boot 의 /error 페이지로 forward 시 헤더가 사라져 401 이 되는 것을 방지
                it.requestMatchers("/error").permitAll()
                // 브라우저 UI 채팅 — JWT 없이 호출 가능 (anonymous 가 catalog:read / order:read 보유)
                it.requestMatchers(HttpMethod.POST, "/chat", "/chat/stream").permitAll()
                // 외부 LLM / MCP 클라이언트 진입점만 JWT 필수
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
