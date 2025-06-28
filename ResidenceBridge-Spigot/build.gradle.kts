import xyz.jpenilla.resourcefactory.bukkit.Permission
import java.text.SimpleDateFormat
import java.util.*

plugins {
    kotlin("jvm")
    id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.3.0"
    id("com.github.johnrengelman.shadow")
}

group = "me.yin"
version = "1.0.0"

repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")

    // maven("https://libraries.minecraft.net/")
    // maven("https://repo.codemc.io/repository/nms/")

    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.21.4-R0.1-SNAPSHOT")
    // compileOnly("org.spigotmc:spigot:${minecraftVersion}-R0.1-SNAPSHOT")

    // 如果要修改需要同步 plugin.yml 的 libraries

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("com.zaxxer:HikariCP:6.3.0")

    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly(files("${project.projectDir}/libraries/Residence.jar"))
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
val minecraftPluginLibraries = listOf(
    "org.jetbrains.kotlin:kotlin-stdlib:2.1.10",
    "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2",
    "com.zaxxer:HikariCP:6.3.0"
)

bukkitPluginYaml {
    apiVersion = "1.16"
    name = minecraftPluginName
    version = minecraftPluginVersion
    main = "${minecraftPluginGroup}.${minecraftPluginName}"
    authors.add("尹")
    softDepend.addAll("Residence", "PlaceholderAPI")
    prefix = "领地桥接"
    libraries = minecraftPluginLibraries

    val label = "${minecraftPluginLowercaseName}.command"
    commands {
        register(minecraftPluginLowercaseName) {
            aliases = listOf(minecraftPluginLowercaseName, "rb")
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

val generateShadowPluginYml by tasks.register("generateShadowPluginYml") {
    group = "build"
    description = "Generates a plugin.yml without a libraries section for the shadow jar."

    // 任务的输入：由 bukkitPluginYaml 插件生成的原始 plugin.yml
    // 我们假设它在 processResources 任务执行后会出现在这个标准路径
    val standardPluginYml = layout.buildDirectory.file("resources/main/plugin.yml")
    inputs.file(standardPluginYml)

    // 任务的输出：我们加工后的新文件
    val shadowPluginYml = layout.buildDirectory.file("generated/plugin/plugin-shadow.yml")
    outputs.file(shadowPluginYml)

    // 确保这个任务在原始 plugin.yml 生成之后再运行
    dependsOn(tasks.named("processResources"))

    doLast {
        val standardPluginYmlFile = standardPluginYml.get().asFile
        val shadowPluginYmlFile = shadowPluginYml.get().asFile

        val filteredLines = mutableListOf<String>()
        var inLibrariesSection = false // 用一个状态标记我们是否正处于 libraries 区块

        standardPluginYmlFile.forEachLine { line ->
            // 当遇到 "libraries:" 这一行时，我们开始进入“跳过模式”
            if (line.trim().startsWith("libraries:")) {
                inLibrariesSection = true
                return@forEachLine // continue, 跳过 "libraries:" 这一行本身
            }

            if (inLibrariesSection) {
                // 如果在“跳过模式”中...
                if (line.startsWith(" ") || line.startsWith("-")) {
                    // 如果这一行是 libraries 下的列表项（有缩进或以'-'开头），则跳过它
                    return@forEachLine // continue
                } else {
                    // 如果遇到一个没有缩进的新字段（比如 "commands:"），说明 libraries 区块结束了
                    inLibrariesSection = false
                }
            }

            // 只有不在“跳过模式”时，才将当前行加入到结果中
            filteredLines.add(line)
        }

        // 将过滤后的内容写入新文件
        shadowPluginYmlFile.apply {
            parentFile.mkdirs()
            writeText(filteredLines.joinToString(System.lineSeparator())) // 使用系统换行符更标准
        }
    }
}

tasks.jar {
    archiveFileName.set("${project.name}.jar")
}

tasks.shadowJar {
    exclude("plugin.yml")

    from(generateShadowPluginYml) {
        rename { "plugin.yml" }
    }

    archiveFileName.set("${project.name}-shadow.jar")

    relocate("kotlin", "${project.group}.relocate.kotlin")
    relocate("kotlinx.coroutines", "${project.group}.relocate.kotlinx.coroutines")

    relocate("org.jetbrains", "${project.group}.relocate.org.jetbrains")
    relocate("org.intellij", "${project.group}.relocate.org.intellij")

    relocate("com.zaxxer.hikari", "${project.group}.relocate.com.zaxxer.hikaricp")
    relocate("org.slf4j", "${project.group}.relocate.org.slf4j")
}

kotlin {
    jvmToolchain(17)
}
