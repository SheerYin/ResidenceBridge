package me.yin.residencebridge

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.ServerConnection
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class ResidenceBridge @Inject constructor(val proxy: ProxyServer, @DataDirectory val dataDirectory: Path, val pluginContainer: PluginContainer) {

    init {
        instance = this

        proxyServer = proxy

        val pluginDescription = pluginContainer.description
        pluginName = pluginDescription.name.get()
        lowercaseName = pluginDescription.id
        pluginVersion = pluginDescription.version.get()
        pluginAuthors = pluginDescription.authors

        rootPath = dataDirectory.toAbsolutePath().parent.parent
    }

    companion object {
        lateinit var instance: ResidenceBridge

        lateinit var proxyServer: ProxyServer

        lateinit var pluginName: String
        lateinit var lowercaseName: String
        lateinit var pluginVersion: String
        lateinit var pluginAuthors: List<String>

        lateinit var rootPath: Path

        fun getPrefixComponent(): TextComponent.Builder {
            return Component.text()
                .color(NamedTextColor.WHITE)
                .append(Component.text("["))
                .append(Component.text("领地桥接", NamedTextColor.GRAY))
                .append(Component.text("]"))
        }

        val pluginChannel: MinecraftChannelIdentifier by lazy {
            MinecraftChannelIdentifier.from("${lowercaseName}:channel")
        }
    }

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        proxy.sendMessage(getPrefixComponent().append(Component.text(" 插件开始加载 $pluginVersion")).build())
        proxy.channelRegistrar.register(pluginChannel)
    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        proxy.sendMessage(getPrefixComponent().append(Component.text(" 插件开始卸载 $pluginVersion")).build())
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

        val player = event.target as? Player
        if (player == null) {
            // proxy.sendMessage(getPrefixComponent().append(Component.text(" 不是玩家")).build())
            return
        }

        DataInputStream(ByteArrayInputStream(event.data)).use { input ->
            val action = input.readUTF()
            if (action != "teleport") {
                return
            }

            val residenceName = input.readUTF()
            val serverName = input.readUTF()

            val registeredServer = proxy.getServer(serverName).orElse(null)
            if (registeredServer == null) {
                proxy.sendMessage(getPrefixComponent().append(Component.text(" 服务器不存在")).build())
                return
            }
            player.createConnectionRequest(registeredServer).fireAndForget()

            val byteArrayOutputStream = ByteArrayOutputStream()
            DataOutputStream(byteArrayOutputStream).use { output ->
                output.writeUTF("teleport")
                output.writeUTF(player.username)
                output.writeUTF(residenceName)
            }

            proxyServer.scheduler.buildTask(this, Runnable {
                registeredServer.sendPluginMessage(pluginChannel, byteArrayOutputStream.toByteArray())
            }).delay(1000L, TimeUnit.MILLISECONDS).schedule()
        }

    }


}