import org.gradle.api.artifacts.Configuration

architectury {
    platformSetupLoomIde()
    fabric()
}

base.archivesName.set("${base.archivesName.get()}-fabric")

loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)
    enableTransitiveAccessWideners.set(true)
//    runs {
//        getByName("client") {
//            ideConfigGenerated(true)
//        }
//    }
}

val common: Configuration = configurations.create("common") {
    configurations.compileClasspath.get().extendsFrom(this)
    configurations.runtimeClasspath.get().extendsFrom(this)
    configurations["developmentFabric"].extendsFrom(this)
}

dependencies {
    val fabricLoaderVersion = providers.gradleProperty("fabric_loader_version").get()
    val fabricApiVersion = providers.gradleProperty("fabric_api_version").get()
    val fabricKotlinVersion = providers.gradleProperty("fabric_kotlin_version").get()
    val clothConfigVersion = providers.gradleProperty("cloth_config_version").get()
    val modMenuVersion = providers.gradleProperty("mod_menu_version").get()

    common(project(":common", configuration = "namedElements")) { isTransitive = false }
    shadowCommon(project(path = ":common", configuration = "transformProductionFabric")) { isTransitive = false }
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    modImplementation("net.fabricmc:fabric-language-kotlin:$fabricKotlinVersion")
    modApi("me.shedaniel.cloth:cloth-config-fabric:$clothConfigVersion")
    modApi("com.terraformersmc:modmenu:$modMenuVersion")
}

tasks {
    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") {
            expand(getProperties())
            expand(mutableMapOf("version" to project.version))
        }
    }

    remapJar {
        injectAccessWidener.set(true)
    }
}
