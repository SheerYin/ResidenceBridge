import com.github.jengelman.gradle.plugins.shadow.transformers.ResourceTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import org.apache.tools.zip.ZipEntry
import org.apache.tools.zip.ZipOutputStream
import xyz.jpenilla.resourcefactory.bukkit.Permission
import java.text.SimpleDateFormat
import java.util.*

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.shadow)

    alias(libs.plugins.resource.factory.bukkit.convention)
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenLocal()
    mavenCentral()

    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")

    // maven("https://libraries.minecraft.net/")
    // maven("https://repo.codemc.io/repository/nms/")

    maven("https://repo.panda-lang.org/releases")

    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.21.10-R0.1-SNAPSHOT")
    // compileOnly("org.spigotmc:spigot:1.21.10-R0.1-SNAPSHOT")

    implementation(libs.kyori.adventure.api)
    implementation(libs.kyori.adventure.text.minimessage)
    implementation(libs.kyori.adventure.platform.bukkit)
    
    implementation(libs.configurate.yaml)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.hikaricp)

    compileOnly(libs.placeholderapi)

    // compileOnly(files("${project.projectDir}/libraries/CMILib.jar"))
    compileOnly(files("${project.projectDir}/libraries/Residence.jar"))
}

group = "me.yin"
version = "1.0.0"

val minecraftPluginName = property("pluginBaseName") as String
val minecraftPluginLowercaseName = minecraftPluginName.lowercase()
val minecraftPluginMain = "$group.$minecraftPluginName.$minecraftPluginName"
val minecraftPluginVersion = SimpleDateFormat("yyyy.MM.dd").format(Date()) + "-SNAPSHOT"
val minecraftPluginAuthors = listOf("尹")

bukkitPluginYaml {
    apiVersion = "1.16"
    name = minecraftPluginName
    version = minecraftPluginVersion
    main = minecraftPluginMain
    authors = minecraftPluginAuthors
    softDepend.addAll("Residence", "PlaceholderAPI")
    prefix = "领地桥接"

    val result = configurations.runtimeClasspath.get().incoming.resolutionResult
    // 遍历根节点的依赖 (相当于 old: firstLevelModuleDependencies)
    result.root.dependencies.forEach { dependency ->
        // 1. 确保依赖解析成功
        if (dependency is ResolvedDependencyResult) {
            // 2. 获取被选中的组件 ID
            val id = dependency.selected.id
            // 3. 过滤类型：通常你只想要外部库 (Maven/Ivy)，而不是子项目 (Project)
            if (id is ModuleComponentIdentifier) {
                // 这里对应 old: "${it.moduleGroup}:${it.moduleName}:${it.moduleVersion}"
                libraries.add("${id.group}:${id.module}:${id.version}")
            }
        }
    }

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
    }
}

tasks.jar {
    archiveFileName.set("$minecraftPluginName-${project.name}.jar")
}


class PluginYmlLibrariesRemover : ResourceTransformer {
    private var transformedContent: String? = null

    override fun canTransformResource(element: FileTreeElement): Boolean {
        return element.relativePath.pathString == "plugin.yml"
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

    archiveFileName.set("$minecraftPluginName-${project.name}-shadow.jar")
    transform(PluginYmlLibrariesRemover())

    relocate("kotlin.", "${project.group}.relocate.kotlin")
    relocate("kotlinx.serialization", "${project.group}.relocate.kotlinx.serialization")
    relocate("kotlinx.coroutines", "${project.group}.relocate.kotlinx.coroutines")

    relocate("org.jetbrains", "${project.group}.relocate.org.jetbrains")
    relocate("org.intellij", "${project.group}.relocate.org.intellij")
    // relocate("org.slf4j", "${project.group}.relocate.org.slf4j")
    relocate("org.jspecify", "${project.group}.relocate.org.jspecify")
    relocate("org.spongepowered", "${project.group}.relocate.org.spongepowered")

    relocate("net.kyori", "${project.group}.relocate.net.kyori")

    relocate("com.zaxxer.hikari", "${project.group}.relocate.com.zaxxer.hikaricp")
    // relocate("com.google", "${project.group}.relocate.com.google")
    relocate("com.github", "${project.group}.relocate.com.github")

    relocate("io.leangen", "${project.group}.relocate.io.leangen")

    relocate("dev.rollczi", "${project.group}.relocate.dev.rollczi")
}


//val generateShadowPluginYml by tasks.register("generateShadowPluginYml") {
//    group = "build"
//    description = "Generates a plugin.yml without a libraries section for the shadow jar."
//
//    // 任务的输入：由 bukkitPluginYaml 插件生成的原始 plugin.yml
//    // 我们假设它在 processResources 任务执行后会出现在这个标准路径
//    val standardPluginYml = layout.buildDirectory.file("resources/main/plugin.yml")
//    inputs.file(standardPluginYml)
//
//    // 任务的输出：我们加工后的新文件
//    val shadowPluginYml = layout.buildDirectory.file("generated/plugin/plugin-shadow.yml")
//    outputs.file(shadowPluginYml)
//
//    // 确保这个任务在原始 plugin.yml 生成之后再运行
//    dependsOn(tasks.named("processResources"))
//
//    doLast {
//        val standardPluginYmlFile = standardPluginYml.get().asFile
//        val shadowPluginYmlFile = shadowPluginYml.get().asFile
//
//        val filteredLines = mutableListOf<String>()
//        var inLibrariesSection = false // 用一个状态标记我们是否正处于 libraries 区块
//
//        standardPluginYmlFile.forEachLine { line ->
//            // 当遇到 "libraries:" 这一行时，我们开始进入“跳过模式”
//            if (line.trim().startsWith("libraries:")) {
//                inLibrariesSection = true
//                return@forEachLine // continue, 跳过 "libraries:" 这一行本身
//            }
//
//            if (inLibrariesSection) {
//                // 如果在“跳过模式”中...
//                if (line.startsWith(" ") || line.startsWith("-")) {
//                    // 如果这一行是 libraries 下的列表项（有缩进或以'-'开头），则跳过它
//                    return@forEachLine // continue
//                } else {
//                    // 如果遇到一个没有缩进的新字段（比如 "commands:"），说明 libraries 区块结束了
//                    inLibrariesSection = false
//                }
//            }
//
//            // 只有不在“跳过模式”时，才将当前行加入到结果中
//            filteredLines.add(line)
//        }
//
//        // 将过滤后的内容写入新文件
//        shadowPluginYmlFile.apply {
//            parentFile.mkdirs()
//            writeText(filteredLines.joinToString(System.lineSeparator())) // 使用系统换行符更标准
//        }
//    }
//}
//
//tasks.jar {
//    archiveFileName.set("${project.name}.jar")
//}
//
//tasks.shadowJar {
//    mergeServiceFiles()
//    exclude("plugin.yml")
//
//    from(generateShadowPluginYml) {
//        rename { "plugin.yml" }
//    }
//
//    archiveFileName.set("${project.name}-shadow.jar")
//
//    relocate("kotlin", "${project.group}.relocate.kotlin")
//    relocate("kotlinx.coroutines", "${project.group}.relocate.kotlinx.coroutines")
//
//    relocate("org.jetbrains", "${project.group}.relocate.org.jetbrains")
//    relocate("org.intellij", "${project.group}.relocate.org.intellij")
//
//    relocate("com.zaxxer.hikari", "${project.group}.relocate.com.zaxxer.hikaricp")
//    relocate("org.slf4j", "${project.group}.relocate.org.slf4j")
//
//    relocate("org.jspecify", "${project.group}.relocate.org.jspecify")
//    relocate("com.google", "${project.group}.relocate.com.google")
//    relocate("com.github", "${project.group}.relocate.com.github")
//}

//kotlin {
//    jvmToolchain(21)
//}