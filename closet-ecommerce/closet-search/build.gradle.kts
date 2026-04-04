dependencies {
    implementation(project(":closet-common"))

    // Spring Boot Web
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Elasticsearch
    implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")
    testImplementation("org.springframework.kafka:spring-kafka-test")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Test
    testImplementation("io.kotest:kotest-runner-junit5:${Versions.KOTEST}")
    testImplementation("io.kotest:kotest-assertions-core:${Versions.KOTEST}")
    testImplementation("io.kotest:kotest-property:${Versions.KOTEST}")
    testImplementation("io.kotest.extensions:kotest-extensions-spring:${Versions.KOTEST_SPRING}")
    testImplementation("io.mockk:mockk:${Versions.MOCKK}")
    testImplementation("org.testcontainers:testcontainers:${Versions.TESTCONTAINERS}")
    testImplementation("org.testcontainers:junit-jupiter:${Versions.TESTCONTAINERS}")
    testImplementation("org.testcontainers:elasticsearch:${Versions.TESTCONTAINERS}")
}
