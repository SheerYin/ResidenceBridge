package me.yin.residencebridge.provider.register

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import me.yin.residencebridge.ResidenceBridge
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
                return ""
                // ResidenceInfoMySQL.getOwnerResidenceNames(player.uniqueId).size.toString()
            }

            parameters.equals("maximum", ignoreCase = true) -> {
                return ""
                // Limit.numberPermissions(player).toString()
            }

            else -> {
                return null
            }
        }
    }
}