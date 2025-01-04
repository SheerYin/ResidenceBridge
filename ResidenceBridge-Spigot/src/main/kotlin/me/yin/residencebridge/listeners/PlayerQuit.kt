package me.yin.residencebridge.listeners

import me.yin.residencebridge.cache.DebounceCache
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

object PlayerQuit : Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player

        DebounceCache.remove(player.name)
    }


}