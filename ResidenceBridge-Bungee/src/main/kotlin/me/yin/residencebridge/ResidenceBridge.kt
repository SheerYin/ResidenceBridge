package me.yin.residencebridge

import kotlinx.coroutines.*
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.connection.Server
import net.md_5.bungee.api.event.PluginMessageEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.event.EventHandler
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class ResidenceBridge : Plugin(), Listener {
    companion object {
        lateinit var instance: ResidenceBridge

        lateinit var proxyServer: ProxyServer

        lateinit var pluginName: String
        lateinit var lowercaseName: String
        lateinit var pluginVersion: String
        lateinit var pluginAuthor: String
        const val pluginPrefix = "§f[§7领地桥接§f]"

        lateinit var scope: CoroutineScope

        val pluginChannel by lazy { "${lowercaseName}:channel" }
    }

    override fun onEnable() {
        instance = this

        proxyServer = proxy

        pluginName = description.name
        lowercaseName = pluginName.lowercase()
        pluginVersion = description.version
        pluginAuthor = description.author

        proxy.console.sendMessage(TextComponent("$pluginPrefix 插件开始加载 $pluginVersion"))

        scope = CoroutineScope(Dispatchers.IO)

        proxy.registerChannel(pluginChannel)
        proxy.pluginManager.registerListener(this, this)
    }

    override fun onDisable() {
        proxy.console.sendMessage(TextComponent("$pluginPrefix 插件开始卸载 $pluginVersion"))

        runBlocking {
            try {
                withTimeout(60000L) {
                    proxy.console.sendMessage(TextComponent("$pluginPrefix 正在等待任务完成，最多等待 1 分钟"))
                    scope.coroutineContext[Job]?.children?.forEach { it.join() }
                    proxy.console.sendMessage(TextComponent("$pluginPrefix 任务全部完成"))
                }
            } catch (exception: TimeoutCancellationException) {
                proxy.console.sendMessage(TextComponent("$pluginPrefix 已超时，强制清理所有任务"))
                scope.cancel()
            }
        }
    }

    @EventHandler
    fun onPluginMessage(event: PluginMessageEvent) {
        if (event.sender !is Server || event.tag != pluginChannel) {
            return
        }

        val proxiedPlayer = event.receiver as? ProxiedPlayer
        if (proxiedPlayer == null) {
            // proxy.console.sendMessage(TextComponent("$pluginPrefix 不是玩家"))
            return
        }

        DataInputStream(ByteArrayInputStream(event.data)).use { input ->
            val action = input.readUTF()
            if (action != "teleport") {
                return
            }

            val residenceName = input.readUTF()
            val serverName = input.readUTF()

            val serverInfo = proxy.getServerInfo(serverName)
            if (serverInfo == null) {
                proxy.console.sendMessage(TextComponent("$pluginPrefix $serverName 服务器不存在"))
                return
            }
            proxiedPlayer.connect(serverInfo)

            val byteArrayOutputStream = ByteArrayOutputStream()
            DataOutputStream(byteArrayOutputStream).use { output ->
                output.writeUTF("teleport")
                output.writeUTF(proxiedPlayer.name)
                output.writeUTF(residenceName)
            }

            scope.launch {
                delay(1000L)
                serverInfo.sendData(pluginChannel, byteArrayOutputStream.toByteArray())
            }
        }
    }


}