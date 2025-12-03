package me.yin.residencebridge

import com.bekvon.bukkit.residence.Residence
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import me.yin.residencebridge.command.DynamicTabExecutor
import me.yin.residencebridge.configuration.MainConfiguration
import me.yin.residencebridge.configuration.MessageConfiguration
import me.yin.residencebridge.listener.AsyncPlayerJoin
import me.yin.residencebridge.listener.ReceivePluginMessage
import me.yin.residencebridge.listener.residence.*
import me.yin.residencebridge.message.SimpleMessage
import me.yin.residencebridge.other.*
import me.yin.residencebridge.placeholder.ResidenceBridgeExpansion
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.plugin.java.JavaPlugin

class ResidenceBridge : JavaPlugin() {

    val pluginName = description.name
    val pluginNameLowercase = pluginName.lowercase()
    val pluginVersion = description.version
    val pluginAuthors = description.authors

    val pluginChannel = "${pluginNameLowercase}:channel"

    val mainJob = SupervisorJob()
    val scope = CoroutineScope(mainJob + CoroutineName(pluginName))


    lateinit var mainConfiguration: MainConfiguration
        private set

    lateinit var messageConfiguration: MessageConfiguration
    private set

    lateinit var databaseManager: DatabaseManager
        private set


    override fun onEnable() {
        logger.info("插件开始加载 $pluginVersion")

        mainConfiguration = MainConfiguration(this)
        messageConfiguration = MessageConfiguration(this)
        val simpleMessage = SimpleMessage(BukkitAudiences.create(this))

        databaseManager = DatabaseManager(mainConfiguration)

        val json = Json {
            serializersModule = SerializersModule {
                contextual(UUIDSerializer())
            }
        }

        val allRepository = AllRepository(mainConfiguration, json)
        // 阻塞
        databaseManager.dataSource.connection.use { connection ->
            allRepository.initializePlayerTable(connection)
            allRepository.initializeResidenceTable(connection)
        }

        val bukkitDispatcher = BukkitDispatcher(this)
        val allCache = AllCache(databaseManager, allRepository, scope, bukkitDispatcher)



        val pluginManager = server.pluginManager
        pluginManager.registerEvents(AsyncPlayerJoin(databaseManager, allRepository), this)

        var residenceInstance: Residence?
        val residencePlugin = pluginManager.getPlugin("Residence")
        if (residencePlugin != null) {
            logger.info("挂钩 Residence")
            residenceInstance = residencePlugin as Residence
        } else {
            logger.warning("找不到 Residence 相关逻辑取消")
            residenceInstance = null
        }

        val placeholderAPIPlugin = pluginManager.getPlugin("PlaceholderAPI")
        if (placeholderAPIPlugin != null) {
            logger.info("挂钩 PlaceholderAPI")
            ResidenceBridgeExpansion(this, pluginNameLowercase, pluginAuthors, pluginVersion, residenceInstance, allCache).register()
        } else {
            logger.warning("找不到 PlaceholderAPI 无法注册 PlaceholderAPI 变量")
        }

        val residenceTeleport = ResidenceTeleport(this, pluginChannel)
        val executor = DynamicTabExecutor(
            this,
            residenceInstance,
            databaseManager,
            allRepository,
            scope,
            allCache,
            residenceTeleport,
            simpleMessage,
            mainConfiguration,
            messageConfiguration
        )
        getCommand(pluginNameLowercase)?.setExecutor(executor)

        if (residenceInstance != null) {
            pluginManager.registerEvents(ResidenceCommand(residenceInstance, databaseManager, allRepository, simpleMessage, messageConfiguration, executor), this)
            pluginManager.registerEvents(ResidenceCreation(this, mainConfiguration, databaseManager, allRepository, scope, allCache), this)
            pluginManager.registerEvents(ResidenceDelete(databaseManager, allRepository, scope, allCache), this)
            pluginManager.registerEvents(ResidenceFlagChange(this, residenceInstance, mainConfiguration, databaseManager, allRepository, scope, allCache), this)
            pluginManager.registerEvents(ResidenceOwnerChange(databaseManager, allRepository, scope, allCache), this)
            pluginManager.registerEvents(ResidenceRename(databaseManager, allRepository, scope, allCache, simpleMessage, messageConfiguration), this)
        }

        server.messenger.registerOutgoingPluginChannel(this, pluginChannel)
        server.messenger.registerIncomingPluginChannel(this, pluginChannel, ReceivePluginMessage(this, residenceInstance, pluginChannel))
    }

    override fun onDisable() {
        logger.info("插件开始卸载 $pluginVersion")
    }


}