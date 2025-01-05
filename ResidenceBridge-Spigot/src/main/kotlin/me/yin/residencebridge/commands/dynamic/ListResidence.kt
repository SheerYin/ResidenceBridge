package me.yin.residencebridge.commands.dynamic

import kotlinx.coroutines.launch
import me.yin.residencebridge.ResidenceBridge
import me.yin.residencebridge.commands.DynamicTabExecutor
import me.yin.residencebridge.storage.ResidenceStorage
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object ListResidence {

    private val mainParameter = "list"

    fun dynamic(player: Player, mainPermission: String) {
        if (!DynamicTabExecutor.permissionMessage(player, "$mainPermission.$mainParameter")) {
            return
        }

        ResidenceBridge.scope.launch {
            val names = ResidenceStorage.selectOwnerResidenceNames(player.uniqueId)
            player.sendMessage("${ResidenceBridge.pluginPrefix} 玩家 §2${player.name}§f 领地列表")
            for (name in names) {
                player.sendMessage("${ResidenceBridge.pluginPrefix} 领地 $name")
            }
        }

    }

    fun dynamic(sender: CommandSender, target: String, mainPermission: String) {
        if (!DynamicTabExecutor.permissionMessage(sender, "$mainPermission.$mainParameter.other")) {
            return
        }

        ResidenceBridge.scope.launch {
            val residenceInfos = ResidenceStorage.selectOwnerResidences(target)
            sender.sendMessage("${ResidenceBridge.pluginPrefix} 玩家 §2${target}§f 领地列表")
            for (residenceInfo in residenceInfos) {
                sender.sendMessage("${ResidenceBridge.pluginPrefix} 领地 ${residenceInfo.residenceName} 位于 ${residenceInfo.serverName}")
            }
        }

    }

}