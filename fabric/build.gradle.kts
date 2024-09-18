architectury {
    platformSetupLoomIde()
    fabric()
}

base.archivesName.set("${base.archivesName.get()}-fabric")

loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)
    enableTransitiveAccessWideners.set(true)
    runs {
        getByName("client") {
            ideConfigGenerated(true)
        }
    }
}

val common: Configuration by configurations.creating {
    configurations.compileClasspath.get().extendsFrom(this)
    configurations.runtimeClasspath.get().extendsFrom(this)
    configurations["developmentFabric"].extendsFrom(this)
}

dependencies {
    common(project(":common", configuration = "namedElements")) { isTransitive = false }
    shadowCommon(project(path = ":common", configuration = "transformProductionFabric")) { isTransitive = false }
    modImplementation("net.fabricmc:fabric-loader:${project.properties["fabric_loader_version"]!!}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.properties["fabric_api_version"]!!}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${project.properties["fabric_kotlin_version"]!!}")
    modApi("me.shedaniel.cloth:cloth-config-fabric:${project.properties["cloth_config_version"]}")
    modApi("com.terraformersmc:modmenu:${project.properties["mod_menu_version"]}")
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
