dependencies {
    implementation(project(":closet-common"))

    // Spring Boot Web
    implementation("org.springframework.boot:spring-boot-starter-web")

    // JPA
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // MySQL
    runtimeOnly("com.mysql:mysql-connector-j")
}
