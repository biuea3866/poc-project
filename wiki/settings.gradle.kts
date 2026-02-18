rootProject.name = "wiki"

include(
    ":wiki-domain",
    ":wiki-api",
)
project(":wiki-domain").projectDir = file("wiki-domain")
project(":wiki-api").projectDir = file("wiki-api")
