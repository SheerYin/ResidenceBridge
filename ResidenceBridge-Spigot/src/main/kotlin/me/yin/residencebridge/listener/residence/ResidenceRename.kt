package me.yin.residencebridge.listener.residence

import com.bekvon.bukkit.residence.event.ResidenceRenameEvent
import kotlinx.coroutines.launch
import me.yin.residencebridge.ResidenceBridge
import me.yin.residencebridge.persistence.ResidenceMySQL
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object ResidenceRename : Listener {

    // 领地被更改名称 前 时才会触发
    @EventHandler
    fun onResidenceRename(event: ResidenceRenameEvent) {

//        Bukkit.broadcastMessage("触发 onResidenceRename")

        val oldName = event.oldResidenceName
        val newName = event.newResidenceName

        if (ResidenceMySQL.isResidenceExists(oldName)) {
            // Bukkit.getPlayerExact(event.residence.owner)?.sendMessage(MessageYAML.configuration.getString("command.create-name-already-exists"))
            event.isCancelled = true
            return
        } else {
            ResidenceBridge.scope.launch {
                ResidenceMySQL.updateResidenceName(oldName, newName)
            }
        }
    }
}
