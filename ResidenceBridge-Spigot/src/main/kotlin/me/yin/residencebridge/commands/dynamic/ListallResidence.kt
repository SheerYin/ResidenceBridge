package me.yin.residencebridge.commands.dynamic

import kotlinx.coroutines.launch
import me.yin.residencebridge.ResidenceBridge
import me.yin.residencebridge.commands.DynamicTabExecutor
import me.yin.residencebridge.storage.ResidenceStorage
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object ListallResidence {

    private val mainParameter = "listall"

    fun dynamic(sender: CommandSender, mainPermission: String) {
        if (!DynamicTabExecutor.permissionMessage(sender, "$mainPermission.$mainParameter")) {
            return
        }


        ResidenceBridge.scope.launch {
            val residenceInfos = ResidenceStorage.selectResidences()
            sender.sendMessage("${ResidenceBridge.pluginPrefix} 领地列表")
            for (residenceInfo in residenceInfos) {
                sender.sendMessage("${ResidenceBridge.pluginPrefix} 领地 ${residenceInfo.residenceName} 位于 ${residenceInfo.serverName}")
            }
        }

    }

}