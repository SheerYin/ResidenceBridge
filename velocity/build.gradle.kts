import java.text.SimpleDateFormat
import java.util.*

plugins {
    alias(libs.plugins.kotlin.jvm)
    // kotlin("kapt")
    alias(libs.plugins.shadow)
}

kotlin {
    jvmToolchain(21)
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

val minecraftPluginName = property("pluginBaseName") as String
val minecraftPluginLowercaseName = minecraftPluginName.lowercase()
val minecraftPluginMain = "$group.$minecraftPluginLowercaseName.$minecraftPluginName"
val minecraftPluginVersion = SimpleDateFormat("yyyy.MM.dd").format(Date()) + "-SNAPSHOT"
val minecraftPluginAuthors = listOf("尹")
val minecraftPluginJarName = minecraftPluginName + "-" + project.name

val generateVelocityPluginJson by tasks.register("generateVelocityPluginJson") {
    group = "build"
    description = "Generates the velocity-plugin.json file."

//    inputs.property("id", minecraftPluginLowercaseName)
//    inputs.property("name", minecraftPluginName)
//    inputs.property("version", minecraftPluginVersion)
//    inputs.property("authors", minecraftPluginAuthors)
//    inputs.property("main", "$minecraftPluginGroup.$minecraftPluginName")


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
              "main": "$minecraftPluginMain"
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
    archiveFileName.set("$minecraftPluginJarName.jar")
}

tasks.shadowJar {
    mergeServiceFiles()

    archiveFileName.set("$minecraftPluginJarName-shadow.jar")

    relocate("kotlin.", "${project.group}.relocate.kotlin.")
    relocate("org.jetbrains.", "${project.group}.relocate.org.jetbrains.")
    relocate("org.intellij.", "${project.group}.relocate.org.intellij.")
}

