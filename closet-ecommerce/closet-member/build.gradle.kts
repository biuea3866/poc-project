dependencies {
    implementation(project(":closet-common"))

    // Spring Boot Web
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // Spring Security Crypto (BCrypt)
    implementation("org.springframework.security:spring-security-crypto")
}
