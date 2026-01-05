// API 모듈 - 의존성은 루트 build.gradle.kts에서 관리
tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    mainClass.set("com.openmarket.api.OpenMarketApplicationKt")
}
