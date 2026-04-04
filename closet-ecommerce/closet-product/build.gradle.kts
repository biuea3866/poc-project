dependencies {
    implementation(project(":closet-common"))

    // Spring Boot Web
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Kafka (Outbox Poller에서 사용)
    implementation("org.springframework.kafka:spring-kafka")
    testImplementation("org.springframework.kafka:spring-kafka-test")
}
