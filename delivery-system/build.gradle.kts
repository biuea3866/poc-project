plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.jpa") version "1.9.25"
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.biuea"
version = "0.0.1-SNAPSHOT"
description = "delivery-system"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Uber H3 — 육각 격자 공간 인덱스
    implementation("com.uber:h3:4.1.1")

    // Redisson — pub/sub 대기 + 워치독을 쓰는 분산락. 직접 구현 스핀락(20ms 폴링)과 비교 측정용.
    //
    // 3.39.0 을 고른 이유: 이 버전까지의 redisson-spring-boot-starter 가 redisson-spring-data-33
    // (= Spring Data Redis 3.3 / Spring Boot 3.3.x 라인)에 맞춰져 있고, starter pom 이 선언한
    // 검증 대상 Spring Boot 가 3.3.4 다. 3.40.0 부터는 redisson-spring-data-34 로 올라가 Boot 3.4 라인이 된다.
    //
    // starter 가 아니라 core 를 쓴 이유: redisson-spring-boot-starter 의 RedissonAutoConfigurationV2 는
    // @AutoConfiguration(before = RedisAutoConfiguration) + @ConditionalOnMissingBean(RedisConnectionFactory) 로
    // RedissonConnectionFactory 를 선점 등록한다. 그러면 기존 StringRedisTemplate(= 스핀락의 전송 계층)이
    // Lettuce → Redisson 으로 통째로 바뀌어, 비교 기준인 스핀락의 조건이 함께 변한다.
    // 락 메커니즘만 바꿔 재측정하려면 스핀락 쪽은 Lettuce 그대로 둬야 한다.
    implementation("org.redisson:redisson:3.39.0")

    runtimeOnly("com.mysql:mysql-connector-j")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    val kotestVersion = "5.8.0"
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.mockk:mockk:1.13.8")

    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:mysql:1.19.3")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.test {
    // 기본 test 는 빠르게 유지한다. benchmark 태그는 opt-in 태스크에서만 실행한다.
    useJUnitPlatform { excludeTags("benchmark") }
    maxHeapSize = "3g"
    testLogging { showStandardStreams = true }
}

// 라이더 탐색 지연·수락 처리량을 실측하는 벤치마크. 실행: ./gradlew benchmarkTest
val benchmarkTest by tasks.registering(Test::class) {
    description = "반경 검색 지연(P50/P95/P99)과 수락 동시성 제어 처리량을 실측한다"
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform { includeTags("benchmark") }
    maxHeapSize = "3g"
    testLogging { showStandardStreams = true }
}
