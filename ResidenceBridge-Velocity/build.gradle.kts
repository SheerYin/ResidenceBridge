import java.text.SimpleDateFormat
import java.util.*

plugins {
    kotlin("jvm")
    // kotlin("kapt")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "me.yin"
version = "1.0.0"


repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    // kapt("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")

    // implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

//    implementation("org.apache.maven.resolver:maven-resolver-api:1.9.22")
//    implementation("org.apache.maven.resolver:maven-resolver-supplier:1.9.22")
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
            val targetFile = layout.buildDirectory.file("resources/main/velocity-plugin.json").get().asFile

            val author = minecraftPluginAuthors.joinToString(",") { "\"$it\"" }
            val text = """{
  "id": "$minecraftPluginLowercaseName",
  "name": "$minecraftPluginName",
  "version": "$minecraftPluginVersion",
  "authors": [$author],
  "main": "$minecraftPluginGroup.$minecraftPluginName"
}"""
            targetFile.writeText(text)
        }
    }
    jar {
        archiveFileName.set("${project.name}.jar")
    }
    shadowJar {
        archiveFileName.set("${project.name}-shadow.jar")
        relocate("kotlin", "${project.group}.relocate.kotlin")
        relocate("org", "${project.group}.relocate.org")
    }
}

kotlin {
    jvmToolchain(17)
}
