dependencies {
    implementation(project(":closet-common"))

    // Spring Boot Web
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")
    testImplementation("org.springframework.kafka:spring-kafka-test")

    // Integration Test (testFixtures from closet-common)
    testImplementation(testFixtures(project(":closet-common")))
}
