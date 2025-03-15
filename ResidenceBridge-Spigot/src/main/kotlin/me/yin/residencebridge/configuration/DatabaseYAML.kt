package me.yin.residencebridge.configuration

import me.yin.residencebridge.ResidenceBridge
import org.bukkit.configuration.file.YamlConfiguration
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object DatabaseYAML {

    lateinit var path: Path
    fun initialize() {
        val instance = ResidenceBridge.instance

        val target = ConfigurationYAML.configuration.getString("residence.file.path")!!
        val directory: Path
        if (target.isEmpty()) { // 确定目录路径
            directory = instance.dataFolder.toPath()
        } else {
            if (target.startsWith("plugins")) {
                directory = instance.dataFolder.toPath().parent.resolve(target.substring(8))
            } else {
                directory = Path.of(target)
            }
        }

        if (Files.notExists(directory)) {
            Files.createDirectory(directory)
        }

        path = directory.resolve("database.yml")

        if (Files.notExists(path)) { // 如果文件不存在，复制资源文件
            instance.getResource("database.yml").use { inputStream ->
                Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    lateinit var configuration: YamlConfiguration
    fun load() {
        configuration = YamlConfiguration.loadConfiguration(path.toFile())
    }

}