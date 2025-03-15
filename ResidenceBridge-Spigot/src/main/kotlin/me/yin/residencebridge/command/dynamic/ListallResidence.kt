package me.yin.residencebridge.command.dynamic

import kotlinx.coroutines.launch
import me.yin.residencebridge.ResidenceBridge
import me.yin.residencebridge.command.DynamicTabExecutor
import me.yin.residencebridge.persistence.ResidenceMySQL
import org.bukkit.command.CommandSender

object ListallResidence {

    val mainParameter = "listall"

    fun dynamic(sender: CommandSender) {
        if (!DynamicTabExecutor.permissionMessage(sender, "${DynamicTabExecutor.mainPermission}.$mainParameter")) {
            return
        }

        ResidenceBridge.scope.launch {
            val residenceInfos = ResidenceMySQL.selectResidences()
            sender.sendMessage("${ResidenceBridge.pluginPrefix} 领地列表")
            for (residenceInfo in residenceInfos) {
                sender.sendMessage("${ResidenceBridge.pluginPrefix} 领地 ${residenceInfo.residenceName} 位于 ${residenceInfo.serverName}")
            }
        }

    }

}