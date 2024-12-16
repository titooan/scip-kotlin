rootProject.name = "lsif-kotlin"

include(
    "semanticdb-kotlin",
    "semanticdb-kotlinc",
    "semanticdb-kotlinc:minimized",
    // TODO: fix this in a better way than just removing `debug-project` from the include list
//    "debug-project"
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
