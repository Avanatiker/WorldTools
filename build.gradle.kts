import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version ("2.4.20")
    id("architectury-plugin") version "3.5.170"
    id("dev.architectury.loom") version "1.17-SNAPSHOT" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
}

val minecraftVersion = providers.gradleProperty("minecraft_version").get()
val yarnMappings = providers.gradleProperty("yarn_mappings").get()
val modVersion = providers.gradleProperty("mod_version").get()

architectury {
    minecraft = minecraftVersion
}

subprojects {
    plugins.apply("dev.architectury.loom")

    dependencies {
        "minecraft"("com.mojang:minecraft:$minecraftVersion")
        "mappings"("net.fabricmc:yarn:$yarnMappings:v2")
    }

    if (path != ":common") {
        plugins.apply("com.github.johnrengelman.shadow")

        val shadowCommon = configurations.create("shadowCommon") {
            isCanBeConsumed = false
            isCanBeResolved = true
        }

        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.release = 21
        }

        val versionWithMCVersion = "$modVersion+$minecraftVersion"

        tasks {
            val shadowJarTask = named<ShadowJar>("shadowJar") {
                archiveVersion = versionWithMCVersion
                archiveClassifier.set("shadow")
                configurations = listOf(shadowCommon)
            }


            named<RemapJarTask>("remapJar") {
                dependsOn(shadowJarTask)
                inputFile = shadowJarTask.flatMap { it.archiveFile }
                archiveVersion = versionWithMCVersion
                archiveClassifier = ""
            }

            named<Jar>("jar") {
                enabled = false
            }
        }
    }
}

allprojects {
    plugins.apply("java")
    plugins.apply("architectury-plugin")
    plugins.apply("maven-publish")
    plugins.apply("org.jetbrains.kotlin.jvm")

    val archivesBaseName = providers.gradleProperty("archives_base_name").get()
    val mavenGroup = providers.gradleProperty("maven_group").get()
    val modVersion = providers.gradleProperty("mod_version").get()

    base.archivesName.set(archivesBaseName)

    repositories {
        maven("https://api.modrinth.com/maven")
        maven("https://jitpack.io")
        maven("https://maven.shedaniel.me/")
        maven("https://maven.terraformersmc.com/releases/")
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    tasks.withType(JavaCompile::class.java) {
        options.encoding = "UTF-8"
        options.release = 21
    }
}
