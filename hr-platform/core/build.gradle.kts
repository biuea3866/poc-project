plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    api(libs.bundles.jackson)

    // JPA 어노테이션 (@Transient · @MappedSuperclass 등) 표준 API만 compileOnly.
    // Hibernate 등 구현체는 의존하지 않음 (ADR-001 §1 "core는 Spring 직접 의존 0").
    // 도메인 서비스가 Entity를 JPA로 영속화할 때 필요한 어노테이션을 핵심 모듈에서 인지하도록 둠.
    compileOnly(libs.jakarta.persistence.api)

    // Spring Data Auditing 어노테이션 (@CreatedBy/@LastModifiedBy 등) + AuditingEntityListener compileOnly.
    // 구현체 활성화(@EnableJpaAuditing)는 각 service 모듈의 @SpringBootApplication에서 선언.
    compileOnly(libs.spring.data.jpa)
}
