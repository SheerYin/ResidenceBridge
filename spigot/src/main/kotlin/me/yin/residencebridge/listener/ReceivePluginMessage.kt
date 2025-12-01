package me.yin.residencebridge.listener

import com.bekvon.bukkit.residence.Residence
import me.yin.residencebridge.ResidenceBridge
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.plugin.messaging.PluginMessageListener
import java.io.ByteArrayInputStream
import java.io.DataInputStream

class ReceivePluginMessage(val residenceBridge: ResidenceBridge, val residenceInstance: Residence?, val pluginChannel: String) : PluginMessageListener {

    @EventHandler
    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        if (channel != pluginChannel) {
            return
        }

        DataInputStream(ByteArrayInputStream(message)).use { input ->
            val action = input.readUTF()
            if (action != "teleport") {
                return
            }

            val playerName = input.readUTF()
            val residenceName = input.readUTF()

            val target = residenceBridge.server.getPlayerExact(playerName)
            if (target == null) {
                //
                return
            }

            if (residenceInstance != null) {
                val claimedResidence = residenceInstance.residenceManager.getByName(residenceName)
                if (claimedResidence != null) {
                    target.teleport(claimedResidence.getTeleportLocation(player, true))
                }
            }

        }
    }

//    @EventHandler
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