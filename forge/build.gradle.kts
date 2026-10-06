architectury {
    platformSetupLoomIde()
    forge()
}

base.archivesName.set("${base.archivesName.get()}-forge")

loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)
    forge {
        convertAccessWideners = true
        extraAccessWideners.add(loom.accessWidenerPath.get().asFile.name)
        mixinConfig("worldtools.mixins.common.json")
    }
}

repositories {
    maven("https://thedarkcolour.github.io/KotlinForForge/") {
        name = "KotlinForForge"
    }
    maven("https://cursemaven.com") {
        name = "Curse"
    }
}

val common: Configuration = configurations.create("forgeConfig") {
    configurations.compileClasspath.get().extendsFrom(this)
    configurations.runtimeClasspath.get().extendsFrom(this)
    configurations["developmentForge"].extendsFrom(this)
}

dependencies {
    val forgeVersion = providers.gradleProperty("forge_version").get()
    val kotlinForgeVersion = providers.gradleProperty("kotlin_forge_version").get()
    val mixinextrasVersion = providers.gradleProperty("mixinextras_version").get()
    val clothConfigVersion = providers.gradleProperty("cloth_config_version").get()

    forge("net.minecraftforge:forge:$forgeVersion")
    implementation("thedarkcolour:kotlinforforge:$kotlinForgeVersion")
    common(project(":common", configuration = "namedElements")) { isTransitive = false }
    shadowCommon(project(path = ":common", configuration = "transformProductionForge")) { isTransitive = false }
    implementation(annotationProcessor("io.github.llamalad7:mixinextras-common:$mixinextrasVersion")!!)
    implementation(include("io.github.llamalad7:mixinextras-forge:$mixinextrasVersion")!!)
    modApi("me.shedaniel.cloth:cloth-config-forge:$clothConfigVersion")
}

tasks {
    processResources {
        inputs.property("version", project.version)

        filesMatching("META-INF/mods.toml") {
            expand(getProperties())
            expand(mutableMapOf("version" to project.version))
        }
    }
}
