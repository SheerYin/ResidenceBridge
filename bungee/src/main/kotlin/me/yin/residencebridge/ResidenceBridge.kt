package me.yin.residencebridge

import net.md_5.bungee.api.chat.TextComponent
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

    val pluginName = description.name
    val pluginNameLowercase = pluginName.lowercase()
    val pluginVersion = description.version
    val pluginAuthors = description.author

    val pluginChannel = "${pluginNameLowercase}:channel"

    override fun onEnable() {
        logger.info("插件开始加载 $pluginVersion")

        proxy.registerChannel(pluginChannel)
        proxy.pluginManager.registerListener(this, this)
    }

    override fun onDisable() {
        logger.info("插件开始卸载 $pluginVersion")
    }

    @EventHandler
    fun onPluginMessage(event: PluginMessageEvent) {
        if (event.sender !is Server || event.tag != pluginChannel) {
            return
        }

//        val proxiedPlayer = event.receiver as? ProxiedPlayer
//        if (proxiedPlayer == null) {
//            // proxy.console.sendMessage(TextComponent("$pluginPrefix 不是玩家"))
//            return
//        }

        DataInputStream(ByteArrayInputStream(event.data)).use { input ->
            val action = input.readUTF()
            if (action != "teleport") {
                return
            }

            val playerName = input.readUTF()
            val serverName = input.readUTF()
            val residenceName = input.readUTF()

            val proxiedPlayer = proxy.getPlayer(playerName)
            if (proxiedPlayer == null) {
//                proxy.console.sendMessage(TextComponent("$pluginPrefix 玩家 $playerName 不存在"))
                return
            }
            val serverInfo = proxy.getServerInfo(serverName)
            if (serverInfo == null) {
                logger.info("服务器 $serverName 不存在")
                return
            }
            proxiedPlayer.connect(serverInfo) { successful, throwable ->
                if (successful) {
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    DataOutputStream(byteArrayOutputStream).use { output ->
                        output.writeUTF("teleport")
                        output.writeUTF(proxiedPlayer.name)
                        output.writeUTF(residenceName)
                    }
                    serverInfo.sendData(pluginChannel, byteArrayOutputStream.toByteArray())
                }
            }


        }
    }


}