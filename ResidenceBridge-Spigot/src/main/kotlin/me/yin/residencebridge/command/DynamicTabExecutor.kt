package me.yin.residencebridge.command

import me.yin.residencebridge.ResidenceBridge
import me.yin.residencebridge.command.dynamic.*
import me.yin.residencebridge.persistence.ResidenceMySQL
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor

object DynamicTabExecutor : TabExecutor {

    val mainPermission: String by lazy { "${ResidenceBridge.lowercaseName}.command" }

    override fun onCommand(sender: CommandSender, command: Command, label: String, arguments: Array<out String>): Boolean {
        if (!permissionMessage(sender, mainPermission)) {
            return true
        }
        when {
            arguments.isEmpty() -> {
                Help.dynamic(sender)
            }

            arguments.size == 1 -> {
                when {
                    arguments[0] == "help" -> {
                        Help.dynamic(sender)
                    }

                    arguments[0] == "list" -> {
                        ListResidence.dynamic(sender)
                    }

                    arguments[0] == "listall" -> {
                        ListallResidence.dynamic(sender)
                    }

                    arguments[0] == "import" -> {
                        ImportResidence.dynamic(sender)
                    }
                }
            }

            arguments.size == 2 -> {
                if (arguments[0] == "list") {
                    ListResidence.dynamic(sender, arguments[1])
                } else if (arguments[0] == "teleport") {
                    Teleport.dynamic(sender, arguments[1])
                }
            }

            arguments.size == 3 -> {
                if (arguments[0] == "teleport") {
                    Teleport.dynamic(sender, arguments[1], arguments[2])
                }
            }
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, arguments: Array<out String>): List<String> {
        when (arguments.size) {
            1 -> {
                return prune(arguments[0], listOf("help", "list", "listall", "teleport", "import"))
            }

            2 -> {
                if (arguments[0] == "list") {
                    val argument = arguments[1]
                    val empty = argument.isEmpty()
                    val list = mutableListOf<String>()
                    for (player in Bukkit.getOnlinePlayers()) {
                        val playerName = player.name
                        if (empty) {
                            list.add(playerName)
                        } else if (playerName.startsWith(argument, true)) {
                            list.add(playerName)
                        }
                    }
                    return list
                } else if (arguments[0] == "teleport") {
                    val names = ResidenceMySQL.selectResidenceNames()
                    return prune(arguments[1], names)
                }
            }

            3 -> {
                if (arguments[0] == "teleport") {
                    val argument = arguments[2]
                    val empty = argument.isEmpty()
                    val list = mutableListOf<String>()
                    for (player in Bukkit.getOnlinePlayers()) {
                        val playerName = player.name
                        if (empty) {
                            list.add(playerName)
                        } else if (playerName.startsWith(argument, true)) {
                            list.add(playerName)
                        }
                    }
                    return list
                }
            }
        }
        return emptyList()
    }

    fun prune(argument: String, suggest: List<String>): List<String> {
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
