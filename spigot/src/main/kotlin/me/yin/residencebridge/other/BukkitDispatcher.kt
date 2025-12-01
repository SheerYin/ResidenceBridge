package me.yin.residencebridge.other

import kotlinx.coroutines.CoroutineDispatcher
import me.yin.residencebridge.ResidenceBridge
import kotlin.coroutines.CoroutineContext

class BukkitDispatcher(private val residenceBridge: ResidenceBridge) : CoroutineDispatcher() {
    val server = residenceBridge.server

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (server.isPrimaryThread) {
            block.run()
        } else {
            server.scheduler.runTask(residenceBridge, block)
        }
    }
}