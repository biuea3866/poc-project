plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.jpa)
    kotlin("kapt")
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

    // QueryDSL Q 클래스 생성 — @MappedSuperclass BaseEntity의 Q 클래스를 생성해
    // 하위 모듈 Entity가 QBaseEntity._super 를 참조할 수 있도록 함.
    compileOnly("${rootProject.libs.querydsl.jpa.get()}:jakarta")
    kapt("${rootProject.libs.querydsl.apt.get()}:jakarta")
    kapt("jakarta.annotation:jakarta.annotation-api:3.0.0")
    kapt("jakarta.persistence:jakarta.persistence-api:3.1.0")
}
