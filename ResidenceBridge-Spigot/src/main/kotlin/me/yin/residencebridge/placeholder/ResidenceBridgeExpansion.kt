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
        val player = (offlinePlayer.player) ?: return null
        when {
            parameters.equals("amount", ignoreCase = true) -> {
                val amount = ResidenceMySQL.selectOwnerResidencesCount(player.uniqueId)
                return amount.toString()
            }

            parameters.equals("maximum", ignoreCase = true) -> {
                val residenceInstance = ResidenceProviderRegister.residence
                if (residenceInstance == null) {
                    return ""
                }
                return residenceInstance.playerManager.getMaxResidences(player.name).toString()
            }
            else -> {
                return null
            }
        }
    }
}