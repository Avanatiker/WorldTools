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
}

include("common")
val enabled = providers.gradleProperty("enabled_platforms").orNull ?: "fabric,forge"
val platforms = enabled.split(',').map { it.trim() }.toSet()
if (platforms.contains("fabric")) include("fabric")
if (platforms.contains("forge")) include("forge")
