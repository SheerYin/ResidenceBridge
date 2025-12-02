package me.yin.residencebridge.listener.residence

import com.bekvon.bukkit.residence.Residence
import com.bekvon.bukkit.residence.event.ResidenceFlagChangeEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.yin.residencebridge.ResidenceBridge
import me.yin.residencebridge.configuration.MainConfiguration
import me.yin.residencebridge.other.AllCache
import me.yin.residencebridge.other.AllRepository
import me.yin.residencebridge.other.DatabaseManager
import me.yin.residencebridge.other.IReadOnlyResidence
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.*

class ResidenceFlagChange(
    val residenceBridge: ResidenceBridge,
    val residenceInstance: Residence,
    val mainConfiguration: MainConfiguration,
    val databaseManager: DatabaseManager,
    val allRepository: AllRepository,
    val scope: CoroutineScope,
    val allCache: AllCache
) : Listener {

    var mark = hashSetOf<String>()

    val bukkitTask = residenceBridge.server.scheduler.runTaskTimer(
        residenceBridge,
        Runnable { clearMark() },
        100L,
        1200L
    )

    @EventHandler
    fun onResidenceFlagChange(event: ResidenceFlagChangeEvent) {
        // 某些 flag 变化不是玩家触发的，而是系统
        val claimedResidence = event.residence ?: return
        val player = event.player ?: return
        val residenceName = claimedResidence.residenceName ?: return

        mark.add(residenceName)
    }

    // 每60s同步调用一次
    fun clearMark() {
        if (mark.isEmpty()) {
            return
        }

        val temporary = mark
        mark = hashSetOf()

        val residences = arrayListOf<IReadOnlyResidence>()
        temporary.forEach { residenceName ->
            val claimedResidence = residenceInstance.residenceManager.getByName(residenceName) ?: return@forEach
            val permissions = claimedResidence.permissions

            val residenceFlags = HashMap(permissions.flags)
            val playerFlags = hashMapOf<UUID, HashMap<String, Boolean>>()
            permissions.playerFlags.forEach { entry ->
                playerFlags[entry.key] = HashMap(entry.value)
            }

            val residence = AllCache.Residence(
                claimedResidence.residenceName,
                claimedResidence.ownerUUID,
                residenceFlags,
                playerFlags,
                mainConfiguration.simpleConfiguration.serverName
            )

            allCache.onResidenceFlagChange(residence)

            residences.add(residence)
        }


        scope.launch {
            databaseManager.dataSource.connection.use { connection ->
                allRepository.updateResidencesFlags(connection, residences)
            }
        }

    }







//    // 鬼屎频繁更新，现在换成定时更新
//
//    // Flags 被修改 前 时才会触发
//    // 同时 ResidenceCreation 也会触发，因为创建领地时同时在初始化 Flags
//    // 但领地没创建完之前，获取的 residenceName 都是 null
//    @EventHandler
//    fun onResidenceFlagChange(event: ResidenceFlagChangeEvent) {
//
////        Bukkit.broadcastMessage("触发 onResidenceFlagChange")
////        val ft = event.flagType
////        val flag = event.flag
////        val e = event.message
////        val flagTargetPlayerOrGroup = event.flagTargetPlayerOrGroup
////        val newState = event.newState
////
////        val broadcastMessage = """
////    §e--- 触发 onResidenceFlagChange ---
////    §fFlag Type: §a$ft
////    §fFlag: §a$flag
////    §fMessage: §a$e
////    §fTarget: §a$flagTargetPlayerOrGroup
////    §fNew State: §a$newState
////""".trimIndent()
////        Bukkit.broadcastMessage(broadcastMessage)
//
//        // 不考虑兼容 group flags
//
//
//        val res = event.residence
//        val residenceName = res.residenceName ?: return
//
//        val player = event.player ?: return
//        val playerUuid = player.uniqueId
//
//
//        val flag = event.flag
//
//        var v: Boolean? = null
//        val flagState = event.newState
//        if (flagState == FlagPermissions.FlagState.TRUE) {
//            v = true
//        } else if (flagState == FlagPermissions.FlagState.FALSE) {
//            v = false
//        }
//
//        val flagType = event.flagType
//
//
//        if (flagType == ResidenceFlagEvent.FlagType.RESIDENCE) {
//            allCache.onResidencePlayerFlagChange(residenceName, flag, v ?: false)
//        } else if (flagType == ResidenceFlagEvent.FlagType.PLAYER) {
//            allCache.onResidencePlayerFlagChange(residenceName, playerUuid, flag, v ?: false)
//        }
//
//        val dataSource = databaseManager.dataSource
//        scope.launch {
//            when (event.flagType) {
//                ResidenceFlagEvent.FlagType.RESIDENCE -> {
//                    if (v == null) {
//                        dataSource.connection.use { connection -> allRepository.updateRemoveResidenceFlags(connection, residenceName, flag) }
//                    } else {
//                        dataSource.connection.use { connection -> allRepository.updateSetResidenceFlags(connection, residenceName, flag, v) }
//                    }
//                }
//
//                ResidenceFlagEvent.FlagType.PLAYER -> {
//                    if (v == null) {
//                        dataSource.connection.use { connection -> allRepository.updateRemovePlayerFlags(connection, residenceName, playerUuid, flag) }
//                    } else {
//                        dataSource.connection.use { connection -> allRepository.updateSetPlayerFlags(connection, residenceName, playerUuid, flag, v) }
//                    }
//
//                }
//
//                else -> {}
//            }
//        }
//    }


}
