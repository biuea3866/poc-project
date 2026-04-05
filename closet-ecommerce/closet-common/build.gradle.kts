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
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<Jar>("jar") {
    enabled = true
}
