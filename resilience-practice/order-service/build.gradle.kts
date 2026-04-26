// order-service: 주문 진입점. Resilience4j 4종 패턴(CircuitBreaker/TimeLimiter/Bulkhead/RateLimiter) 적용
//                + Redis 기반 글로벌 RateLimiter (스케일아웃 환경 대응)
dependencies {
    val implementation by configurations
    val testImplementation by configurations

    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    testImplementation("org.wiremock:wiremock-standalone:3.9.1")
    testImplementation("org.testcontainers:junit-jupiter:1.20.2")
    testImplementation("com.redis:testcontainers-redis:2.2.2")
}
