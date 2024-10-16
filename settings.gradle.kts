enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "adastra"

pluginManagement {
    repositories {
        maven(url = "https://maven.architectury.dev/")
        maven(url = "https://maven.neoforged.net/releases/")
        maven(url = "https://kneelawk.com/maven")
        maven(url = "https://maven.teamresourceful.com/repository/maven-public/")
        gradlePluginPortal()
    }
}

include("common")
include("fabric")
include("neoforge")
