package me.yin.residencebridge.configuration

import me.yin.residencebridge.ResidenceBridge
import org.bukkit.configuration.file.YamlConfiguration
import java.nio.file.Files
import java.nio.file.Path

object ConfigurationYAML {

    lateinit var path: Path
    fun initialize() {
        val instance = ResidenceBridge.instance
        path = instance.dataFolder.toPath().resolve("config.yml")

        if (Files.notExists(path)) {
            instance.saveResource("config.yml", false)
        }
    }

    lateinit var configuration: YamlConfiguration
    fun load() {
        configuration = YamlConfiguration.loadConfiguration(path.toFile())
    }

    val serverName: String by lazy { configuration.getString("server-name")!! }
}