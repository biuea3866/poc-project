// Batch 모듈 - 배치 작업
tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    mainClass.set("com.openmarket.batch.BatchApplicationKt")
}
