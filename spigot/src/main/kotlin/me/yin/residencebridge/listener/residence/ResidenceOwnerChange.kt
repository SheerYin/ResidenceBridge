package me.yin.residencebridge.listener.residence

import com.bekvon.bukkit.residence.event.ResidenceOwnerChangeEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.yin.residencebridge.other.AllCache
import me.yin.residencebridge.other.AllRepository
import me.yin.residencebridge.other.DatabaseManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class ResidenceOwnerChange(
    val databaseManager: DatabaseManager,
    val allRepository: AllRepository,
    val scope: CoroutineScope,
    val allCache: AllCache
) : Listener {

    // 领地被更改所有者 前 时才会触发
    @EventHandler
    fun onResidenceOwnerChange(event: ResidenceOwnerChangeEvent) {
        val newOwnerUuid = event.newOwnerUuid ?: return
        val claimedResidence = event.residence
        val residenceName = claimedResidence.residenceName

        scope.launch {
            databaseManager.dataSource.connection.use { connection ->
                allRepository.updateResidencePlayerUuid(connection, residenceName, newOwnerUuid)
            }
        }

        allCache.onResidenceOwnerChange(residenceName, newOwnerUuid)
    }


}
