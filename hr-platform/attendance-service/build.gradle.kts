plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.kotlin.noarg)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    kotlin("kapt")
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
        mavenBom("org.testcontainers:testcontainers-bom:${libs.versions.testcontainers.get()}")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

noArg {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

dependencies {
    // Internal modules
    implementation(project(":core"))
    implementation(project(":common-kafka"))

    // Spring Boot
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.data.redis)

    // Spring Security (JWT 검증용)
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Kotlin
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)

    // Jackson
    implementation(libs.bundles.jackson)

    // Kafka
    implementation(libs.spring.kafka)

    // Database
    runtimeOnly(libs.mysql.connector)
    implementation(libs.flyway.core)
    implementation(libs.flyway.mysql)

    // QueryDSL
    implementation("${libs.querydsl.jpa.get()}:jakarta")
    kapt("${libs.querydsl.apt.get()}:jakarta")
    kapt("jakarta.annotation:jakarta.annotation-api")
    kapt("jakarta.persistence:jakarta.persistence-api")

    // Hypersistence Utils (JsonStringType)
    implementation(libs.hypersistence.utils)

    // JWT
    implementation(libs.bundles.jjwt)

    // Micrometer
    implementation(libs.micrometer.registry.prometheus)

    // Test
    testImplementation(libs.spring.boot.starter.test) {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito")
    }
    testImplementation(libs.spring.kafka.test)
    testImplementation(libs.bundles.kotest.all)
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.spring)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.mysql)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.testcontainers.core)
}
