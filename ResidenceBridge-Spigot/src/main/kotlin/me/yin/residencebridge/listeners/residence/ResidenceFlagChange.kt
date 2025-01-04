package me.yin.residencebridge.listeners.residence

import com.bekvon.bukkit.residence.event.ResidenceFlagChangeEvent
import com.bekvon.bukkit.residence.event.ResidenceFlagEvent.FlagType
import com.bekvon.bukkit.residence.protection.FlagPermissions.FlagState
import kotlinx.coroutines.launch
import me.yin.residencebridge.ResidenceBridge
import me.yin.residencebridge.storage.ResidenceStorage
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

object ResidenceFlagChange : Listener {

    // Flags 被修改 前 时才会触发
    // 同时 ResidenceCreation 也会触发，因为创建领地时同时在初始化 Flags
    // 但领地没创建完之前，获取的 residenceName 都是 null
    @EventHandler
    fun onResidenceFlagChange(event: ResidenceFlagChangeEvent) {

//        Bukkit.broadcastMessage("触发 onResidenceFlagChange")

        val residenceName = event.residence.residenceName ?: return

        ResidenceBridge.scope.launch {
            when (event.flagType) {
                FlagType.RESIDENCE -> {
                    when (event.newState) {
                        FlagState.TRUE -> ResidenceStorage.updateSetResidenceFlags(residenceName, event.flag, true)
                        FlagState.FALSE -> ResidenceStorage.updateSetResidenceFlags(residenceName, event.flag, false)
                        else -> ResidenceStorage.updateRemoveResidenceFlags(residenceName, event.flag)
                    }
                }

                FlagType.PLAYER -> {
                    val playerUUID = event.player.uniqueId
                    when (event.newState) {
                        FlagState.TRUE -> ResidenceStorage.updateSetPlayerFlags(residenceName, playerUUID, event.flag, true)
                        FlagState.FALSE -> ResidenceStorage.updateSetPlayerFlags(residenceName, playerUUID, event.flag, false)
                        else -> ResidenceStorage.updateRemovePlayerFlags(residenceName, playerUUID, event.flag)
                    }
                }

                else -> {}
            }
        }
    }


}
