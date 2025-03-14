package me.yin.residencebridge.commands.dynamic

import kotlinx.coroutines.launch
import me.yin.residencebridge.ResidenceBridge
import me.yin.residencebridge.commands.DynamicTabExecutor
import me.yin.residencebridge.persistence.ResidenceMySQL
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
            val names = ResidenceMySQL.selectOwnerResidenceNames(player.uniqueId)
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
            val residenceInfos = ResidenceMySQL.selectOwnerResidences(targetName)
            sender.sendMessage("${ResidenceBridge.pluginPrefix} 玩家 §2${targetName}§f 领地列表")
            for (residenceInfo in residenceInfos) {
                sender.sendMessage("${ResidenceBridge.pluginPrefix} 领地 ${residenceInfo.residenceName} 位于 ${residenceInfo.serverName}")
            }
        }

    }

}