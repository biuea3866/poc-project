plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    api(project(":core"))
    implementation(libs.spring.kafka)
    implementation(libs.bundles.jackson)
}
