package me.yin.residencebridge.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

object PlayerJoin : Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player


//        ResidenceBridge.scope.launch {
//            val player = event.player
//            delay(500)
////            SendByte.serverName(player)
////            SendByte.playerNames(player)
//        }

    }


}