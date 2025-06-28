package me.yin.residencebridge.command.dynamic

import kotlinx.coroutines.launch
import me.yin.residencebridge.ResidenceBridge
import me.yin.residencebridge.command.DynamicTabExecutor
import me.yin.residencebridge.persistence.MySqlResidenceRepository
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object ListResidence {

    val mainParameter = "list"

    fun dynamic(sender: CommandSender) {
        if (!DynamicTabExecutor.permissionMessage(sender, "${DynamicTabExecutor.mainPermission}.$mainParameter")) {
            return
        }

        val player = sender as? Player
        if (player == null) {
            sender.sendMessage("${ResidenceBridge.pluginPrefix} 此命令仅限玩家执行")
            return
        }

        ResidenceBridge.scope.launch {
            val names = MySqlResidenceRepository.selectOwnerResidenceNames(player.uniqueId)
            player.sendMessage("${ResidenceBridge.pluginPrefix} 玩家 §2${player.name}§f 领地列表")
            for (name in names) {
                player.sendMessage("${ResidenceBridge.pluginPrefix} 领地 $name")
            }
        }

    }

    fun dynamic(sender: CommandSender, targetName: String) {
        if (!DynamicTabExecutor.permissionMessage(sender, "${DynamicTabExecutor.mainPermission}.$mainParameter.other")) {
            return
        }

        ResidenceBridge.scope.launch {
            val residenceInfos = MySqlResidenceRepository.selectOwnerResidences(targetName)
            sender.sendMessage("${ResidenceBridge.pluginPrefix} 玩家 §2${targetName}§f 领地列表")
            for (residenceInfo in residenceInfos) {
                sender.sendMessage("${ResidenceBridge.pluginPrefix} 领地 ${residenceInfo.residenceName} 位于 ${residenceInfo.serverName}")
            }
        }

    }

}