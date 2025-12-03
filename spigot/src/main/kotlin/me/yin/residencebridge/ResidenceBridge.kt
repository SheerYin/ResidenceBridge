package me.yin.residencebridge

import com.bekvon.bukkit.residence.Residence
import kotlinx.coroutines.*
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
    val mainScope = CoroutineScope(mainJob + Dispatchers.IO + CoroutineName(pluginName))

    lateinit var mainConfiguration: MainConfiguration
        private set

    lateinit var messageConfiguration: MessageConfiguration
        private set

    lateinit var simpleMessage: SimpleMessage
        private set

    lateinit var databaseManager: DatabaseManager
        private set


    override fun onEnable() {
        logger.info("插件开始加载 $pluginVersion")

        val pluginManager = server.pluginManager
        var residenceInstance: Residence?
        val residencePlugin = pluginManager.getPlugin("Residence")
        if (residencePlugin != null) {
            logger.info("挂钩 Residence")
            residenceInstance = residencePlugin as Residence

            val chestCreateResidence = residenceInstance.configManager.isNewPlayerUse
            if (chestCreateResidence) {
                logger.warning("箱子自动创建领地功能和本插件冲突！")
                logger.warning("请修改 Residence/config.yml NewPlayer.Use: true -> false 再重启")
                pluginManager.disablePlugin(this)
                return
            }
        } else {
            logger.warning("找不到 Residence 相关逻辑取消")
            residenceInstance = null
        }

        mainConfiguration = MainConfiguration(this)
        messageConfiguration = MessageConfiguration(this)

        simpleMessage = SimpleMessage(BukkitAudiences.create(this))

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
        val allCache = AllCache(databaseManager, allRepository, mainScope, bukkitDispatcher)

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
            mainScope,
            allCache,
            residenceTeleport,
            simpleMessage,
            mainConfiguration,
            messageConfiguration
        )
        getCommand(pluginNameLowercase)?.setExecutor(executor)

        pluginManager.registerEvents(AsyncPlayerJoin(databaseManager, allRepository), this)
        if (residenceInstance != null) {
            pluginManager.registerEvents(ResidenceCommand(residenceInstance, databaseManager, allRepository, simpleMessage, messageConfiguration, executor), this)
            pluginManager.registerEvents(ResidenceCreation(this, mainConfiguration, databaseManager, allRepository, mainScope, allCache), this)
            pluginManager.registerEvents(ResidenceDelete(databaseManager, allRepository, mainScope, allCache), this)
            pluginManager.registerEvents(ResidenceFlagChange(this, residenceInstance, mainConfiguration, databaseManager, allRepository, mainScope, allCache), this)
            pluginManager.registerEvents(ResidenceOwnerChange(databaseManager, allRepository, mainScope, allCache), this)
            pluginManager.registerEvents(ResidenceRename(databaseManager, allRepository, mainScope, allCache, simpleMessage, messageConfiguration), this)
        }

        server.messenger.registerOutgoingPluginChannel(this, pluginChannel)
        server.messenger.registerIncomingPluginChannel(this, pluginChannel, ReceivePluginMessage(this, residenceInstance, pluginChannel))
    }

    override fun onDisable() {
        logger.info("插件开始卸载 $pluginVersion")

        runBlocking {
            try {
                withTimeout(5000L) {
                    val jobs = mainJob.children.toList()
                    if (jobs.isNotEmpty()) {
                        logger.info("正在等待 ${jobs.size} 个后台任务完成，最多等待 5 秒")
                        jobs.forEach { job ->
                            logger.info("等待协程 ${job[CoroutineName]?.name ?: "未命名"}")
                        }
                        jobs.joinAll()
                        logger.info("任务全部完成")
                    }
                }
            } catch (e: TimeoutCancellationException) {
                logger.warning("等待超时，强制取消所有任务")
            } finally {
                mainJob.cancel()
            }
        }

        if (::simpleMessage.isInitialized) {
            simpleMessage.bukkitAudiences.close()
        }

        if (::databaseManager.isInitialized) {
            logger.info("正在关闭数据库连接…")
            databaseManager.dataSource.close()
        }
    }


}