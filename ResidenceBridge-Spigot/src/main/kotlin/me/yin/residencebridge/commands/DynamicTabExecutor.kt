package me.yin.residencebridge.commands

import me.yin.residencebridge.ResidenceBridge
import me.yin.residencebridge.commands.dynamic.*
import me.yin.residencebridge.storage.ResidenceStorage
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

object DynamicTabExecutor : TabExecutor {

    val mainPermission: String by lazy { "${ResidenceBridge.lowercaseName}.command" }

    override fun onCommand(sender: CommandSender, command: Command, label: String, arguments: Array<out String>): Boolean {
        if (!permissionMessage(sender, mainPermission)) {
            return true
        }
        when {
            arguments.isEmpty() -> {
                Help.dynamic(sender, mainPermission)
            }

            arguments.size == 1 -> {
                when {
                    arguments[0] == "help" -> {
                        Help.dynamic(sender, mainPermission)
                    }

                    arguments[0] == "list" -> {
                        val player = sender as? Player
                        if (player == null) {
                            sender.sendMessage(ResidenceBridge.pluginPrefix + " 此命令仅限玩家执行")
                            return true
                        }
                        ListResidence.dynamic(player, mainPermission)
                    }

                    arguments[0] == "listall" -> {
                        ListallResidence.dynamic(sender, mainPermission)
                    }

                    arguments[0] == "import" -> {
                        ImportResidence.dynamic(sender, mainPermission)
                    }
                }
            }

            arguments.size == 2 -> {
                if (arguments[0] == "list") {
                    ListResidence.dynamic(sender, arguments[1], mainPermission)
                } else
                    if (arguments[0] == "teleport") {
                        val player = sender as? Player
                        if (player == null) {
                            sender.sendMessage(ResidenceBridge.pluginPrefix + " 此命令仅限玩家执行")
                            return true
                        }
                        Teleport.dynamic(player, mainPermission, arguments[1])
                    }
            }
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, arguments: Array<out String>): List<String> {
        when (arguments.size) {
            1 -> {
                return prune(arguments[0], "help", "list", "listall", "teleport", "import")
            }

            2 -> {
                if (arguments[0] == "list") {
                    val s = arguments[1]
                    return ResidenceBridge.bukkitServer.onlinePlayers.mapNotNull { player ->
                        player.name.takeIf { arguments.isEmpty() || it.startsWith(s, ignoreCase = true) }
                    }
                } else
                    if (arguments[0] == "teleport") {
                        val player = sender as? Player ?: return emptyList()
                        val names = ResidenceStorage.selectOwnerResidenceNames(player.uniqueId)
                        return prune(arguments[1], names)
                    }
            }
        }
        return emptyList()
    }

    private fun prune(argument: String, vararg suggest: String): List<String> {
        if (argument.isEmpty()) {
            return suggest.toList()
        } else {
            return suggest.filter { it.startsWith(argument, ignoreCase = true) }
        }
    }

    private fun prune(argument: String, suggest: List<String>): List<String> {
        if (argument.isEmpty()) {
            return suggest
        } else {
            return suggest.filter { it.startsWith(argument, ignoreCase = true) }
        }
    }

    fun permissionMessage(sender: CommandSender, permission: String): Boolean {
        if (sender.hasPermission(permission)) {
            return true
        }
        sender.sendMessage("${ResidenceBridge.pluginPrefix} 您没有权限 $permission")
        return false
    }


}
