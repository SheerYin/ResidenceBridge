package me.yin.residencebridge.listeners.residence

import com.bekvon.bukkit.residence.event.ResidenceCreationEvent
import kotlinx.coroutines.launch
import me.yin.residencebridge.ResidenceBridge
import me.yin.residencebridge.model.ResidenceInfo
import me.yin.residencebridge.storage.ResidenceStorage
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

object ResidenceCreation : Listener {

    // 领地创建 前 触发
    @EventHandler(priority = EventPriority.NORMAL)
    fun onResidenceCreation(event: ResidenceCreationEvent) {

//        Bukkit.broadcastMessage("触发 onResidenceCreation")

        val residence = event.residence
        val residenceName = residence.residenceName
        val ownerUUID = residence.ownerUUID
        val owner = residence.owner
        val permissions = residence.permissions

//        if (ResidenceStorage.isResidenceExists(residenceName)) {
//            event.isCancelled = true
//            return
//        }

        ResidenceBridge.scope.launch {
            val residenceInfo = ResidenceInfo(
                residenceName,
                ownerUUID,
                owner,
                permissions.flags,
                permissions.playerFlags,
                ResidenceBridge.serverName
            )
            ResidenceStorage.insertResidence(residenceInfo)
        }
    }

}

