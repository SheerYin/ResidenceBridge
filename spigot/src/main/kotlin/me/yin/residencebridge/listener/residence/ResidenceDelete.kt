package me.yin.residencebridge.listener.residence

import com.bekvon.bukkit.residence.event.ResidenceDeleteEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.yin.residencebridge.other.AllRepository
import me.yin.residencebridge.other.DatabaseManager
import me.yin.residencebridge.other.AllCache
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class ResidenceDelete(val databaseManager: DatabaseManager, val allRepository: AllRepository, val scope: CoroutineScope, val allCache: AllCache) : Listener {

    // 领地被删除 前 时才会触发
    @EventHandler
    fun onResidenceDelete(event: ResidenceDeleteEvent) {

        // Bukkit.broadcastMessage("触发 onResidenceDelete")

        // val playerUuid = event.residence.ownerUUID
        val residenceName = event.residence.residenceName

        scope.launch {
            databaseManager.dataSource.connection.use { connection ->
                allRepository.deleteResidence(connection, residenceName)
            }
        }

        allCache.onResidenceDelete(residenceName)
    }
}
