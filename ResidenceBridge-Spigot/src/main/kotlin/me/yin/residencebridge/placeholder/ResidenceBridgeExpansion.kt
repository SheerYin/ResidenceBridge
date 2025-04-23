package me.yin.residencebridge.placeholder

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import me.yin.residencebridge.ResidenceBridge
import me.yin.residencebridge.persistence.ResidenceMySQL
import me.yin.residencebridge.provider.register.ResidenceProviderRegister
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.Plugin

class ResidenceBridgeExpansion(plugin: Plugin) : PlaceholderExpansion() {

    override fun getIdentifier(): String {
        return ResidenceBridge.lowercaseName
    }

    override fun getAuthor(): String {
        return ResidenceBridge.pluginAuthors.joinToString(",")
    }

    override fun getVersion(): String {
        return ResidenceBridge.pluginVersion
    }

    override fun persist(): Boolean {
        return true
    }

    override fun onRequest(offlinePlayer: OfflinePlayer, parameters: String): String? {
        val playerName = offlinePlayer.name ?: return null
        when {
            parameters.startsWith("names", ignoreCase = true) -> {
                val names = ResidenceMySQL.selectOwnerResidenceNames(playerName)
                if (names.isEmpty()) {
                    return null
                }
                return names.joinToString(",")
            }

            parameters.startsWith("infos", ignoreCase = true) -> {
                val residenceInfos = ResidenceMySQL.selectOwnerResidences(playerName)
                val list = mutableListOf<String>()
                for (residenceInfo in residenceInfos) {
                    list.add(residenceInfo.residenceName + ":" + residenceInfo.serverName)
                }
                if (list.isEmpty()) {
                    return null
                }
                return list.joinToString(",")
            }

            parameters.startsWith("amount", ignoreCase = true) -> {
                val amount = ResidenceMySQL.selectOwnerResidencesCount(playerName)
                return amount.toString()
            }

            parameters.startsWith("maximum", ignoreCase = true) -> {
                val residenceInstance = ResidenceProviderRegister.residence
                if (residenceInstance == null) {
                    return null
                }
                return residenceInstance.playerManager.getMaxResidences(playerName).toString()
            }

            else -> {
                return null
            }
        }
    }
}