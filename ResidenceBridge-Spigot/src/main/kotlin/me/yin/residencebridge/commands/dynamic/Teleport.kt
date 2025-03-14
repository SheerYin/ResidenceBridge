package me.yin.residencebridge.commands.dynamic

import kotlinx.coroutines.launch
import me.yin.residencebridge.ResidenceBridge
import me.yin.residencebridge.commands.DynamicTabExecutor
import me.yin.residencebridge.provider.register.ResidenceProviderRegister
import me.yin.residencebridge.service.ResidenceTeleport
import me.yin.residencebridge.persistence.ResidenceMySQL
import org.bukkit.Bukkit
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
        if (residenceInstance == null) {
            sender.sendMessage("${ResidenceBridge.pluginPrefix} 未安装 Residence")
            return
        }

        ResidenceBridge.scope.launch {
            val claimedResidence = residenceInstance.residenceManager.getByName(residenceName)
            if (claimedResidence != null) {
                Bukkit.getScheduler().runTask(ResidenceBridge.instance, Runnable {
                    ResidenceTeleport.local(player, claimedResidence)
                })
                return@launch // 本地存在领地
            }

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