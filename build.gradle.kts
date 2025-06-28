plugins {
    kotlin("jvm") version "2.2.0-RC3" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
}

group = "me.yin"
version = "1.0.0"

repositories {
    mavenLocal()
    mavenCentral()
}

subprojects {
    repositories {
        mavenLocal()
        mavenCentral()

        // layout.buildDirectory.set(file("${rootProject.projectDir}/build/${project.name}"))
    }
}