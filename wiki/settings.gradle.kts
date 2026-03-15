rootProject.name = "wiki"

include(
    ":wiki-domain",
    ":wiki-api",
    ":wiki-worker",
)
project(":wiki-domain").projectDir = file("wiki-domain")
project(":wiki-api").projectDir = file("wiki-api")
project(":wiki-worker").projectDir = file("wiki-worker")
