package me.yin.residencebridge.service

import com.bekvon.bukkit.residence.protection.ClaimedResidence
import me.yin.residencebridge.ResidenceBridge
import org.bukkit.entity.Player
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

object ResidenceTeleport {

    fun local(player: Player, claimedResidence: ClaimedResidence) {
        player.teleport(claimedResidence.getTeleportLocation(player, true))
    }

    fun global(player: Player, residenceName: String, serverName: String) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        DataOutputStream(byteArrayOutputStream).use { output ->
            output.writeUTF("teleport")
            output.writeUTF(player.name)
            output.writeUTF(residenceName)
            output.writeUTF(serverName)
        }
        player.sendPluginMessage(ResidenceBridge.instance, ResidenceBridge.pluginChannel, byteArrayOutputStream.toByteArray())
    }

}