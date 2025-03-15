plugins {
    kotlin("jvm") version "2.1.10"
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

tasks {
    jar {
        enabled = false
    }
    assemble {
        enabled = false
    }
    build {
        enabled = false
    }
}