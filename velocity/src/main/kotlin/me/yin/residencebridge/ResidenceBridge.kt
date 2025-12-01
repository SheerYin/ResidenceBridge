package me.yin.residencebridge

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.plugin.PluginDescription
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.ServerConnection
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.logging.Logger

class ResidenceBridge @Inject constructor(
    val logger: Logger,
    val proxy: ProxyServer,
    val pluginContainer: PluginContainer
) {

    val pluginDescription: PluginDescription = pluginContainer.description

    val pluginName = pluginDescription.name.get()
    val pluginNameLowercase: String = pluginDescription.id
    val pluginVersion = pluginDescription.version.get()
    val pluginAuthors: List<String> = pluginDescription.authors

    val pluginChannel: MinecraftChannelIdentifier = MinecraftChannelIdentifier.create(pluginNameLowercase, "channel")

    // val rootPath = dataDirectory.toAbsolutePath().parent.parent

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        logger.info("插件开始加载 $pluginVersion")
        proxy.channelRegistrar.register(pluginChannel)
    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        logger.info("插件开始卸载 $pluginVersion")
    }

    @Subscribe
    fun onPluginMessage(event: PluginMessageEvent) {
        if (event.identifier != pluginChannel) {
            return
        }
        event.result = PluginMessageEvent.ForwardResult.handled();
        if (event.source !is ServerConnection) {
            return
        }

//        val player = event.target as? Player
//        if (player == null) {
//            // proxy.sendMessage(getPrefixComponent().append(Component.text(" 不是玩家")).build())
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

            val player = proxy.getPlayer(playerName).orElse(null)
            if (player == null) {
//                proxy.console.sendMessage(TextComponent("$pluginPrefix 玩家 $playerName 不存在"))
                return
            }
            val registeredServer = proxy.getServer(serverName).orElse(null)
            if (registeredServer == null) {
                logger.info("$serverName 服务器不存在")
                return
            }

            val connect = player.createConnectionRequest(registeredServer).connect()
            connect.thenAccept { connectResult ->
                if (connectResult.isSuccessful) {
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    DataOutputStream(byteArrayOutputStream).use { output ->
                        output.writeUTF("teleport")
                        output.writeUTF(player.username)
                        output.writeUTF(residenceName)
                    }
                    registeredServer.sendPluginMessage(pluginChannel, byteArrayOutputStream.toByteArray())
                }
            }

        }

    }


}