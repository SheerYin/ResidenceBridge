package me.yin.residencebridge.commands.dynamic

import me.yin.residencebridge.ResidenceBridge
import org.bukkit.command.CommandSender

object Help {

    fun dynamic(sender: CommandSender) {
        sender.sendMessage(ResidenceBridge.pluginPrefix + " 主命令 /residencebridge 缩写 /rb")
        sender.sendMessage(ResidenceBridge.pluginPrefix + " 玩家列表 /rb list [玩家] [页]")
        sender.sendMessage(ResidenceBridge.pluginPrefix + " 所有列表 /rb listall [页]")
        sender.sendMessage(ResidenceBridge.pluginPrefix + " 传送领地 /rb teleport <玩家> <领地>")
        sender.sendMessage(ResidenceBridge.pluginPrefix + " 导入数据 /rb import")
        sender.sendMessage(ResidenceBridge.pluginPrefix + " 重新加载 /rb reload")
    }

}