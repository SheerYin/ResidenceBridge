package me.yin.residencebridge.listener

import me.yin.residencebridge.other.AllRepository
import me.yin.residencebridge.other.DatabaseManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent

class AsyncPlayerJoin(val databaseManager: DatabaseManager, val allRepository: AllRepository) : Listener {

    @EventHandler
    fun onAsyncPlayerJoin(event: AsyncPlayerPreLoginEvent) {
        val result = event.loginResult
        if (result == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            // 阻塞
            databaseManager.dataSource.connection.use { connection ->
                allRepository.updateInsertPlayer(connection, AllRepository.Player(event.uniqueId, event.name))
            }
        }
    }


//    @EventHandler
//    fun onPlayerJoin(event: PlayerJoinEvent) {
//        val player = event.player
//
//        scope.launch {
//            allRepository.updateInsertPlayer(AllRepository.Player(player.uniqueId, player.name))
//        }
//
////        scope.launch {
////            try {
////                val success = allRepository.updateInsertPlayer(AllRepository.Player(playerUuid, playerName))
////                if (success) {
////                    // 可以添加调试信息
////                    // player.server.logger.info("已更新玩家 ${playerName} 的名字")
////                } else {
////                    // 更新失败，可能是玩家不存在于数据库中
////                    // player.server.logger.info("玩家 ${playerName} 名字更新失败，可能是新玩家")
////                }
////            } catch (exception: Exception) {
////                exception.printStackTrace()
////                player.server.logger.warning("更新玩家 ${playerName} 名字时发生错误: ${exception.message}")
////            }
////        }
//    }
} 