import java.text.SimpleDateFormat
import java.util.*

plugins {
    kotlin("jvm")
}

val rootName = rootProject.name
val lowercaseName = rootName.lowercase()
group = "${rootProject.group}.$lowercaseName"
version = SimpleDateFormat("yyyy.MM.dd").format(Date()) + "-SNAPSHOT"

repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")

    maven("https://libraries.minecraft.net/")
    maven("https://repo.codemc.io/repository/nms/")
}

val minecraftVersion = "1.21"
dependencies {
    compileOnly("net.md-5:bungeecord-api:${minecraftVersion}-R0.1-SNAPSHOT")

    // 如果要修改需要同步 bungee.yml 的 libraries

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
}

val pluginAuthor = "尹"
val pluginLibraries = listOf(
    "org.jetbrains.kotlin:kotlin-stdlib:2.1.0",
    "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1"
)
tasks.named<ProcessResources>("processResources") {
    filesMatching("bungee.yml") {
        expand(
            mapOf(
                "rootName" to rootName,
                "group" to project.group.toString(),
                "pluginVersion" to project.version.toString(),
                "pluginAuthor" to pluginAuthor,
                "pluginLibraries" to pluginLibraries.joinToString("") { "\n  - \"$it\"" }
            )
        )
    }
}

tasks.jar {
    archiveFileName.set("$rootName-Bungee.jar")
}

