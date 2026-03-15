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

extra["springAiVersion"] = "1.0.0"

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
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

    // Spring AI — ChatClient 빌더 API (org.springframework.ai.chat.client)
    // spring-ai-starter-* 는 RestClientAutoConfiguration(Spring Boot 3.x only)을 참조하여 Spring Boot 4.x 충돌
    // → 스타터 대신 코어/클라이언트 모듈 직접 사용
    implementation("org.springframework.ai:spring-ai-client-chat")

    // Spring AI — Anthropic (요약/태깅) : 스타터 제외, 모델 구현체만
    implementation("org.springframework.ai:spring-ai-anthropic")

    // Spring AI — OpenAI (임베딩) : 스타터 제외, 모델 구현체만
    implementation("org.springframework.ai:spring-ai-openai")

    // PostgreSQL (벡터 저장)
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    runtimeOnly("org.postgresql:postgresql")

    // MySQL (ai_status 업데이트)
    runtimeOnly("com.mysql:mysql-connector-j")

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
