package me.yin.residencebridge.commands.dynamic

import kotlinx.coroutines.launch
import me.yin.residencebridge.ResidenceBridge
import me.yin.residencebridge.commands.DynamicTabExecutor
import me.yin.residencebridge.model.ResidenceInfo
import me.yin.residencebridge.provider.register.ResidenceProviderRegister
import me.yin.residencebridge.storage.ResidenceStorage
import org.bukkit.command.CommandSender

object ImportResidence {

    private val mainParameter = "import"

    fun dynamic(sender: CommandSender, mainPermission: String) {
        if (!DynamicTabExecutor.permissionMessage(sender, "$mainPermission.$mainParameter")) {
            return
        }

        ResidenceBridge.scope.launch {
            val map = ResidenceProviderRegister.residence.residenceManager.residences

            val localNames = mutableListOf<String>()
            val globalNames = ResidenceStorage.selectResidenceNames()

            val serverName = ResidenceBridge.serverName
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
                ResidenceStorage.batchInsertResidences(residenceInfos)
                sender.sendMessage(ResidenceBridge.pluginPrefix + " 导入完成")
            } else {
                sender.sendMessage(ResidenceBridge.pluginPrefix + " 数据库中已有重名领地 " + duplicates)
                sender.sendMessage(ResidenceBridge.pluginPrefix + " 请处理同名领地再重试")
            }

        }

    }


    fun amns() {
        val serverName = ResidenceBridge.serverName

        val localResidenceInfos = mutableListOf<ResidenceInfo>()
        val map = ResidenceProviderRegister.residence.residenceManager.residences
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

        val globalResidenceInfos = ResidenceStorage.selectResidences()

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