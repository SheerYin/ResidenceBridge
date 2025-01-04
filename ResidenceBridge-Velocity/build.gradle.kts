import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.text.SimpleDateFormat
import java.util.*

plugins {
    kotlin("jvm")
    // kotlin("kapt")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

val rootName = rootProject.name
val lowercaseName = rootName.lowercase()
group = "${rootProject.group}.${lowercaseName}"
version = SimpleDateFormat("yyyy.MM.dd").format(Date()) + "-SNAPSHOT"
val pluginAuthor = "尹"

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

tasks.named<ShadowJar>("shadowJar") {
    archiveFileName.set("${project.name}.jar")

    relocate("kotlin", project.group.toString() + ".relocate.kotlin")
    relocate("org", project.group.toString() + ".relocate.org")

//    dependencies {
//        exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
//    }
}

tasks.named<ProcessResources>("processResources") {
    filesMatching("velocity-plugin.json") {
        expand(
            mapOf(
                "lowercaseName" to lowercaseName,
                "rootName" to rootName,
                "group" to project.group.toString(),
                "pluginVersion" to project.version.toString(),
                "pluginAuthor" to pluginAuthor
            )
        )
    }
}

kotlin {
    jvmToolchain(17)
}
