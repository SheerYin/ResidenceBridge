package me.yin.residencebridge.listeners.residence

import com.bekvon.bukkit.residence.event.ResidenceDeleteEvent
import kotlinx.coroutines.launch
import me.yin.residencebridge.ResidenceBridge
import me.yin.residencebridge.persistence.ResidenceMySQL
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object ResidenceDelete : Listener {

    // 领地被删除 前 时才会触发
    @EventHandler
    fun onResidenceDelete(event: ResidenceDeleteEvent) {
        val residenceName = event.residence.residenceName

//        Bukkit.broadcastMessage("触发 onResidenceDelete")

        ResidenceBridge.scope.launch {
            ResidenceMySQL.deleteResidence(residenceName)
        }
    }
}
