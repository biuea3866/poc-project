import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.2.21" apply false
    kotlin("plugin.spring") version "2.2.21" apply false
    kotlin("plugin.jpa") version "2.2.21" apply false
    id("org.springframework.boot") version "4.0.1" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    kotlin("kapt") version "2.2.21" apply false
}

allprojects {
    group = "com.openmarket"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "kotlin-spring")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    dependencies {
        add("implementation", "org.jetbrains.kotlin:kotlin-reflect")
        add("implementation", "org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        
        // Logging
        add("implementation", "io.github.microutils:kotlin-logging-jvm:3.0.5")
        
        // Test
        add("testImplementation", "org.springframework.boot:spring-boot-starter-test")
    }

    tasks.withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.add("-Xjsr305=strict")
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

// API 모듈
project(":api") {
    dependencies {
        add("implementation", project(":domain"))
        add("implementation", project(":infra"))
        
        add("implementation", "org.springframework.boot:spring-boot-starter-web")
        add("implementation", "org.springframework.boot:spring-boot-starter-validation")
        add("implementation", "org.springframework.boot:spring-boot-starter-security")
        add("implementation", "com.fasterxml.jackson.module:jackson-module-kotlin")
        
        // JWT
        add("implementation", "io.jsonwebtoken:jjwt-api:0.12.3")
        add("runtimeOnly", "io.jsonwebtoken:jjwt-impl:0.12.3")
        add("runtimeOnly", "io.jsonwebtoken:jjwt-jackson:0.12.3")
        
        // Swagger
        add("implementation", "org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
    }
}

// Domain 모듈
project(":domain") {
    apply(plugin = "kotlin-jpa")
    apply(plugin = "org.jetbrains.kotlin.kapt")

    dependencies {
        add("implementation", "org.springframework.boot:spring-boot-starter-data-jpa")
        
        // QueryDSL
        add("implementation", "com.querydsl:querydsl-jpa:5.0.0:jakarta")
        add("kapt", "com.querydsl:querydsl-apt:5.0.0:jakarta")
    }
}

// Infra 모듈
project(":infra") {
    dependencies {
        add("implementation", project(":domain"))
        
        // Database
        add("implementation", "org.springframework.boot:spring-boot-starter-data-jpa")
        add("runtimeOnly", "com.mysql:mysql-connector-j")
        
        // Redis
        add("implementation", "org.springframework.boot:spring-boot-starter-data-redis")
        add("implementation", "org.redisson:redisson-spring-boot-starter:3.25.0")
        
        // Kafka
        add("implementation", "org.springframework.kafka:spring-kafka")
        
        // Elasticsearch
        add("implementation", "org.springframework.boot:spring-boot-starter-data-elasticsearch")
        
        // AWS S3
        add("implementation", "software.amazon.awssdk:s3:2.21.0")
    }
}

// Batch 모듈
project(":batch") {
    dependencies {
        add("implementation", project(":domain"))
        add("implementation", project(":infra"))
        
        add("implementation", "org.springframework.boot:spring-boot-starter-batch")
    }
}
