package me.yin.residencebridge.listeners.residence

import com.bekvon.bukkit.residence.event.ResidenceCommandEvent
import kotlinx.coroutines.launch
import me.yin.residencebridge.ResidenceBridge
import me.yin.residencebridge.provider.register.ResidenceProviderRegister
import me.yin.residencebridge.service.ResidenceTeleport
import me.yin.residencebridge.storage.ResidenceStorage
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object ResidenceCommand : Listener {

    @EventHandler
    fun onResidenceCommand(event: ResidenceCommandEvent) {

        val player = event.sender as? Player ?: return

        val command = event.command.lowercase()
        val arguments = event.args

        if (command in listOf("residence", "res")) {
            when (arguments.size) {
                1 -> {
                    if (arguments[0] == "list") {
                        event.isCancelled = true

                        ResidenceBridge.scope.launch {
                            val names = ResidenceStorage.selectOwnerResidenceNames(player.uniqueId)
                            player.sendMessage("${ResidenceBridge.pluginPrefix} 玩家 §2${player.name}§f 领地列表")
                            for (name in names) {
                                player.sendMessage("${ResidenceBridge.pluginPrefix} 领地 $name")
                            }
                        }
                    }
                }

                2 -> {
                    when {
                        arguments[0].lowercase() == "tp" -> {
                            val residenceName = arguments[1]
                            if (ResidenceProviderRegister.residence.residenceManager.getByName(residenceName) != null) {
                                return // 本地存在领地
                            }
                            event.isCancelled = true

                            ResidenceBridge.scope.launch {
                                val residenceInfo = ResidenceStorage.selectResidence(residenceName)
                                if (residenceInfo == null) {
                                    player.sendMessage("${ResidenceBridge.pluginPrefix} 领地不存在")
                                    return@launch
                                }
                                val playerUUID = player.uniqueId
                                if (player.hasPermission("residence.admin.tp") || residenceInfo.ownerUUID == playerUUID || residenceInfo.residenceFlags["tp"] == true || residenceInfo.playerFlags[playerUUID.toString()]?.get("tp") == true) {
                                    ResidenceTeleport.global(player, residenceName, residenceInfo.serverName)
                                    player.sendMessage("${ResidenceBridge.pluginPrefix} 开始传送")
                                } else {
                                    player.sendMessage("${ResidenceBridge.pluginPrefix} 你没有权限传送这个领地")
                                }
                            }
                        }

                        arguments[0].lowercase() == "create" -> {
                            if (ResidenceStorage.isResidenceExists(arguments[1])) {
                                player.sendMessage("${ResidenceBridge.pluginPrefix} 领地重名")
                                event.isCancelled = true
                            } else {
                                val maximum = ResidenceProviderRegister.residence.playerManager.getMaxResidences(player.name)
                                val count = ResidenceStorage.selectOwnerResidencesCount(player.uniqueId)
                                if (count >= maximum) {
                                    player.sendMessage("${ResidenceBridge.pluginPrefix} 创建领地已达上限 $count / $maximum")
                                    event.isCancelled = true
                                }
                            }
                        }
                    }
                }
            }
        } else if (command == "resadmin") {
            // 防管理员
            if (arguments.size == 2 && arguments[0] == "create") {
                if (ResidenceStorage.isResidenceExists(arguments[1])) {
                    player.sendMessage("${ResidenceBridge.pluginPrefix} 领地重名")
                    event.isCancelled = true
                }
            }
        }
    }

//    @EventHandler
//    fun onResidenceCommand(event: ResidenceCommandEvent) {
//
////        Bukkit.broadcastMessage("触发 onResidenceCommand")
//
//        val player = event.sender as? Player ?: return
//
//        val command = event.command.lowercase()
//        val arguments = event.args
//
//        if (command in listOf("residence", "res")) {
//            when (arguments.size) {
//                1 -> {
//                    val argument = arguments[0].lowercase()
//                    if (argument == "list") {
//                        listOne(player, event)
//                    }
//                }
//                2 -> {
//                    when {
//                        arguments[0].lowercase() == "tp" -> {
//                            teleport(player, arguments[1], event)
//                        }
//                        arguments[0].lowercase() == "create" -> {
//                            val names = ResidenceStorage.getResidenceNames().map { it.lowercase() }
//                            if (arguments[1].lowercase() in names) {
//                                // player.sendMessage(MessageYAML.configuration.getString("command.create-name-already-exists"))
//                                event.isCancelled = true
//                                return
//                            }
//                            val self = ResidenceStorage.getOwnerResidenceNames(player.uniqueId)
//                            // 玩家领地总数有没有大于 config 的 residences.amount 权限
//                            val number = Limit.numberPermissions(player)
//                            if (self.size >= number) {
//                                event.isCancelled = true
//                                player.sendMessage(
//                                    IndexReplace.replace(
//                                        MessageYAML.configuration.getString("command.create-amount-limit")!!,
//                                        (self.size).toString(),
//                                        number.toString()
//                                    )
//                                )
//                                return
//                            }
//                        }
//                    }
//                }
//            }
//        } else if (command == "resadmin") {
//            // 防管理员
//            if (arguments.size == 2 && arguments[0] == "create") {
//                val names = ResidenceStorage.getResidenceNames().map { it.lowercase() }
//                if (arguments[1].lowercase() in names) {
//                    player.sendMessage(MessageYAML.configuration.getString("command.create-name-already-exists"))
//                    event.isCancelled = true
//                    return
//                }
//            }
//        }
//    }
//
//
//    private fun listOne(player: Player, event: ResidenceCommandEvent) {
//        val playerName = player.name
//
//        val names = ResidenceInfoMySQL.getOwnerResidenceNames(playerName)
//        if (names.isEmpty()) {
//            return
//        }
//
//        event.isCancelled = true
//        ResidencePage.playerPage[playerName] = ResidencePage.split(names.sortedBy {
//            it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0
//        }, 10)
//
//
//        val list = ResidencePage.playerPage[playerName]
//        if (list == null) {
//            player.sendMessage(
//                IndexReplace.replace(
//                    MessageYAML.configuration.getString("command.player-page-no-residence")!!,
//                    playerName
//                )
//            )
//            return
//        }
//
//        player.sendMessage(MessageYAML.configuration.getString("command.player-page-header"))
//        for (name in list[0]) {
//            val text = IndexReplace.replace(
//                MessageYAML.configuration.getString("command.player-page-list")!!,
//                playerName,
//                name
//            )
//            val baseComponents = ComponentSerializer.parse(text)
//            player.spigot().sendMessage(*baseComponents)
//        }
//
//        val text = IndexReplace.replace(
//            MessageYAML.configuration.getString("command.player-page-footer")!!,
//            playerName,
//            "1",
//            "0",
//            "2",
//            (list.size).toString()
//        )
//        val baseComponents = ComponentSerializer.parse(text)
//        player.spigot().sendMessage(*baseComponents)
//    }
//
//
//    private fun teleport(player: Player, residenceName: String, event: ResidenceCommandEvent) {
//
//        val claimedResidence = Residence.getInstance().residenceManager.residences[residenceName.lowercase()]
//        if (claimedResidence != null) {
//            val a = claimedResidence.permissions.flags["tp"]
//            val b = claimedResidence.permissions.playerFlags[player.uniqueId.toString()]?.get("tp")
//            if (claimedResidence.owner == player.name || a == true || b == true) {
//                event.isCancelled = true
//                player.teleport(claimedResidence.getTeleportLocation(player, true), PlayerTeleportEvent.TeleportCause.PLUGIN)
//                return
//            }
//        }
//
//        val residenceInfo = ResidenceInfoMySQL.getResidence(residenceName) ?: return
//
//        val ownerUUID = player.uniqueId
//        if (residenceInfo.ownerUUID == ownerUUID || residenceInfo.residenceFlags["tp"] == true || residenceInfo.playerFlags[ownerUUID.toString()]?.get("tp") == true) {
//            event.isCancelled = true
//            ResidenceStorage.scope.launch {
//                val serverName = residenceInfo.serverName
//                SendByte.teleport(player, player.name, serverName, residenceName)
//            }
//        }
//    }


}
