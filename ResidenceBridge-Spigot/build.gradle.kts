import xyz.jpenilla.resourcefactory.bukkit.Permission
import java.text.SimpleDateFormat
import java.util.*

plugins {
    kotlin("jvm")
    id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.2.0"
}

val rootName = rootProject.name
val lowercaseName = rootName.lowercase()
group = "${rootProject.group}.${lowercaseName}"
version = SimpleDateFormat("yyyy.MM.dd").format(Date()) + "-SNAPSHOT"

repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")

    // maven("https://libraries.minecraft.net/")
    // maven("https://repo.codemc.io/repository/nms/")

    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

val minecraftVersion = "1.21.4"
dependencies {
    compileOnly("org.spigotmc:spigot-api:${minecraftVersion}-R0.1-SNAPSHOT")
    // compileOnly("org.spigotmc:spigot:${minecraftVersion}-R0.1-SNAPSHOT")

    // 如果要修改需要同步 plugin.yml 的 libraries

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("com.zaxxer:HikariCP:6.2.1")

    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly(files("${rootProject.projectDir}/libraries/Residence.jar"))
}

bukkitPluginYaml {
    apiVersion = "1.16"
    name = rootName
    version = project.version.toString()
    main = "${project.group}.${rootName}"
    authors.add("尹")
    depend.add("Residence")
    // softDepend.add("PlaceholderAPI")
    prefix = "领地桥接"
    libraries = listOf(
        "org.jetbrains.kotlin:kotlin-stdlib:2.1.10",
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1",
        "com.zaxxer:HikariCP:6.2.1"
    )

    val label = "${lowercaseName}.command"
    commands {
        register(lowercaseName) {
            aliases = listOf(lowercaseName, "rb")
            permission = label
        }
    }
    permissions {
        register(label) {
            default = Permission.Default.OP
        }
        register("${label}.help") {
            default = Permission.Default.OP
        }
        register("${label}.list") {
            default = Permission.Default.OP
        }
        register("${label}.listall") {
            default = Permission.Default.OP
        }
        register("${label}.teleport") {
            default = Permission.Default.OP
        }
        register("${label}.import") {
            default = Permission.Default.OP
        }
//        register("${label}.reload") {
//            default = Permission.Default.OP
//        }
    }
}

tasks.jar {
    archiveFileName.set("${project.name}.jar")
}

kotlin {
    jvmToolchain(17)
}
