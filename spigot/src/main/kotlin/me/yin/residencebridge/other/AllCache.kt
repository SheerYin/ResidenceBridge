package me.yin.residencebridge.other

import kotlinx.coroutines.*
import java.util.*

class AllCache(val databaseManager: DatabaseManager, val allRepository: AllRepository, val scope: CoroutineScope, val bukkitDispatcher: BukkitDispatcher) {

    var interval: Long = 5 * 60000 * 1_000_000
    var lastRefreshTime: Long = 0
    private var refreshJob: Job? = null

    val dataSource = databaseManager.dataSource

    private val playersByUuid = hashMapOf<UUID, Player>()
    private val residencesByName = hashMapOf<String, Residence>()

    // 加速层
    private val playersByName = hashMapOf<String, Player>()
    private val residencesSort = ArrayList<Residence>()
    private val residencesSortByPlayerUuid = hashMapOf<UUID, ArrayList<Residence>>()

    init {
        tryRefresh()
    }

    fun fetchPlayersByUuid(): Map<UUID, IReadOnlyPlayer> {
        tryRefresh()
        return playersByUuid
    }

    fun fetchPlayersByName(): Map<String, IReadOnlyPlayer> {
        tryRefresh()
        return playersByName
    }

    fun fetchResidencesByName(): Map<String, IReadOnlyResidence> {
        tryRefresh()
        return residencesByName
    }

    fun fetchSortedResidences(): List<IReadOnlyResidence> {
        tryRefresh()
        return residencesSort
    }

    fun fetchSortedResidencesByPlayerUuid(): Map<UUID, List<IReadOnlyResidence>> {
        tryRefresh()
        return residencesSortByPlayerUuid
    }

    fun fetchSortedResidencesByPlayerName(name: String): List<IReadOnlyResidence>? {
        tryRefresh()
        val player = playersByName[name] ?: return null
        return residencesSortByPlayerUuid[player.uuid]
    }

    fun tryRefresh(force: Boolean = false) {
        if (refreshJob?.isActive == true) {
            return
        }

        if (!force) {
            // 语义：如果 "现在" 距离 "上次刷新" 的时间还没超过 "间隔"，则跳过
            if (System.nanoTime() - lastRefreshTime < interval) {
                return
            }
        }

        refreshJob = scope.launch {
            val playersDeferred = async { dataSource.connection.use { connection -> allRepository.selectPlayers(connection) } }
            val residencesDeferred = async { dataSource.connection.use { connection -> allRepository.selectResidences(connection) } }

            val players = playersDeferred.await()
            val residences = residencesDeferred.await()

            withContext(bukkitDispatcher) {
                playersByUuid.clear()
                residencesByName.clear()
                residencesSort.clear()
                residencesSortByPlayerUuid.clear()

                players.forEach { player ->
                    val playerUuid = player.uuid
                    val playerName = player.name
                    val p = Player(playerUuid, playerName)
                    playersByUuid[playerUuid] = p
                    playersByName[playerName] = p
                }

                residences.forEach { residence ->
                    val residenceName = residence.name
                    val ownerUuid = residence.ownerUuid
                    val residenceFlags = HashMap(residence.residenceFlags)
                    val playerFlags = residence.playerFlags.mapValuesTo(HashMap()) { (key, flags) ->
                        HashMap(flags)
                    }

                    val r = Residence(residenceName, ownerUuid, residenceFlags, playerFlags, residence.serverName)
                    residencesByName[residenceName] = r
                    residencesSort.add(r)
                    residencesSortByPlayerUuid.getOrPut(ownerUuid) { arrayListOf() }.add(r)
                }

                lastRefreshTime = System.nanoTime()
            }
        }
    }


    fun addPlayer1(player: Player) {
        val playerUuid = player.uuid
        val playerName = player.name
        playersByUuid[playerUuid] = player
        playersByName[playerName] = player
    }

    fun addSortedList(list: ArrayList<Residence>, residence: Residence) {
        val index = list.binarySearch { it.name.compareTo(residence.name) }
        if (index < 0) {
            val insertionPoint = -(index + 1)
            list.add(insertionPoint, residence)
        }
    }

    fun addResidence1(residence: Residence) {
        residencesByName[residence.name] = residence

        addSortedList(residencesSort, residence)

        val residences = residencesSortByPlayerUuid.getOrPut(residence.ownerUuid) { arrayListOf() }
        addSortedList(residences, residence)
    }

    fun removePlayer1(uuid: UUID): Player? {
        val player = playersByUuid.remove(uuid) ?: return null
        playersByName.remove(player.name)
        return player
    }

    fun removePlayer1(name: String): Player? {
        val player = playersByName.remove(name) ?: return null
        playersByUuid.remove(player.uuid)
        return player
    }

    fun removeSortedList(list: ArrayList<Residence>, residence: Residence) {
        val index = list.binarySearch { it.name.compareTo(residence.name) }
        if (index >= 0) {
            list.removeAt(index)
        }
    }

    fun removeResidence1(name: String): Residence? {
        val residence = residencesByName.remove(name) ?: return null
        removeSortedList(residencesSort, residence)

        val residences = residencesSortByPlayerUuid[residence.ownerUuid]
        if (residences != null) {
            removeSortedList(residences, residence)
        }

        return residence
    }


    fun onResidenceCreation(player: Player, residence: Residence) {
        addPlayer1(player)
        addResidence1(residence)
    }

    fun onResidenceFlagChange(residence: Residence) {
        val residenceName = residence.name

        val res = residencesByName[residenceName] ?: return
        res.residenceFlags = residence.residenceFlags
        res.playerFlags = residence.playerFlags
    }

    fun onResidenceDelete(name: String) {
        removeResidence1(name)
    }

    fun onResidenceRename(oldName: String, newName: String) {
        val residence = removeResidence1(oldName) ?: return
        residence.name = newName
        addResidence1(residence)
    }

    fun onResidenceOwnerChange(name: String, newPlayerUuid: UUID) {
        val residence = residencesByName[name] ?: return // 如果 Residence 不存在，直接返回
        val oldPlayerUuid = residence.ownerUuid // 获取旧玩家的 UUID
        val oldResidence = residencesSortByPlayerUuid[oldPlayerUuid]
        if (oldResidence != null) {
            removeSortedList(oldResidence, residence)
        }

        residence.ownerUuid = newPlayerUuid
        residencesByName[name] = residence

        val newResidences = residencesSortByPlayerUuid.getOrPut(newPlayerUuid) { arrayListOf() }
        addSortedList(newResidences, residence)
    }


    class Player(
        override val uuid: UUID,
        override var name: String
    ) : IReadOnlyPlayer

    class Residence(
        override var name: String,
        override var ownerUuid: UUID,
        override var residenceFlags: HashMap<String, Boolean>,
        override var playerFlags: HashMap<UUID, HashMap<String, Boolean>>,
        override var serverName: String
    ) : IReadOnlyResidence

}


interface IReadOnlyPlayer {
    val uuid: UUID
    val name: String
}

interface IReadOnlyResidence {
    val name: String
    val ownerUuid: UUID
    val residenceFlags: Map<String, Boolean>
    val playerFlags: Map<UUID, Map<String, Boolean>>
    val serverName: String
}
