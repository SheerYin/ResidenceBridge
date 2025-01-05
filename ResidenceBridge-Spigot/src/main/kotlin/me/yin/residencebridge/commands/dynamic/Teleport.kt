package me.yin.residencebridge.commands.dynamic

import kotlinx.coroutines.launch
import me.yin.residencebridge.ResidenceBridge
import me.yin.residencebridge.commands.DynamicTabExecutor
import me.yin.residencebridge.provider.register.ResidenceProviderRegister
import me.yin.residencebridge.service.ResidenceTeleport
import me.yin.residencebridge.storage.ResidenceStorage
import org.bukkit.entity.Player

object Teleport {

    private val mainParameter = "teleport"

    fun dynamic(player: Player, mainPermission: String, residenceName: String) {
        if (!DynamicTabExecutor.permissionMessage(player, "$mainPermission.$mainParameter")) {
            return
        }

        ResidenceBridge.scope.launch {
            val claimedResidence = ResidenceProviderRegister.residence.residenceManager.getByName(residenceName)
            if (claimedResidence != null) {
                ResidenceTeleport.local(player, claimedResidence)
                return@launch // 本地存在领地
            }

            val residenceInfo = ResidenceStorage.selectResidence(residenceName)
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