package me.yin.residencebridge.configuration

import me.yin.residencebridge.ResidenceBridge
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Path

class MainConfiguration(val residenceBridge: ResidenceBridge) {

    val path: Path = residenceBridge.dataFolder.toPath().resolve("config.yml")
    val loader: YamlConfigurationLoader = YamlConfigurationLoader.builder()
        .path(path)
        .nodeStyle(NodeStyle.BLOCK)
        .indent(2)
        .build()

    lateinit var simpleConfiguration: SimpleConfiguration
        private set

    init {
        reload()
    }

    fun reload() {
        val node = loader.load()
        simpleConfiguration = node.get(SimpleConfiguration::class.java) ?: throw IllegalStateException("配置格式错误，无法解析为 SimpleConfiguration")

        node.set(SimpleConfiguration::class.java, simpleConfiguration)
        loader.save(node)
    }

    @ConfigSerializable
    class SimpleConfiguration(
        val url: String = "jdbc:mysql://localhost:3306/database?user=root&password=password",
        val maximumPoolSize: Int = 10,
        val minimumIdle: Int = 10,
        val connectionTimeout: Long = 30000,
        val idleTimeout: Long = 600000,
        val maximumLifetime: Long = 1800000,
        val tablePrefix: String = "residencebridge_",

        val serverName: String = "",
    )
}


