import java.text.SimpleDateFormat
import java.util.*

plugins {
    kotlin("jvm")
}

group = "me.yin"
version = "1.0.0"

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

var minecraftPluginName: String
var minecraftPluginLowercaseName: String
var minecraftPluginGroup: String
if (project == rootProject) {
    minecraftPluginName = project.name
    minecraftPluginLowercaseName = minecraftPluginName.lowercase()
    minecraftPluginGroup = "${project.group}.$minecraftPluginLowercaseName"
} else {
    minecraftPluginName = rootProject.name
    minecraftPluginLowercaseName = minecraftPluginName.lowercase()
    minecraftPluginGroup = "${rootProject.group}.$minecraftPluginLowercaseName"
}
val minecraftPluginVersion: String = SimpleDateFormat("yyyy.MM.dd").format(Date()) + "-SNAPSHOT"
val minecraftPluginAuthors = listOf("尹")
val minecraftPluginLibraries = listOf(
    "org.jetbrains.kotlin:kotlin-stdlib:2.1.10",
    "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1"
)

tasks {
    processResources {
        doLast {
            val targetFile = layout.buildDirectory.file("resources/main/bungee.yml").get().asFile

            val text = """name: $minecraftPluginName
main: $minecraftPluginGroup.$minecraftPluginLowercaseName
version: "$minecraftPluginVersion"
author: ${minecraftPluginAuthors.joinToString("") { "\n  - $it" }}
libraries: ${minecraftPluginLibraries.joinToString("") { "\n  - $it" }}"""
            targetFile.writeText(text)
        }
    }
    jar {
        archiveFileName.set("${project.name}.jar")
    }
}

