package me.yin.residencebridge.listeners

import me.yin.residencebridge.ResidenceBridge
import me.yin.residencebridge.provider.register.ResidenceProvider
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.plugin.messaging.PluginMessageListener
import java.io.ByteArrayInputStream
import java.io.DataInputStream

object ReceivePluginMessage : PluginMessageListener {

    @EventHandler
    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        if (channel != ResidenceBridge.pluginChannel) {
            return
        }

        DataInputStream(ByteArrayInputStream(message)).use { input ->
            val action = input.readUTF()
            if (action != ("teleport")) {
                return
            }

            val playerName = input.readUTF()
            val residenceName = input.readUTF()

            val target = ResidenceBridge.bukkitServer.getPlayerExact(playerName)
            if (target == null) {
                // ResidenceBridge.broadcastPrefix("找不到玩家 $playerName")
                return
            }

            val claimedResidence = ResidenceProvider.residence.residenceManager.getByName(residenceName)
            if (claimedResidence == null) {
                // ResidenceBridge.broadcastPrefix("找不到领地 $residenceName")
                return
            }
            target.teleport(claimedResidence.getTeleportLocation(target, true))
        }
    }

//    @EventHandler(priority = EventPriority.NORMAL)
//    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
//        if (channel != ResidenceStorage.pluginChannel) {
//            return
//        }
//
//        DataInputStream(ByteArrayInputStream(message)).use { input ->
//            val action = input.readUTF()
//
//            when (action) {
//                // 标识符 bytes.size bytes
//                "residencestorage:identifier" -> {
//                    val length = input.readShort()
//                    val bytes = ByteArray(length.toInt())
//                    input.readFully(bytes)
//                    DataInputStream(ByteArrayInputStream(bytes)).use { stream ->
//                        val target = Bukkit.getPlayerExact(stream.readUTF()) ?: return
//                        val residenceName = stream.readUTF()
//                        val claimedResidence = Residence.getInstance().residenceManager.residences[residenceName.lowercase(Locale.getDefault())] ?: return
//                        target.teleport(claimedResidence.getTeleportLocation(target, true), PlayerTeleportEvent.TeleportCause.PLUGIN)
//                    }
//                }
//                // PlayerList ALL "a, b, c"
//                "PlayerList" -> {
//                    if (input.readUTF() != "ALL") {
//                        return
//                    }
//                    ResidenceStorage.playerNames = input.readUTF().split(", ").toMutableList()
//                }
//                /*
//                // GetServers "a, b, c"
//                "GetServers" -> {
//                    ResidenceStorageSpigotMain.serverNames.addAll(input.readUTF().split(", "))
//                }
//                 */
//                // GetServers serverName
//                "GetServer" -> {
//                    ResidenceStorage.serverName = input.readUTF()
//                }
//
//                else -> {
//                    return
//                }
//            }
//
//        }
//
//    }


}