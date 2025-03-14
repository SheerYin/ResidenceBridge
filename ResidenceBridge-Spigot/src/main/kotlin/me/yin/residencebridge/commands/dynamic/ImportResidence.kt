package me.yin.residencebridge.commands.dynamic

import kotlinx.coroutines.launch
import me.yin.residencebridge.ResidenceBridge
import me.yin.residencebridge.commands.DynamicTabExecutor
import me.yin.residencebridge.configuration.ConfigurationYAML
import me.yin.residencebridge.model.ResidenceInfo
import me.yin.residencebridge.provider.register.ResidenceProviderRegister
import me.yin.residencebridge.persistence.ResidenceMySQL
import org.bukkit.command.CommandSender

object ImportResidence {

    val mainParameter = "import"

    fun dynamic(sender: CommandSender) {
        if (!DynamicTabExecutor.permissionMessage(sender, "${DynamicTabExecutor.mainPermission}.$mainParameter")) {
            return
        }

        val residenceInstance = ResidenceProviderRegister.residence
        if (residenceInstance == null) {
            sender.sendMessage("${ResidenceBridge.pluginPrefix} 未安装 Residence")
            return
        }

        ResidenceBridge.scope.launch {
            val map = residenceInstance.residenceManager.residences

            val localNames = mutableListOf<String>()
            val globalNames = ResidenceMySQL.selectResidenceNames()

            val serverName = ConfigurationYAML.serverName
            val residenceInfos = mutableListOf<ResidenceInfo>()
            for ((key, claimedResidence) in map) {
                // key 是小写
                val residenceName = claimedResidence.residenceName
                localNames.add(residenceName)
                residenceInfos.add(
                    ResidenceInfo(
                        residenceName,
                        claimedResidence.ownerUUID,
                        claimedResidence.owner,
                        claimedResidence.permissions.flags,
                        claimedResidence.permissions.playerFlags,
                        serverName
                    )
                )
            }

            val duplicates = mutableListOf<String>()
            for (element in localNames) {
                if (element in globalNames) {
                    duplicates.add(element)
                }
            }

            if (duplicates.isEmpty()) {
                ResidenceMySQL.batchInsertResidences(residenceInfos)
                sender.sendMessage("${ResidenceBridge.pluginPrefix} 导入完成")
            } else {
                sender.sendMessage("${ResidenceBridge.pluginPrefix} 数据库中已有重名领地 $duplicates")
                sender.sendMessage("${ResidenceBridge.pluginPrefix} 请处理同名领地再重试")
            }

        }

    }


    fun amns() {
        val residenceInstance = ResidenceProviderRegister.residence
        if (residenceInstance == null) {
            // sender.sendMessage("${ResidenceBridge.pluginPrefix} 未安装 Residence")
            return
        }

        val serverName = ConfigurationYAML.serverName

        val localResidenceInfos = mutableListOf<ResidenceInfo>()
        val map = residenceInstance.residenceManager.residences
        for ((key, claimedResidence) in map) {
            localResidenceInfos.add(
                ResidenceInfo(
                    claimedResidence.residenceName,
                    claimedResidence.ownerUUID,
                    claimedResidence.owner,
                    claimedResidence.permissions.flags,
                    claimedResidence.permissions.playerFlags,
                    serverName
                )
            )
        }

        val globalResidenceInfos = ResidenceMySQL.selectResidences()

        val duplicates = mutableListOf<ResidenceInfo>()
        localResidenceInfos.forEach { localResidenceInfo ->
            globalResidenceInfos.forEach { globalResidenceInfo ->
                if (localResidenceInfo.residenceName == globalResidenceInfo.residenceName && localResidenceInfo.serverName != globalResidenceInfo.serverName) {
                    duplicates.add(localResidenceInfo)
                }
            }
        }

        // duplicates
    }

}