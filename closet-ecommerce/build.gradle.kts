plugins {
    kotlin("jvm") version Versions.KOTLIN
    kotlin("plugin.spring") version Versions.KOTLIN apply false
    kotlin("plugin.jpa") version Versions.KOTLIN apply false
    kotlin("kapt") version Versions.KOTLIN apply false
    id("org.springframework.boot") version Versions.SPRING_BOOT apply false
    id("io.spring.dependency-management") version Versions.SPRING_DEPENDENCY_MANAGEMENT apply false
}

allprojects {
    group = "com.closet"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

// Gateway 모듈은 WebFlux 기반이므로 JPA/QueryDSL/Flyway 의존성 제외
val jpaModules = subprojects.filter { it.name != "closet-gateway" }

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    dependencies {
        // Kotlin
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("org.jetbrains.kotlin:kotlin-stdlib")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

        // Logging
        implementation("io.github.microutils:kotlin-logging-jvm:${Versions.KOTLIN_LOGGING}")

        // Actuator + Prometheus Metrics
        implementation("org.springframework.boot:spring-boot-starter-actuator")
        implementation("io.micrometer:micrometer-registry-prometheus")

        // Test
        testImplementation("org.springframework.boot:spring-boot-starter-test") {
            exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
            exclude(group = "org.mockito")
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "21"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

// JPA/QueryDSL/Flyway 의존성은 Gateway 외 모듈에만 적용
configure(jpaModules) {
    apply(plugin = "kotlin-kapt")
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")

    dependencies {
        // Spring Boot
        implementation("org.springframework.boot:spring-boot-starter-data-jpa")
        implementation("org.springframework.boot:spring-boot-starter-validation")

        // QueryDSL
        implementation("com.querydsl:querydsl-jpa:${Versions.QUERYDSL}:jakarta")
        "kapt"("com.querydsl:querydsl-apt:${Versions.QUERYDSL}:jakarta")
        "kapt"("jakarta.annotation:jakarta.annotation-api")
        "kapt"("jakarta.persistence:jakarta.persistence-api")

        // Database
        runtimeOnly("com.mysql:mysql-connector-j")

        // Flyway
        implementation("org.flywaydb:flyway-core")
        implementation("org.flywaydb:flyway-mysql")

        // Test
        testImplementation("io.kotest:kotest-runner-junit5:${Versions.KOTEST}")
        testImplementation("io.kotest:kotest-assertions-core:${Versions.KOTEST}")
        testImplementation("io.kotest:kotest-property:${Versions.KOTEST}")
        testImplementation("io.kotest.extensions:kotest-extensions-spring:${Versions.KOTEST_SPRING}")
        testImplementation("io.mockk:mockk:${Versions.MOCKK}")
        testImplementation("org.testcontainers:testcontainers:${Versions.TESTCONTAINERS}")
        testImplementation("org.testcontainers:junit-jupiter:${Versions.TESTCONTAINERS}")
        testImplementation("org.testcontainers:mysql:${Versions.TESTCONTAINERS}")
    }
}
