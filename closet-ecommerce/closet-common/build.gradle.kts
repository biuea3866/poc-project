plugins {
    `java-test-fixtures`
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.kafka:spring-kafka")

    // AOP (DistributedLock 등)
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Redisson (분산 락)
    implementation("org.redisson:redisson-spring-boot-starter:3.25.2")

    // AWS S3 SDK (MinIO 호환 — endpoint만 변경하면 AWS S3로 전환 가능)
    implementation(platform("software.amazon.awssdk:bom:2.25.0"))
    implementation("software.amazon.awssdk:s3")

    // testFixtures — 통합 테스트 베이스 클래스 (MySQL + Redis Testcontainers)
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito")
    }
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testFixturesImplementation("org.testcontainers:testcontainers:${Versions.TESTCONTAINERS}")
    testFixturesImplementation("org.testcontainers:junit-jupiter:${Versions.TESTCONTAINERS}")
    testFixturesImplementation("org.testcontainers:mysql:${Versions.TESTCONTAINERS}")
    testFixturesImplementation("io.kotest:kotest-runner-junit5:${Versions.KOTEST}")
    testFixturesImplementation("io.kotest:kotest-assertions-core:${Versions.KOTEST}")
    testFixturesImplementation("io.kotest.extensions:kotest-extensions-spring:${Versions.KOTEST_SPRING}")
    testFixturesImplementation("io.mockk:mockk:${Versions.MOCKK}")
    testFixturesImplementation("com.mysql:mysql-connector-j")
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<Jar>("jar") {
    enabled = true
}
