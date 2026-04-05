dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.kafka:spring-kafka")

    // AOP (DistributedLock 등)
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Redisson (분산 락)
    implementation("org.redisson:redisson-spring-boot-starter:3.25.2")
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<Jar>("jar") {
    enabled = true
}
