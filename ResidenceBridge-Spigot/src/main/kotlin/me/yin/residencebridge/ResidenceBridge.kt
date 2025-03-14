package me.yin.residencebridge

import com.bekvon.bukkit.residence.Residence
import kotlinx.coroutines.*
import me.yin.residencebridge.commands.DynamicTabExecutor
import me.yin.residencebridge.configuration.ConfigurationYAML
import me.yin.residencebridge.configuration.ResidenceYAML
import me.yin.residencebridge.listeners.PlayerJoin
import me.yin.residencebridge.listeners.ReceivePluginMessage
import me.yin.residencebridge.listeners.residence.*
import me.yin.residencebridge.persistence.ResidenceMySQL
import me.yin.residencebridge.provider.register.ResidenceBridgeExpansion
import me.yin.residencebridge.provider.register.ResidenceProviderRegister
import org.bukkit.plugin.java.JavaPlugin

class ResidenceBridge : JavaPlugin() {

    companion object {
        lateinit var instance: ResidenceBridge

        lateinit var pluginName: String
        val lowercaseName: String by lazy { pluginName.lowercase() }
        lateinit var pluginVersion: String
        lateinit var pluginAuthors: List<String>
        lateinit var pluginPrefix: String

        val scope: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO) }

        val pluginChannel: String by lazy { "${lowercaseName}:channel" }

        // 如果需要 reload 则修改为 lateinit var

        //        fun broadcast(message: String) {
//            for (player in bukkitServer.onlinePlayers) {
//                player.sendMessage(message)
//            }
//        }
//
//        fun broadcastPrefix(message: String, prefix: String = pluginPrefix) {
//            for (player in bukkitServer.onlinePlayers) {
//                player.sendMessage("$prefix $message")
//            }
//        }
    }

    override fun onEnable() {
        instance = this

        pluginName = description.name
        pluginVersion = description.version
        pluginAuthors = description.authors
        pluginPrefix = "§f[§7${description.prefix}§f]"

        server.consoleSender.sendMessage("$pluginPrefix 插件开始加载 $pluginVersion")

        setupProvider()

        ConfigurationYAML.initialize()
        ConfigurationYAML.load()
        ResidenceYAML.initialize()
        ResidenceYAML.load()

        ResidenceMySQL.initialize()

        //

        server.messenger.registerOutgoingPluginChannel(this, pluginChannel)
        server.messenger.registerIncomingPluginChannel(this, pluginChannel, ReceivePluginMessage)

        server.pluginManager.registerEvents(PlayerJoin, this)

        if (ResidenceProviderRegister.residence != null) {
            server.pluginManager.registerEvents(ResidenceCommand, this)
            server.pluginManager.registerEvents(ResidenceCreation, this)
            server.pluginManager.registerEvents(ResidenceDelete, this)
            server.pluginManager.registerEvents(ResidenceFlagChange, this)
            server.pluginManager.registerEvents(ResidenceOwnerChange, this)
            server.pluginManager.registerEvents(ResidenceRename, this)
        }

        getCommand(lowercaseName)?.setExecutor(DynamicTabExecutor)
    }

    fun setupProvider() {
        if (server.pluginManager.getPlugin("Residence") == null) {
            server.consoleSender.sendMessage("$pluginPrefix 找不到 Residence 相关逻辑取消")
        } else {
           ResidenceProviderRegister.residence = Residence.getInstance()
        }

        if (server.pluginManager.getPlugin("PlaceholderAPI") == null) {
            server.consoleSender.sendMessage("$pluginPrefix 没有找到 PlaceholderAPI 无法提供解析 PlaceholderAPI 变量")
        } else {
            ResidenceBridgeExpansion(this).register()
        }
    }

    override fun onDisable() {
        server.consoleSender.sendMessage("$pluginPrefix 插件开始卸载 $pluginVersion")

        runBlocking {
            try {
                withTimeout(60000L) {
                    server.consoleSender.sendMessage("$pluginPrefix 正在等待任务完成，最多等待 1 分钟")
                    scope.coroutineContext[Job]?.children?.forEach { it.join() }
                    server.consoleSender.sendMessage("$pluginPrefix 任务全部完成")
                }
            } catch (exception: TimeoutCancellationException) {
                server.consoleSender.sendMessage("$pluginPrefix 已超时，强制清理所有任务")
                scope.cancel()
            }
        }

        ResidenceMySQL.dataSource.close()
    }


}