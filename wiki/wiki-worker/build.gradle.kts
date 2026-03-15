plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.biuea"
version = "0.0.1-SNAPSHOT"
description = "wiki-worker"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}


dependencies {
    implementation(project(":wiki-domain"))

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Spring Web (RestClient for Anthropic/OpenAI HTTP calls)
    // Spring AI 1.0.0 은 Spring Framework 6.x 전용 → Spring Boot 4.x(Framework 7.x)와 바이너리 비호환
    // 직접 HTTP 구현으로 대체
    implementation("org.springframework.boot:spring-boot-starter-web")

    // wiki-domain의 UserService가 PasswordEncoder를 의존 → SecurityConfig 빈 제공용
    implementation("org.springframework.boot:spring-boot-starter-security")

    // PostgreSQL (벡터 저장)
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    runtimeOnly("org.postgresql:postgresql")

    // MySQL (ai_status 업데이트)
    runtimeOnly("com.mysql:mysql-connector-j")

    // Actuator + Prometheus (AI 메트릭 노출)
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
