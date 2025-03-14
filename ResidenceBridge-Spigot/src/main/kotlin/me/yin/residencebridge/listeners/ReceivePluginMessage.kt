package me.yin.residencebridge.listeners

import kotlinx.coroutines.launch
import me.yin.residencebridge.ResidenceBridge
import me.yin.residencebridge.provider.register.ResidenceProviderRegister
import me.yin.residencebridge.service.ResidenceTeleport
import org.bukkit.Bukkit
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
            if (action != "teleport") {
                return
            }

            val playerName = input.readUTF()
            val residenceName = input.readUTF()

            val target = Bukkit.getPlayerExact(playerName)
            if (target == null) {
                //
                return
            }

            val residenceInstance = ResidenceProviderRegister.residence
            if (residenceInstance == null) {
                // 没装 residence
                return
            }

            ResidenceBridge.scope.launch {
                val claimedResidence = residenceInstance.residenceManager.getByName(residenceName)
                if (claimedResidence != null) {
                    Bukkit.getScheduler().runTask(ResidenceBridge.instance, Runnable {
                        ResidenceTeleport.local(player, claimedResidence)
                    })
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