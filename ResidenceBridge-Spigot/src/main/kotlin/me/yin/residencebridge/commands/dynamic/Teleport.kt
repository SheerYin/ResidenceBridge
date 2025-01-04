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

        val claimedResidence = ResidenceProviderRegister.residence.residenceManager.getByName(residenceName)
        if (claimedResidence != null) {
            ResidenceTeleport.local(player, claimedResidence)
            return // 本地存在领地
        }

        ResidenceBridge.scope.launch {
            val serverName = ResidenceStorage.selectResidenceServerName(residenceName)
            if (serverName == null) {
                player.sendMessage(ResidenceBridge.pluginPrefix + " 领地或服务器不存在")
                return@launch
            }
            ResidenceTeleport.global(player, residenceName)
            player.sendMessage(ResidenceBridge.pluginPrefix + " 开始传送")
        }
    }

}