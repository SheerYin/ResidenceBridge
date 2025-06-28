import java.text.SimpleDateFormat
import java.util.*

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

group = "me.yin"
version = "1.0.0"

repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")

    maven("https://libraries.minecraft.net/")
    maven("https://repo.codemc.io/repository/nms/")
}

dependencies {
    compileOnly("net.md-5:bungeecord-api:1.21-R0.3-SNAPSHOT")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
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
    "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2"
)


val generateStandardBungeeYml by tasks.register("generateStandardBungeeYml") {
    group = "build"
    description = "Generates the standard bungee.yml with a 'libraries' section."

    inputs.property("name", minecraftPluginName)
    inputs.property("group", minecraftPluginGroup)
    inputs.property("version", minecraftPluginVersion)
    inputs.property("authors", minecraftPluginAuthors)
    inputs.property("libraries", minecraftPluginLibraries) // 使用自动生成的列表

    val outputFile = layout.buildDirectory.file("generated/bungee/bungee.yml")
    outputs.file(outputFile)

    doLast {
        val content = buildString {
            appendLine("name: $minecraftPluginName")
            appendLine("main: $minecraftPluginGroup.$minecraftPluginName")
            appendLine("version: \"$minecraftPluginVersion\"")
            val size = minecraftPluginAuthors.size
            if (size == 1) {
                appendLine("author: ${minecraftPluginAuthors.first()}")
            } else if (size > 1) {
                appendLine("authors:")
                minecraftPluginAuthors.forEach { appendLine("  - \"$it\"") }
            }
            appendLine("libraries:")
            minecraftPluginLibraries.forEach { appendLine("  - \"$it\"") }
        }

        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(content)
        }
    }
}

val generateShadowBungeeYml by tasks.register("generateShadowBungeeYml") {
    group = "build"
    description = "Generates the bungee.yml for the shadow jar (without 'libraries')."

    inputs.property("name", minecraftPluginName)
    inputs.property("group", minecraftPluginGroup)
    inputs.property("version", minecraftPluginVersion)
    inputs.property("authors", minecraftPluginAuthors)

    val outputFile = layout.buildDirectory.file("generated/bungee/bungee-shadow.yml")
    outputs.file(outputFile)

    doLast {
        val content = buildString {
            appendLine("name: $minecraftPluginName")
            appendLine("main: $minecraftPluginGroup.$minecraftPluginName")
            appendLine("version: \"$minecraftPluginVersion\"")
            val size = minecraftPluginAuthors.size
            if (size == 1) {
                appendLine("author: ${minecraftPluginAuthors.first()}")
            } else if (size > 1) {
                appendLine("authors:")
                minecraftPluginAuthors.forEach { appendLine("  - \"$it\"") }
            }
        }
        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(content)
        }
    }
}

tasks.processResources {
    from(generateStandardBungeeYml)
}

tasks.jar {
    archiveFileName.set("${project.name}.jar")
}

tasks.shadowJar {
    exclude("bungee.yml")

    from(generateShadowBungeeYml) {
        rename { "bungee.yml" }
    }

    archiveFileName.set("${project.name}-shadow.jar")
    relocate("kotlin", "${project.group}.relocate.kotlin")
    relocate("kotlinx.coroutines", "${project.group}.relocate.kotlinx.coroutines")

    relocate("org.jetbrains", "${project.group}.relocate.org.jetbrains")
    relocate("org.intellij", "${project.group}.relocate.org.intellij")
}

kotlin {
    jvmToolchain(17)
}