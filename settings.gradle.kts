rootProject.name = "WorldTools"
pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        maven("https://maven.architectury.dev/")
        maven("https://maven.minecraftforge.net/")
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        kotlin("kapt") version "2.1.0"
    }
}

include("common")
include("fabric")
//include("forge")
