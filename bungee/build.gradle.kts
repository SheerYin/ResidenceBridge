import com.github.jengelman.gradle.plugins.shadow.transformers.ResourceTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import org.apache.tools.zip.ZipEntry
import org.apache.tools.zip.ZipOutputStream
import java.text.SimpleDateFormat
import java.util.*

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
}

kotlin {
    jvmToolchain(21)
}

group = "me.yin"
version = "1.0.0"

repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")

    maven("https://libraries.minecraft.net/")
    maven("https://repo.codemc.io/repository/nms/")

    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("net.md-5:bungeecord-api:1.21-R0.4-SNAPSHOT")
}

val minecraftPluginName = property("pluginBaseName") as String
val minecraftPluginLowercaseName = minecraftPluginName.lowercase()
val minecraftPluginMain = "$group.$minecraftPluginLowercaseName.$minecraftPluginName"
val minecraftPluginVersion = SimpleDateFormat("yyyy.MM.dd").format(Date()) + "-SNAPSHOT"
val minecraftPluginAuthors = listOf("尹")
val minecraftPluginJarName = minecraftPluginName + "-" + project.name

val generateBungeeYml by tasks.register("generateBungeeYml") {
    group = "build"
    description = "Generates the standard bungee.yml with a 'libraries' section."

    val result = configurations.runtimeClasspath.get().incoming.resolutionResult

//    inputs.property("name", minecraftPluginName)
//    inputs.property("group", minecraftPluginGroup)
//    inputs.property("version", minecraftPluginVersion)
//    inputs.property("authors", minecraftPluginAuthors)
//    inputs.property("libraries", minecraftPluginLibraries) // 使用自动生成的列表

    val outputFile = layout.buildDirectory.file("generated/bungee/bungee.yml")
    outputs.file(outputFile)

    doLast {
        val content = buildString {
            appendLine("name: $minecraftPluginName")
            appendLine("main: $minecraftPluginMain")
            appendLine("version: \"$minecraftPluginVersion\"")
            val size = minecraftPluginAuthors.size
            if (size == 1) {
                appendLine("author: ${minecraftPluginAuthors.first()}")
            } else if (size > 1) {
                appendLine("authors:")
                minecraftPluginAuthors.forEach { appendLine("  - \"$it\"") }
            }
            appendLine("libraries:")
            result.root.dependencies.forEach { dependency ->
                // 1. 确保依赖解析成功
                if (dependency is ResolvedDependencyResult) {
                    // 2. 获取被选中的组件 ID
                    val id = dependency.selected.id
                    // 3. 过滤类型：通常你只想要外部库 (Maven/Ivy)，而不是子项目 (Project)
                    if (id is ModuleComponentIdentifier) {
                        // 这里对应 old: "${it.moduleGroup}:${it.moduleName}:${it.moduleVersion}"
                        appendLine("  - ${id.group}:${id.module}:${id.version}")
                    }
                }
            }
        }

        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(content)
        }
    }
}

tasks.processResources {
    from(generateBungeeYml)
}

tasks.jar {
    archiveFileName.set("$minecraftPluginJarName.jar")
}

class PluginYmlLibrariesRemover : ResourceTransformer {
    private var transformedContent: String? = null

    override fun canTransformResource(element: FileTreeElement): Boolean {
        return element.relativePath.pathString == "bungee.yml"
    }

    override fun transform(context: TransformerContext) {
        val originalContent = context.inputStream.bufferedReader().use { it.readText() }

        // --- 核心逻辑：逐行移除 libraries 及其子节点 ---
        val lines = originalContent.lines()
        val result = StringBuilder()
        var skippingLibraries = false

        for (line in lines) {
            val trimmed = line.trim()

            // 1. 发现 libraries: 开头，开始跳过
            if (trimmed.startsWith("libraries:")) {
                skippingLibraries = true
                continue
            }

            if (skippingLibraries) {
                // 2. 如果正在跳过，且当前行是缩进的 (子项)，或者是空行，继续跳过
                // YAML 列表项通常以 "- " 开头，且有缩进
                if (trimmed.isEmpty() || line.startsWith(" ") || line.startsWith("\t") || trimmed.startsWith("-")) {
                    continue
                } else {
                    // 3. 遇到顶格写的新 key (例如 commands:)，停止跳过
                    skippingLibraries = false
                }
            }

            // 保留该行
            result.append(line).append("\n")
        }

        transformedContent = result.toString()
    }

    override fun hasTransformedResource(): Boolean {
        return transformedContent != null
    }

    override fun modifyOutputStream(os: ZipOutputStream, preserveFileTimestamps: Boolean) {
        val content = transformedContent ?: return

        val entry = ZipEntry("plugin.yml")
        entry.time = System.currentTimeMillis()
        os.putNextEntry(entry)
        os.write(content.toByteArray())
        os.closeEntry()
    }

}

tasks.shadowJar {
    mergeServiceFiles()

    archiveFileName.set("$minecraftPluginJarName-shadow.jar")
    transform(PluginYmlLibrariesRemover())

    relocate("kotlin.", "${project.group}.relocate.kotlin.")
    relocate("org.jetbrains.", "${project.group}.relocate.org.jetbrains.")
    relocate("org.intellij.", "${project.group}.relocate.org.intellij.")
}




//var minecraftPluginName: String
//var minecraftPluginLowercaseName: String
//var minecraftPluginGroup: String
//if (project == rootProject) {
//    minecraftPluginName = project.name
//    minecraftPluginLowercaseName = minecraftPluginName.lowercase()
//    minecraftPluginGroup = "${project.group}.$minecraftPluginLowercaseName"
//} else {
//    minecraftPluginName = rootProject.name
//    minecraftPluginLowercaseName = minecraftPluginName.lowercase()
//    minecraftPluginGroup = "${rootProject.group}.$minecraftPluginLowercaseName"
//}
//val minecraftPluginVersion: String = SimpleDateFormat("yyyy.MM.dd").format(Date()) + "-SNAPSHOT"
//val minecraftPluginAuthors = listOf("尹")
//val minecraftPluginLibraries: List<String> = project.configurations.runtimeClasspath.get()
//    .allDependencies
//    .filter { it.group != null && it.version != null } // 过滤掉没有 group 或 version 的特殊依赖（例如项目依赖）
//    .map { dependency -> "${dependency.group}:${dependency.name}:${dependency.version}" } // 将依赖对象格式化为 "group:name:version" 格式的字符串
//    .toList()
//
//
//val generateStandardBungeeYml by tasks.register("generateStandardBungeeYml") {
//    group = "build"
//    description = "Generates the standard bungee.yml with a 'libraries' section."
//
//    inputs.property("name", minecraftPluginName)
//    inputs.property("group", minecraftPluginGroup)
//    inputs.property("version", minecraftPluginVersion)
//    inputs.property("authors", minecraftPluginAuthors)
//    inputs.property("libraries", minecraftPluginLibraries) // 使用自动生成的列表
//
//    val outputFile = layout.buildDirectory.file("generated/bungee/bungee.yml")
//    outputs.file(outputFile)
//
//    doLast {
//        val content = buildString {
//            appendLine("name: $minecraftPluginName")
//            appendLine("main: $minecraftPluginGroup.$minecraftPluginName")
//            appendLine("version: \"$minecraftPluginVersion\"")
//            val size = minecraftPluginAuthors.size
//            if (size == 1) {
//                appendLine("author: ${minecraftPluginAuthors.first()}")
//            } else if (size > 1) {
//                appendLine("authors:")
//                minecraftPluginAuthors.forEach { appendLine("  - \"$it\"") }
//            }
//            appendLine("libraries:")
//            minecraftPluginLibraries.forEach { appendLine("  - \"$it\"") }
//        }
//
//        outputFile.get().asFile.apply {
//            parentFile.mkdirs()
//            writeText(content)
//        }
//    }
//}
//
//val generateShadowBungeeYml by tasks.register("generateShadowBungeeYml") {
//    group = "build"
//    description = "Generates the bungee.yml for the shadow jar (without 'libraries')."
//
//    inputs.property("name", minecraftPluginName)
//    inputs.property("group", minecraftPluginGroup)
//    inputs.property("version", minecraftPluginVersion)
//    inputs.property("authors", minecraftPluginAuthors)
//
//    val outputFile = layout.buildDirectory.file("generated/bungee/bungee-shadow.yml")
//    outputs.file(outputFile)
//
//    doLast {
//        val content = buildString {
//            appendLine("name: $minecraftPluginName")
//            appendLine("main: $minecraftPluginGroup.$minecraftPluginName")
//            appendLine("version: \"$minecraftPluginVersion\"")
//            val size = minecraftPluginAuthors.size
//            if (size == 1) {
//                appendLine("author: ${minecraftPluginAuthors.first()}")
//            } else if (size > 1) {
//                appendLine("authors:")
//                minecraftPluginAuthors.forEach { appendLine("  - \"$it\"") }
//            }
//        }
//        outputFile.get().asFile.apply {
//            parentFile.mkdirs()
//            writeText(content)
//        }
//    }
//}
//
//tasks.processResources {
//    from(generateStandardBungeeYml)
//}
//
//tasks.jar {
//    archiveFileName.set("${project.name}.jar")
//}
//
//tasks.shadowJar {
//    mergeServiceFiles()
//    exclude("bungee.yml")
//
//    from(generateShadowBungeeYml) {
//        rename { "bungee.yml" }
//    }
//
//    archiveFileName.set("${project.name}-shadow.jar")
//    relocate("kotlin", "${project.group}.relocate.kotlin")
//    relocate("kotlinx.coroutines", "${project.group}.relocate.kotlinx.coroutines")
//
//    relocate("org.jetbrains", "${project.group}.relocate.org.jetbrains")
//    relocate("org.intellij", "${project.group}.relocate.org.intellij")
//}
//
//kotlin {
//    jvmToolchain(17)
//}