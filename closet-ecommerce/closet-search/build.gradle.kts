dependencies {
    implementation(project(":closet-common"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.kafka:spring-kafka")

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
