package me.yin.residencebridge.commands.dynamic

import me.yin.residencebridge.service.ResidenceTeleport
import org.bukkit.entity.Player

object Teleport {

    fun dynamic(player: Player, residenceName: String) {
        ResidenceTeleport.teleport(player, residenceName)
    }

}