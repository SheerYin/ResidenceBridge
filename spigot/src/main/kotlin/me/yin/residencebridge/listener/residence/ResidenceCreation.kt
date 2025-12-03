package me.yin.residencebridge.listener.residence

import com.bekvon.bukkit.residence.event.ResidenceCreationEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.yin.residencebridge.ResidenceBridge
import me.yin.residencebridge.configuration.MainConfiguration
import me.yin.residencebridge.other.AllCache
import me.yin.residencebridge.other.AllRepository
import me.yin.residencebridge.other.DatabaseManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.*

class ResidenceCreation(
    val residenceBridge: ResidenceBridge,
    val mainConfiguration: MainConfiguration,
    val databaseManager: DatabaseManager,
    val allRepository: AllRepository,
    val scope: CoroutineScope,
    val allCache: AllCache
) : Listener {

    // 领地创建 前 触发
    @EventHandler
    fun onResidenceCreation(event: ResidenceCreationEvent) {

//        Bukkit.broadcastMessage("触发 onResidenceCreation")

        val residence = event.residence
        val residenceName = residence.residenceName

        // 阻塞
        // 防 residence 弱智行为
        // 暂时取消，防止阻塞
        // 但是有未知行为
//        databaseManager.dataSource.connection.use { connection ->
//            if (allRepository.selectResidenceName(connection, residenceName)) {
//                event.isCancelled = true
//                residenceBridge.logger.info("命令没拦截的领地 $residenceName 玩家 ${residence.owner} 尝试创建，为了保证不重名强行阻止")
//                return
//            }
//        }


        val ownerUuid = residence.ownerUUID
        val ownerName = residence.owner
        val permissions = residence.permissions

        val residenceFlags = HashMap(permissions.flags)
        val playerFlags = hashMapOf<UUID, HashMap<String, Boolean>>()
        permissions.playerFlags.forEach {
            playerFlags[it.key] = HashMap(it.value)
        }

        val serverName = mainConfiguration.simpleConfiguration.serverName

        scope.launch {
            databaseManager.dataSource.connection.use { connection ->
                allRepository.insertResidence(connection, AllRepository.Residence(residenceName, ownerUuid, residenceFlags, playerFlags, serverName))
            }
        }

        allCache.onResidenceCreation(AllCache.Player(ownerUuid, ownerName), AllCache.Residence(residenceName, ownerUuid, residenceFlags, playerFlags, serverName))
    }

}

