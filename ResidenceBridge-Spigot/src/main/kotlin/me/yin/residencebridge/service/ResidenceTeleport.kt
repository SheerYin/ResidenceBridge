package me.yin.residencebridge.service

import me.yin.residencebridge.ResidenceBridge
import org.bukkit.entity.Player
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

object ResidenceTeleport {

    fun teleport(player: Player, residenceName: String, serverName: String = ResidenceBridge.serverName) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        DataOutputStream(byteArrayOutputStream).use { output ->
            output.writeUTF("teleport")
            output.writeUTF(residenceName)
            output.writeUTF(serverName)
        }
        player.sendPluginMessage(ResidenceBridge.instance, ResidenceBridge.pluginChannel, byteArrayOutputStream.toByteArray())
    }

}