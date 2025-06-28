import java.text.SimpleDateFormat
import java.util.*

plugins {
    kotlin("jvm")
    // kotlin("kapt")
    id("com.github.johnrengelman.shadow")
}

group = "me.yin"
version = "1.0.0"

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")

    // kapt("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")

    // implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // implementation("org.apache.maven.resolver:maven-resolver-api:1.9.22")
    // implementation("org.apache.maven.resolver:maven-resolver-supplier:1.9.22")
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

val generateVelocityPluginJson by tasks.register("generateVelocityPluginJson") {
    group = "build"
    description = "Generates the velocity-plugin.json file."

    inputs.property("id", minecraftPluginLowercaseName)
    inputs.property("name", minecraftPluginName)
    inputs.property("version", minecraftPluginVersion)
    inputs.property("authors", minecraftPluginAuthors)
    inputs.property("main", "$minecraftPluginGroup.$minecraftPluginName")


    val outputFile = layout.buildDirectory.file("generated/velocity/velocity-plugin.json")
    outputs.file(outputFile)

    doLast {
        val authorsJson = minecraftPluginAuthors.joinToString(", ") { "\"$it\"" }
        val content = """
            {
              "id": "$minecraftPluginLowercaseName",
              "name": "$minecraftPluginName",
              "version": "$minecraftPluginVersion",
              "authors": [$authorsJson],
              "main": "$minecraftPluginGroup.$minecraftPluginName"
            }
        """.trimIndent()

        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(content)
        }
    }
}

tasks.processResources {
    from(generateVelocityPluginJson)
}

tasks.jar {
    archiveFileName.set("${project.name}.jar")
}

tasks.shadowJar {
    archiveFileName.set("${project.name}-shadow.jar")
    relocate("kotlin", "${project.group}.relocate.kotlin")
    relocate("kotlinx.coroutines", "${project.group}.relocate.kotlinx.coroutines")

    relocate("org.jetbrains", "${project.group}.relocate.org.jetbrains")
    relocate("org.intellij", "${project.group}.relocate.org.intellij")
}

kotlin {
    jvmToolchain(17)
}