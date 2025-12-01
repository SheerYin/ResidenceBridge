package me.yin.residencebridge.message

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.command.CommandSender

class SimpleMessage(val bukkitAudiences: BukkitAudiences, val miniMessage: MiniMessage) {

    fun sendMessage(sender: CommandSender, message: String, tagResolver: TagResolver? = null) {
        if (message.isEmpty()) return
        val audience = bukkitAudiences.sender(sender)

        if (tagResolver == null) {
            audience.sendMessage(miniMessage.deserialize(message))
            return
        }
        audience.sendMessage(miniMessage.deserialize(message, tagResolver))
    }

    fun sendMessage(audience: Audience, message: String, tagResolver: TagResolver? = null) {
        if (message.isEmpty()) return

        if (tagResolver == null) {
            audience.sendMessage(miniMessage.deserialize(message))
            return
        }
        audience.sendMessage(miniMessage.deserialize(message, tagResolver))
    }

}