package me.yin.residencebridge.command.dynamic

import kotlinx.coroutines.launch
import me.yin.residencebridge.ResidenceBridge
import me.yin.residencebridge.command.DynamicTabExecutor
import me.yin.residencebridge.persistence.ResidenceMySQL
import me.yin.residencebridge.provider.register.ResidenceProviderRegister
import me.yin.residencebridge.service.ResidenceTeleport
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object Teleport {

    val mainParameter = "teleport"

    fun dynamic(sender: CommandSender, residenceName: String) {
        if (!DynamicTabExecutor.permissionMessage(sender, "${DynamicTabExecutor.mainPermission}.$mainParameter")) {
            return
        }

        val player = sender as? Player
        if (player == null) {
            sender.sendMessage("${ResidenceBridge.pluginPrefix} 此命令仅限玩家执行")
            return
        }

        val residenceInstance = ResidenceProviderRegister.residence
        if (residenceInstance != null) {
            val claimedResidence = residenceInstance.residenceManager.getByName(residenceName)
            if (claimedResidence != null) {
                ResidenceTeleport.local(player, claimedResidence.getTeleportLocation(player, true))
                return
            }
        }

        ResidenceBridge.scope.launch {
            val residenceInfo = ResidenceMySQL.selectResidence(residenceName)
            if (residenceInfo == null) {
                player.sendMessage("${ResidenceBridge.pluginPrefix} 领地不存在")
                return@launch
            }
            val playerUUID = player.uniqueId
            if (player.hasPermission("residence.admin.tp") || residenceInfo.ownerUUID == playerUUID || residenceInfo.residenceFlags["tp"] == true || residenceInfo.playerFlags[playerUUID.toString()]?.get("tp") == true) {
                ResidenceTeleport.global(player, residenceName, residenceInfo.serverName)
                player.sendMessage("${ResidenceBridge.pluginPrefix} 开始传送")
            } else {
                player.sendMessage("${ResidenceBridge.pluginPrefix} 你没有权限传送这个领地")
            }
        }
    }

}