import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask

plugins {
    kotlin("jvm") version ("2.1.0")
    id("architectury-plugin") version "3.4-SNAPSHOT"
    id("dev.architectury.loom") version "1.13-SNAPSHOT" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
}

architectury {
    minecraft = project.properties["minecraft_version"]!! as String
}

subprojects {
    apply(plugin = "dev.architectury.loom")
    dependencies {
        "minecraft"("com.mojang:minecraft:${project.properties["minecraft_version"]!!}")
        "mappings"("net.fabricmc:yarn:${project.properties["yarn_mappings"]}:v2")
    }
    if (path != ":common") {
        apply(plugin = "com.github.johnrengelman.shadow")

        val shadowCommon by configurations.creating {
            isCanBeConsumed = false
            isCanBeResolved = true
        }
        val versionWithMCVersion = "${project.properties["mod_version"]!!}+${project.properties["minecraft_version"]!!}"

        tasks.withType<JavaCompile> {
            options.encoding = "UTF-8"
            options.release = 21
        }

        tasks {
            val shadowJarTask = named("shadowJar", ShadowJar::class)
            shadowJarTask {
                archiveVersion = versionWithMCVersion
                archiveClassifier.set("shadow")
                configurations = listOf(shadowCommon)
            }

            "remapJar"(RemapJarTask::class) {
                dependsOn(shadowJarTask)
                inputFile = shadowJarTask.flatMap { it.archiveFile }
                archiveVersion = versionWithMCVersion
                archiveClassifier = ""
            }
            jar {
                enabled = false
            }
        }
    }
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "architectury-plugin")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    base.archivesName.set(project.properties["archives_base_name"]!! as String)
    group = project.properties["maven_group"]!!
    version = project.properties["mod_version"]!!

    repositories {
        maven("https://api.modrinth.com/maven")
        maven("https://jitpack.io")
        maven("https://server.bbkr.space/artifactory/libs-release") {
            name = "CottonMC"
        }
        maven("https://maven.shedaniel.me/")
        maven("https://maven.terraformersmc.com/releases/")
    }

    tasks {
        compileKotlin {
            kotlinOptions.jvmTarget = "21"
        }
    }

    tasks.withType(JavaCompile::class.java) {
        options.encoding = "UTF-8"
        options.release = 21
    }
}
