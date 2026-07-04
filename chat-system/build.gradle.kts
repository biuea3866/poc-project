plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.biuea"
version = "0.0.1-SNAPSHOT"
description = "chat-system"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Kotest + MockK
    val kotestVersion = "5.8.0"
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.mockk:mockk:1.13.8")

    // Testcontainers (Redis via GenericContainer)
    testImplementation("org.testcontainers:testcontainers:1.19.3")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.test {
    // 기본 test 는 빠르게 유지한다. docker-compose 태그는 opt-in(composeTest)에서만 실행한다.
    useJUnitPlatform { excludeTags("dockercompose") }
    // 다중 임베디드 서버 인스턴스 + 대량 WebSocket 소켓 부하 테스트를 위한 힙
    maxHeapSize = "3g"
}

// 실제 분리된 프로세스(컨테이너) 인스턴스를 docker-compose 로 띄워 검증하는 opt-in 태스크.
// 실행: ./gradlew composeTest  (Docker 필요, bootJar 선행)
val composeTest by tasks.registering(Test::class) {
    description = "docker-compose 로 분리된 프로세스 멀티 인스턴스를 검증한다"
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform { includeTags("dockercompose") }
    dependsOn(tasks.named("bootJar"))
    maxHeapSize = "2g"
    // Testcontainers 로컬 compose 가 docker/docker-compose 와 자격증명 헬퍼를 찾도록 PATH 를 보강한다
    val dockerBinDirs = listOf("/opt/homebrew/bin", "/Applications/Docker.app/Contents/Resources/bin")
    environment("PATH", (listOf(System.getenv("PATH")) + dockerBinDirs).joinToString(":"))
}
