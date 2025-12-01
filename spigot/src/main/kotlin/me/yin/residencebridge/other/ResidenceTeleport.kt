package me.yin.residencebridge.other

import me.yin.residencebridge.ResidenceBridge
import org.bukkit.entity.Player
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class ResidenceTeleport(val instance: ResidenceBridge, val pluginChannel: String) {

    fun global(target: Player, residenceName: String, serverName: String) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        DataOutputStream(byteArrayOutputStream).use { output ->
            output.writeUTF("teleport")
            output.writeUTF(target.name)
            output.writeUTF(serverName)
            output.writeUTF(residenceName)
        }
        target.sendPluginMessage(instance, pluginChannel, byteArrayOutputStream.toByteArray())
    }

}