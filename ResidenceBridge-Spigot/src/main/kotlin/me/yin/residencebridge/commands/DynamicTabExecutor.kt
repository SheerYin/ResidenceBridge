package me.yin.residencebridge.commands

import me.yin.residencebridge.commands.dynamic.Help
import me.yin.residencebridge.commands.dynamic.Teleport
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

object DynamicTabExecutor : TabExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, arguments: Array<out String>): Boolean {
        when {
            arguments.isEmpty() -> {
                Help.dynamic(sender)
            }

            arguments.size == 1 -> {
                when {
                    arguments[0] == "help" -> {
                        Help.dynamic(sender)
                    }

                    arguments[0] == "teleport" -> {
                        val player: Player
                        if (sender is Player) {
                            player = sender
                        } else {
                            return true
                        }

                        Teleport.dynamic(player, "")
                    }
                }
            }

        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, arguments: Array<out String>): List<String> {
        when (arguments.size) {
            1 -> {
                return prune(arguments[0], "help", "list", "listall", "teleport", "import", "reload")
            }
        }
        return emptyList()
    }


    private fun prune(argument: String, vararg suggest: String): List<String> {
        return suggest.filter { it.startsWith(argument, ignoreCase = true) }
    }


}
