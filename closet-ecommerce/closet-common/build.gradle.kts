dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Kafka (Outbox Polling Publisher)
    implementation("org.springframework.kafka:spring-kafka")
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<Jar>("jar") {
    enabled = true
}
