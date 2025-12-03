package me.yin.residencebridge.listener.residence

import com.bekvon.bukkit.residence.Residence
import com.bekvon.bukkit.residence.event.ResidenceCommandEvent
import me.yin.residencebridge.command.DynamicTabExecutor
import me.yin.residencebridge.configuration.MessageConfiguration
import me.yin.residencebridge.message.SimpleMessage
import me.yin.residencebridge.other.AllRepository
import me.yin.residencebridge.other.DatabaseManager
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class ResidenceCommand(
    val residenceInstance: Residence,
    val databaseManager: DatabaseManager,
    val allRepository: AllRepository,
    val simpleMessage: SimpleMessage,
    val messageConfiguration: MessageConfiguration,
    val dynamicTabExecutor: DynamicTabExecutor
) : Listener {

    @EventHandler
    fun onResidenceCommand(event: ResidenceCommandEvent) {
        val sender = event.sender
        val command = event.command.lowercase()
        val arguments = event.args

        if (command in listOf("residence", "res")) {
            when (arguments.size) {
                1 -> {
                    if (arguments[0] == "list") {
                        event.isCancelled = true
                        dynamicTabExecutor.executeList(sender)
                    }
                    if (arguments[0] == "listall") {
                        event.isCancelled = true
                        dynamicTabExecutor.executeListAll(sender)
                    }
                }

                2 -> {
                    when {
                        arguments[0].lowercase() == "tp" -> {
                            val residenceName = arguments[1]
                            val claimedResidence = residenceInstance.residenceManager.getByName(residenceName)
                            if (claimedResidence == null) {
                                event.isCancelled = true
                                dynamicTabExecutor.executeTeleport(sender, residenceName)
                            }
                        }

                        arguments[0].lowercase() == "create" -> {
                            val audience = simpleMessage.bukkitAudiences.sender(sender)
                            if (sender !is Player) {
                                val s = messageConfiguration.message.onlyPlayer
                                simpleMessage.sendMessage(audience, s)
                                return
                            }

                            val residenceName = arguments[1]
                            // 阻塞
                            databaseManager.dataSource.connection.use { connection ->
                                if (allRepository.selectResidenceName(connection, residenceName)) {
                                    event.isCancelled = true
                                    val s = messageConfiguration.message.createSection.nameExists
                                    simpleMessage.sendMessage(audience, s, Placeholder.unparsed("residence", residenceName))
                                } else {
                                    val groupName = residenceInstance.permissionManager.getPermissionsGroup(sender)
                                    val group = residenceInstance.permissionManager.getGroupByName(groupName)
                                    val maximum = group.maxZones

                                    val count = allRepository.selectPlayerResidencesCount(connection, sender.uniqueId) ?: 0
                                    // 当已有数量 >= 上限时，禁止继续创建
                                    if (count >= maximum) {
                                        event.isCancelled = true
                                        val s = messageConfiguration.message.createSection.limit
                                        val tagResolver = TagResolver.resolver(
                                            Placeholder.unparsed("count", count.toString()),
                                            Placeholder.unparsed("maximum", maximum.toString())
                                        )
                                        simpleMessage.sendMessage(audience, s, tagResolver)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (command == "resadmin") {
            // 防管理员
            if (arguments.size == 2 && arguments[0] == "create") {
                val residenceName = arguments[1]

                val audience = simpleMessage.bukkitAudiences.sender(sender)
                // 阻塞
                databaseManager.dataSource.connection.use { connection ->
                    if (allRepository.selectResidenceName(connection, residenceName)) {
                        event.isCancelled = true
                        val s = messageConfiguration.message.createSection.nameExists
                        simpleMessage.sendMessage(audience, s, Placeholder.unparsed("residence", residenceName))
                    }
                }
            }
        }
    }
}
