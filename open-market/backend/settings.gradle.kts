plugins {
    // Enable automatic JDK toolchain downloads.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "open-market"

include(
    "api",
    "domain",
    "infra",
    "batch"
)
