plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.spring") version "2.1.20"
    id("org.springframework.boot") version "3.4.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.biuea"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

extra["springAiVersion"] = "1.1.6"

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Spring AI - chat model (Ollama 기본, 로컬 무료)
    implementation("org.springframework.ai:spring-ai-starter-model-ollama")

    // Spring AI - OpenAI 이미지 생성(ImageModel) 용. API 키 미설정 시 ImageModel 빈 미등록 → 도구가 graceful 메시지 반환.
    implementation("org.springframework.ai:spring-ai-starter-model-openai")

    // Spring AI - MCP 서버 (외부 LLM 호스트가 /sse 로 접속해 도구 호출)
    implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc")

    // Spring AI - RAG (QuestionAnswerAdvisor 자동 컨텍스트 주입). VectorStore (SimpleVectorStore) 는 spring-ai-vector-store 포함.
    implementation("org.springframework.ai:spring-ai-advisors-vector-store")

    // 보안 (JWT 인증 + 스코프 인가)
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // JWT (jjwt 0.12.x)
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Rate limit (Resilience4j 인메모리 RateLimiter)
    implementation("io.github.resilience4j:resilience4j-ratelimiter:2.2.0")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
        events("passed", "failed")
    }
}
