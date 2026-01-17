architectury { common("fabric") }

loom {
    accessWidenerPath.set(File("src/main/resources/worldtools.accesswidener"))
}

repositories {
    maven("https://maven.fabricmc.net/") {
        name = "Fabric"
    }
    maven("https://jitpack.io")
    mavenCentral()
    mavenLocal()
}

dependencies {
    // We depend on fabric loader here to use the fabric @Environment annotations and get the mixin dependencies
    // Do NOT use other classes from fabric loader
    modImplementation("net.fabricmc:fabric-loader:${project.properties["fabric_loader_version"]!!}")
    // Add dependencies on the required Kotlin modules.
    modImplementation("net.fabricmc:fabric-language-kotlin:${project.properties["fabric_kotlin_version"]!!}")
    implementation(annotationProcessor("io.github.llamalad7:mixinextras-common:${project.properties["mixinextras_version"]}")!!)
    modCompileOnly("me.shedaniel.cloth:cloth-config-fabric:${project.properties["cloth_config_version"]}") {
        exclude(group = "net.fabricmc.fabric-api", module = "fabric-api")
    }
}

tasks.named("remapJar") {
    enabled = false
}

