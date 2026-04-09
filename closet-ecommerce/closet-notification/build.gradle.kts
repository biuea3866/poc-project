dependencies {
    implementation(project(":closet-common"))

    // Spring Boot Web
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")
    testImplementation("org.springframework.kafka:spring-kafka-test")
}
