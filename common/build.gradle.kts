plugins {
    kotlin("jvm") version "1.9.10"
}

architectury { common("fabric", "forge") }

loom {  }

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
    modImplementation("net.fabricmc:fabric-loader:${"0.14.21"}")
    // Add dependencies on the required Kotlin modules.
//    implementation(kotlin("stdlib"))
//    implementation(kotlin("reflect"))
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.7.3")
    modImplementation("net.fabricmc:fabric-language-kotlin:1.10.10+kotlin.1.9.10")
    implementation(include("net.kyori:adventure-text-minimessage:4.14.0")!!)
    implementation(include("net.kyori:adventure-text-serializer-gson:4.14.0")!!)
}

kotlin {
    jvmToolchain(17)
}

tasks.withType(JavaCompile::class.java) {
    options.encoding = "UTF-8"
    options.release = 17
}

tasks.named("remapJar") {
    enabled = false
}

