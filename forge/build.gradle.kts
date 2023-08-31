architectury {
    platformSetupLoomIde()
    forge()
}

loom {
    forge {
        mixinConfig("world_tools.mixins.common.json")
    }
}

repositories {
    maven("https://thedarkcolour.github.io/KotlinForForge/") {
        name = "KotlinForForge"
    }
}

val common: Configuration by configurations.creating {
    configurations.compileClasspath.get().extendsFrom(this)
    configurations.runtimeClasspath.get().extendsFrom(this)
    configurations["developmentForge"].extendsFrom(this)
}

dependencies {
    forge("net.minecraftforge:forge:${"1.20.1-47.1.0"}")
    implementation("thedarkcolour:kotlinforforge:4.4.0")
    common(project(":common", configuration = "namedElements")) { isTransitive = false }
    shadowCommon(project(path = ":common", configuration = "transformProductionForge")) { isTransitive = false }
    implementation(shadowCommon("net.kyori:adventure-text-minimessage:4.14.0")!!)
    implementation(shadowCommon("net.kyori:adventure-text-serializer-gson:4.14.0")!!)
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
