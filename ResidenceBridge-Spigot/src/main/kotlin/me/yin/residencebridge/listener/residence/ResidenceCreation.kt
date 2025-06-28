package me.yin.residencebridge.listener.residence

import com.bekvon.bukkit.residence.event.ResidenceCreationEvent
import kotlinx.coroutines.launch
import me.yin.residencebridge.ResidenceBridge
import me.yin.residencebridge.infrastructure.configuration.ConfigurationYAML
import me.yin.residencebridge.model.ResidenceInfo
import me.yin.residencebridge.persistence.MySqlResidenceRepository
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object ResidenceCreation : Listener {

    // 领地创建 前 触发
    @EventHandler
    fun onResidenceCreation(event: ResidenceCreationEvent) {

//        Bukkit.broadcastMessage("触发 onResidenceCreation")

        val residence = event.residence
        val residenceName = residence.residenceName
        val ownerUUID = residence.ownerUUID
        val owner = residence.owner
        val permissions = residence.permissions

        // 双重防御
        if (MySqlResidenceRepository.isResidenceExists(residenceName)) {
            event.isCancelled = true
            return
        }

        ResidenceBridge.scope.launch {
            val residenceInfo = ResidenceInfo(
                residenceName,
                ownerUUID,
                owner,
                permissions.flags,
                permissions.playerFlags,
                ConfigurationYAML.serverName
            )
            MySqlResidenceRepository.insertResidence(residenceInfo)
        }
    }

}

