package me.yin.residencebridge.placeholder

import com.bekvon.bukkit.residence.Residence
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import me.yin.residencebridge.other.AllCache
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.Plugin

class ResidenceBridgeExpansion(plugin: Plugin, val lowercaseName: String, val pluginAuthors: List<String>, val pluginVersion: String, val residenceInstance: Residence?, val allCache: AllCache) : PlaceholderExpansion() {

    override fun getIdentifier(): String {
        return lowercaseName
    }

    override fun getAuthor(): String {
        return pluginAuthors.joinToString(",")
    }

    override fun getVersion(): String {
        return pluginVersion
    }

    override fun persist(): Boolean {
        return true
    }

    override fun onRequest(offlinePlayer: OfflinePlayer, parameters: String): String? {
        when {
            parameters.equals("player_residence_names", true) -> {
                // 如果在线就有 player
                // 无需 online 判断
                val player = offlinePlayer.player ?: return ""
                val residences = allCache.fetchSortedResidencesByPlayerUuid()[player.uniqueId] ?: return ""
                if (residences.isEmpty()) {
                    return ""
                }
                return residences.joinToString(", ") { it.name }
            }

            // _player_residence_names_%index%
            parameters.startsWith("player_residence_names_") -> {
                val indexString = parameters.substringAfter("player_residence_names_")
                val index = indexString.toIntOrNull() ?: return ""

                val player = offlinePlayer.player ?: return ""
                val residences = allCache.fetchSortedResidencesByPlayerUuid()[player.uniqueId] ?: return ""

                val residence = residences.elementAtOrNull(index) ?: return ""
                return residence.name
            }

            parameters.equals("player_residence_count", true) -> {
                val player = offlinePlayer.player ?: return ""
                val residences = allCache.fetchSortedResidencesByPlayerUuid()[player.uniqueId] ?: return ""
                return residences.size.toString()
            }

            parameters.equals("player_residence_maximum", true) -> {
                if (residenceInstance == null) {
                    return ""
                } else {
                    val player = offlinePlayer.player ?: return ""

                    val permissionManager = residenceInstance.permissionManager
                    val groupName = permissionManager.getPermissionsGroup(player)
                    val group = permissionManager.getGroupByName(groupName)

                    return group.maxZones.toString()
                }
            }

            parameters.equals("server_residence_names", true) -> {
                val residences = allCache.fetchSortedResidences()
                if (residences.isEmpty()) {
                    return ""
                }
                return residences.joinToString(", ") { it.name }
            }

            // _server_residence_names_%index%
            parameters.startsWith("server_residence_names_") -> {
                val indexString = parameters.substringAfter("server_residence_names_")
                val index = indexString.toIntOrNull() ?: return ""

                val residence = allCache.fetchSortedResidences().elementAtOrNull(index) ?: return ""
                return residence.name
            }

            parameters.equals("server_residence_count", true) -> {
                val residences = allCache.fetchSortedResidences()
                return residences.size.toString()
            }

            parameters.equals("residence_public_teleport_names", true) -> {
                val residences = allCache.fetchSortedResidences()
                if (residences.isEmpty()) {
                    return ""
                }
                return residences.filter { it.residenceFlags["tp"] == true }.joinToString(", ") { it.name }
            }

            // _residence_public_teleport_names_%index%
            parameters.startsWith("residence_public_teleport_names_", true) -> {
                val indexString = parameters.substringAfter("residence_public_teleport_names_")
                val index = indexString.toIntOrNull() ?: return ""

                val residences = allCache.fetchSortedResidences()
                if (residences.isEmpty()) {
                    return ""
                }
                val f = residences.filter { it.residenceFlags["tp"] == true }
                val r = f.elementAtOrNull(index) ?: return ""
                return r.name
            }
        }

        return null
    }


}