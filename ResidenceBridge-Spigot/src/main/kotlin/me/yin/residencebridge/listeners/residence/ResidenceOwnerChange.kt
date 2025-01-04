package me.yin.residencebridge.listeners.residence

import com.bekvon.bukkit.residence.event.ResidenceOwnerChangeEvent
import kotlinx.coroutines.launch
import me.yin.residencebridge.ResidenceBridge
import me.yin.residencebridge.storage.ResidenceStorage
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.*

object ResidenceOwnerChange : Listener {

    // 领地被更改所有者 前 时才会触发
    @EventHandler
    fun onResidenceOwnerChange(event: ResidenceOwnerChangeEvent) {

//        Bukkit.broadcastMessage("触发 onResidenceOwnerChange")

        val residenceName = event.residence.residenceName
        val ownerUUID = event.newOwnerUuid ?: UUID(0L, 0L) // 转移给服务器时是 null
        val owner = event.newOwner

        ResidenceBridge.scope.launch {
            ResidenceStorage.updateResidenceOwner(residenceName, ownerUUID, owner)
        }
    }

}
