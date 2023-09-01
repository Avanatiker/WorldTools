import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask

plugins {
    kotlin("jvm") version("1.9.10")
    id("architectury-plugin") version "3.4-SNAPSHOT"
    id("dev.architectury.loom") version "1.3-SNAPSHOT" apply false
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

        tasks.withType<JavaCompile> {
            options.encoding = "UTF-8"
            options.release = 17
        }

        tasks {
            "shadowJar"(ShadowJar::class) {
                archiveClassifier.set("dev-shadow")
                configurations = listOf(shadowCommon)
                exclude("org/intellij/**")
                exclude("org/objectweb/**")
                exclude("org/jetbrains/**")
                exclude("com/google/**")
                exclude("net/fabricmc/**")
                exclude("client-fabric-**")
                exclude("CREDITS.txt")
                exclude("fabric-installer**")
                exclude("fabric-lifecycle-events**")
                exclude("fabric-networking**")
                exclude("fabric-rendering**")
                exclude("LICENSE-**")
                exclude("LICENSE_**")
                exclude("assets/fabric-api-base/**")
                exclude("assets/fabricloader/**")
                exclude("META-INF/jars/**")
                exclude("META-INF/maven/**")
                exclude("META-INF/services/net.f")
            }

            "remapJar"(RemapJarTask::class) {
                dependsOn("shadowJar")
                inputFile.set(named<ShadowJar>("shadowJar").flatMap { it.archiveFile })
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
    }

    tasks {
        compileKotlin {
            kotlinOptions.jvmTarget = "17"
        }
    }

    tasks.withType(JavaCompile::class.java) {
        options.encoding = "UTF-8"
        options.release = 17
    }

    java {
        withSourcesJar()
    }
}
