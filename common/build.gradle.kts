plugins {
    kotlin("kapt")
}
architectury { common("fabric", "forge") }

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
    val fabricLoaderVersion = providers.gradleProperty("fabric_loader_version").get()
    val fabricKotlinVersion = providers.gradleProperty("fabric_kotlin_version").get()
    val mixinextrasVersion = providers.gradleProperty("mixinextras_version").get()
    val clothConfigVersion = providers.gradleProperty("cloth_config_version").get()

    // We depend on fabric loader here to use the fabric @Environment annotations and get the mixin dependencies
    // Do NOT use other classes from fabric loader
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    // Add dependencies on the required Kotlin modules.
    modImplementation("net.fabricmc:fabric-language-kotlin:$fabricKotlinVersion")

    val mixinextras = "io.github.llamalad7:mixinextras-common:$mixinextrasVersion"
    implementation(mixinextras)
    annotationProcessor(mixinextras)

    modCompileOnly("me.shedaniel.cloth:cloth-config-fabric:$clothConfigVersion") {
        exclude(group = "net.fabricmc.fabric-api", module = "fabric-api")
    }
    testImplementation(kotlin("test"))
}

tasks.named("remapJar") {
    enabled = false
}

