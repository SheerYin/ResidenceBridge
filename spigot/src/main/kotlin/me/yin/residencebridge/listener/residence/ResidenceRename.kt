package me.yin.residencebridge.listener.residence

import com.bekvon.bukkit.residence.event.ResidenceRenameEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.yin.residencebridge.configuration.MessageConfiguration
import me.yin.residencebridge.message.SimpleMessage
import me.yin.residencebridge.other.AllCache
import me.yin.residencebridge.other.AllRepository
import me.yin.residencebridge.other.DatabaseManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class ResidenceRename(
    val databaseManager: DatabaseManager,
    val allRepository: AllRepository,
    val scope: CoroutineScope,
    val allCache: AllCache,
    val simpleMessage: SimpleMessage,
    val messageConfiguration: MessageConfiguration
) : Listener {


    // 领地被更改名称 前 时才会触发
    @EventHandler
    fun onResidenceRename(event: ResidenceRenameEvent) {
//        Bukkit.broadcastMessage("触发 onResidenceRename")
        val oldName = event.oldResidenceName
        val newName = event.newResidenceName

        val player = event.residence.rPlayer.player

        val dataSource = databaseManager.dataSource
        // 阻塞
        val conflict = dataSource.connection.use { connection ->
            allRepository.selectResidenceName(connection, newName)
        }

        if (conflict) {
            event.isCancelled = true
            val s = messageConfiguration.message.createSection.nameExists
            simpleMessage.sendMessage(player, s)
            return
        } else {
            scope.launch {
                databaseManager.dataSource.connection.use { connection ->
                    allRepository.updateResidenceName(connection, oldName, newName)
                }
            }
        }

        allCache.onResidenceRename(oldName, newName)
    }
}
