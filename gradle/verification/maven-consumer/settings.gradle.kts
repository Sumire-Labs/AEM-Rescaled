pluginManagement {
    repositories {
        maven("https://maven.cleanroommc.com")
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("com.cleanroommc.cleanroomgradle.settings") version "0.23.0"
}

rootProject.name = "aem-publication-consumer"
