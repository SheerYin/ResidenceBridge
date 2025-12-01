package me.yin.residencebridge.other

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.yin.residencebridge.configuration.MainConfiguration
import java.sql.Connection
import java.sql.SQLIntegrityConstraintViolationException
import java.util.*

class AllRepository(
    val mainConfiguration: MainConfiguration,
    val json: Json
) {

    // 将表名也设为私有，仅内部使用
    private val tableNamePlayers = mainConfiguration.simpleConfiguration.tablePrefix + "players"
    private val tableNameResidences = mainConfiguration.simpleConfiguration.tablePrefix + "residences"


    // --- Initialization ---

    private val sqlCreateTablePlayers = """
        CREATE TABLE IF NOT EXISTS $tableNamePlayers (
            id INT AUTO_INCREMENT PRIMARY KEY,
            uuid CHAR(36) UNIQUE NOT NULL,
            name VARCHAR(64),
            INDEX (uuid),
            INDEX (name)
        );
    """

    fun initializePlayerTable(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.executeUpdate(sqlCreateTablePlayers)
        }
    }

    private val sqlCreateTableResidences = """
        CREATE TABLE IF NOT EXISTS $tableNameResidences (
            id INT AUTO_INCREMENT PRIMARY KEY,
            name VARCHAR(64),
            owner_uuid CHAR(36),
            residence_flags JSON DEFAULT NULL,
            player_flags JSON DEFAULT NULL,
            server_name VARCHAR(64),
            INDEX (name),
            INDEX (owner_uuid),
            INDEX (owner_uuid, name)
        );
    """

    fun initializeResidenceTable(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.executeUpdate(sqlCreateTableResidences)
        }
    }

    // --- Player Operations ---

    private val sqlUpdatePlayerName = "UPDATE $tableNamePlayers SET name = ? WHERE uuid = ?"
    private val sqlInsertPlayer = "INSERT INTO $tableNamePlayers (uuid, name) VALUES (?, ?)"

    fun updateInsertPlayer(connection: Connection, player: IReadOnlyPlayer) {
        val uuid = player.uuid.toString()
        val name = player.name

        // 1. Attempt update
        var updatedRows: Int
        connection.prepareStatement(sqlUpdatePlayerName).use { preparedStatement ->
            preparedStatement.setString(1, name)
            preparedStatement.setString(2, uuid)
            updatedRows = preparedStatement.executeUpdate()
        }

        // 2. If no rows updated, insert new player
        if (updatedRows == 0) {
            try {
                connection.prepareStatement(sqlInsertPlayer).use { preparedStatement ->
                    preparedStatement.setString(1, uuid)
                    preparedStatement.setString(2, name)
                    preparedStatement.executeUpdate()
                }
            } catch (e: SQLIntegrityConstraintViolationException) {
                // 处理并发情况下的唯一性冲突
                connection.prepareStatement(sqlUpdatePlayerName).use { preparedStatement ->
                    preparedStatement.setString(1, name)
                    preparedStatement.setString(2, uuid)
                    preparedStatement.executeUpdate()
                }
            }
        }
    }

    fun updateInsertPlayersBatch(connection: Connection, players: List<IReadOnlyPlayer>, batchSize: Int = 500) {
        if (players.isEmpty()) return

        val noInsert = ArrayList<IReadOnlyPlayer>()

        // --- Update 阶段 ---
        connection.prepareStatement(sqlUpdatePlayerName).use { ps ->
            val updateCounts = ArrayList<Int>(players.size)
            players.chunked(batchSize).forEach { chunk ->
                for (player in chunk) {
                    ps.setString(1, player.name)
                    ps.setString(2, player.uuid.toString())
                    ps.addBatch()
                }
                val results = ps.executeBatch()
                for (res in results) updateCounts.add(res)
            }

            // 筛选逻辑
            for (i in updateCounts.indices) {
                if (updateCounts[i] == 0) {
                    noInsert.add(players[i])
                }
            }
        }

        // --- Insert 阶段 ---
        if (noInsert.isEmpty()) return

        connection.prepareStatement(sqlInsertPlayer).use { ps ->
            noInsert.chunked(batchSize).forEach { chunk ->
                chunk.forEach { player ->
                    ps.setString(1, player.uuid.toString())
                    ps.setString(2, player.name)
                    ps.addBatch()
                }
                ps.executeBatch()
            }
        }
    }

    private val sqlSelectAllPlayers = """
        SELECT
            uuid,
            name
        FROM $tableNamePlayers
        ORDER BY name
    """.trimIndent()

    fun selectPlayers(connection: Connection): List<IReadOnlyPlayer> {
        val list = mutableListOf<IReadOnlyPlayer>()
        connection.prepareStatement(sqlSelectAllPlayers).use { statement ->
            statement.executeQuery().use { rs ->
                while (rs.next()) {
                    val uuid = UUID.fromString(rs.getString("uuid"))
                    val name = rs.getString("name")
                    list.add(Player(uuid, name))
                }
            }
        }
        return list
    }

    private val sqlSelectPlayerNameByUuid = "SELECT name FROM $tableNamePlayers WHERE uuid = ? LIMIT 1"

    fun selectPlayerName(connection: Connection, playerUuid: UUID): String? {
        connection.prepareStatement(sqlSelectPlayerNameByUuid).use { statement ->
            statement.setString(1, playerUuid.toString())
            statement.executeQuery().use { resultSet ->
                return if (resultSet.next()) {
                    resultSet.getString("name")
                } else {
                    null
                }
            }
        }
    }

    private val sqlSelectPlayerUuidByName = "SELECT uuid FROM $tableNamePlayers WHERE name = ? LIMIT 1"

    fun selectPlayerUuid(connection: Connection, playerName: String): UUID? {
        connection.prepareStatement(sqlSelectPlayerUuidByName).use { statement ->
            statement.setString(1, playerName)
            statement.executeQuery().use { resultSet ->
                return if (resultSet.next()) {
                    UUID.fromString(resultSet.getString("uuid"))
                } else {
                    null
                }
            }
        }
    }

    // --- Residence Basic Operations ---

    private val sqlInsertResidence = "INSERT INTO $tableNameResidences (name, owner_uuid, residence_flags, player_flags, server_name) VALUES (?, ?, ?, ?, ?)"

    fun insertResidence(connection: Connection, residence: IReadOnlyResidence): Boolean {
        return connection.prepareStatement(sqlInsertResidence).use { preparedStatement ->
            preparedStatement.setString(1, residence.name)
            preparedStatement.setString(2, residence.ownerUuid.toString())
            preparedStatement.setString(3, json.encodeToString(residence.residenceFlags))
            preparedStatement.setString(4, json.encodeToString(residence.playerFlags))
            preparedStatement.setString(5, residence.serverName)
            preparedStatement.executeUpdate() > 0
        }
    }

    fun insertResidencesBatch(connection: Connection, residences: List<IReadOnlyResidence>, batchSize: Int = 500) {
        if (residences.isEmpty()) return

        connection.prepareStatement(sqlInsertResidence).use { ps ->
            residences.chunked(batchSize) { chunk ->
                chunk.forEach { residence ->
                    ps.setString(1, residence.name)
                    ps.setString(2, residence.ownerUuid.toString())
                    ps.setString(3, json.encodeToString(residence.residenceFlags))
                    ps.setString(4, json.encodeToString(residence.playerFlags))
                    ps.setString(5, residence.serverName)
                    ps.addBatch()
                }
                ps.executeBatch()
            }
        }
    }

    private val sqlDeleteResidenceByName = "DELETE FROM $tableNameResidences WHERE name = ? LIMIT 1"

    fun deleteResidence(connection: Connection, residenceName: String): Boolean {
        return connection.prepareStatement(sqlDeleteResidenceByName).use { statement ->
            statement.setString(1, residenceName)
            statement.executeUpdate() > 0
        }
    }

    // --- Residence Selection ---

    private val sqlSelectAllResidences = """
        SELECT
            name,
            residence_flags,
            player_flags,
            server_name,
            owner_uuid
        FROM $tableNameResidences
        ORDER BY name
    """.trimIndent()

    fun selectResidences(connection: Connection): List<IReadOnlyResidence> {
        val list = mutableListOf<IReadOnlyResidence>()
        connection.prepareStatement(sqlSelectAllResidences).use { statement ->
            statement.executeQuery().use { rs ->
                while (rs.next()) {
                    val residenceName = rs.getString("name")
                    val ownerUuid = UUID.fromString(rs.getString("owner_uuid"))
                    val residenceFlags = json.decodeFromString<Map<String, Boolean>>(rs.getString("residence_flags"))
                    val playerFlags = json.decodeFromString<Map<UUID, Map<String, Boolean>>>(rs.getString("player_flags"))
                    val serverName = rs.getString("server_name")
                    list.add(Residence(residenceName, ownerUuid, residenceFlags, playerFlags, serverName))
                }
            }
        }
        return list
    }

    private val sqlSelectResidenceByName = """
        SELECT
            name,
            residence_flags,
            player_flags,
            server_name,
            owner_uuid
        FROM $tableNameResidences
        WHERE name = ?
    """.trimIndent()

    fun selectResidence(connection: Connection, residenceName: String): IReadOnlyResidence? {
        connection.prepareStatement(sqlSelectResidenceByName).use { statement ->
            statement.setString(1, residenceName)
            statement.executeQuery().use { rs ->
                if (rs.next()) {
                    val name = rs.getString("name")
                    val ownerUuid = UUID.fromString(rs.getString("owner_uuid"))
                    val residenceFlags = json.decodeFromString<Map<String, Boolean>>(rs.getString("residence_flags"))
                    val playerFlags = json.decodeFromString<Map<UUID, Map<String, Boolean>>>(rs.getString("player_flags"))
                    val serverName = rs.getString("server_name")
                    return Residence(name, ownerUuid, residenceFlags, playerFlags, serverName)
                }
            }
        }
        return null
    }

    // --- Residence Names ---

    private val sqlSelectAllResidenceNames = "SELECT name FROM $tableNameResidences ORDER BY name"

    fun selectResidenceNames(connection: Connection): List<String> {
        val list = mutableListOf<String>()
        connection.prepareStatement(sqlSelectAllResidenceNames).use { statement ->
            statement.executeQuery().use { rs ->
                while (rs.next()) {
                    list.add(rs.getString("name"))
                }
            }
        }
        return list
    }

    private val sqlSelectResidenceNamesByOwnerUuid = """
        SELECT name 
        FROM $tableNameResidences 
        WHERE owner_uuid = ? 
        ORDER BY name
    """.trimIndent()

    fun selectPlayerResidenceNames(connection: Connection, playerUuid: UUID): List<String> {
        val list = mutableListOf<String>()
        connection.prepareStatement(sqlSelectResidenceNamesByOwnerUuid).use { statement ->
            statement.setString(1, playerUuid.toString())
            statement.executeQuery().use { rs ->
                while (rs.next()) {
                    list.add(rs.getString("name"))
                }
            }
        }
        return list
    }

    private val sqlSelectResidenceNamesByOwnerName = """
        SELECT residence.name 
        FROM $tableNameResidences residence 
        JOIN $tableNamePlayers player 
            ON residence.owner_uuid = player.uuid 
        WHERE player.name = ? 
        ORDER BY residence.name
    """.trimIndent()

    fun selectPlayerResidenceNames(connection: Connection, playerName: String): List<String> {
        val list = mutableListOf<String>()
        connection.prepareStatement(sqlSelectResidenceNamesByOwnerName).use { statement ->
            statement.setString(1, playerName)
            statement.executeQuery().use { rs ->
                while (rs.next()) {
                    list.add(rs.getString("name"))
                }
            }
        }
        return list
    }

    // --- Residence Counts ---

    private val sqlCountResidencesByOwnerName = """
        SELECT COUNT(*) AS residence_count 
        FROM $tableNameResidences residence 
        JOIN $tableNamePlayers player ON residence.owner_uuid = player.uuid 
        WHERE player.name = ?
    """.trimIndent()

    fun selectPlayerResidencesCount(connection: Connection, playerName: String): Int? {
        connection.prepareStatement(sqlCountResidencesByOwnerName).use { statement ->
            statement.setString(1, playerName)
            statement.executeQuery().use { resultSet ->
                return if (resultSet.next()) {
                    resultSet.getInt("residence_count")
                } else {
                    null
                }
            }
        }
    }

    private val sqlCountResidencesByOwnerUuid = "SELECT COUNT(*) AS residence_count FROM $tableNameResidences WHERE owner_uuid = ?"

    fun selectPlayerResidencesCount(connection: Connection, playerUuid: UUID): Int? {
        connection.prepareStatement(sqlCountResidencesByOwnerUuid).use { statement ->
            statement.setString(1, playerUuid.toString())
            statement.executeQuery().use { resultSet ->
                return if (resultSet.next()) {
                    resultSet.getInt("residence_count")
                } else {
                    null
                }
            }
        }
    }

    private val sqlCheckResidenceExists = "SELECT 1 FROM $tableNameResidences WHERE name = ? LIMIT 1"

    fun selectResidenceName(connection: Connection, residenceName: String): Boolean {
        connection.prepareStatement(sqlCheckResidenceExists).use { statement ->
            statement.setString(1, residenceName)
            statement.executeQuery().use { resultSet ->
                return resultSet.next()
            }
        }
    }

    // --- Updates (Name/Owner) ---

    private val sqlUpdateResidenceName = "UPDATE $tableNameResidences SET name = ? WHERE name = ? LIMIT 1"

    fun updateResidenceName(connection: Connection, oldName: String, newName: String): Boolean {
        return connection.prepareStatement(sqlUpdateResidenceName).use { statement ->
            statement.setString(1, newName)
            statement.setString(2, oldName)
            statement.executeUpdate() > 0
        }
    }

    private val sqlUpdateResidenceOwner = "UPDATE $tableNameResidences SET owner_uuid = ? WHERE name = ? LIMIT 1"

    fun updateResidencePlayerUuid(connection: Connection, residenceName: String, playerUuid: UUID): Boolean {
        return connection.prepareStatement(sqlUpdateResidenceOwner).use { statement ->
            statement.setString(1, playerUuid.toString())
            statement.setString(2, residenceName)
            statement.executeUpdate() > 0
        }
    }

    private val sqlUpdateResidenceFlags = """
        UPDATE $tableNameResidences 
        SET residence_flags = ?, 
            player_flags = ?
        WHERE name = ?
    """.trimIndent()

    fun updateResidenceFlags(connection: Connection, residence: IReadOnlyResidence): Boolean {
        return connection.prepareStatement(sqlUpdateResidenceFlags).use { preparedStatement ->
            // [修复] 参数顺序改为正常的 1, 2, 3 (之前代码中是 2, 3, 5 且 SQL 语法有误)
            preparedStatement.setString(1, json.encodeToString(residence.residenceFlags))
            preparedStatement.setString(2, json.encodeToString(residence.playerFlags))
            preparedStatement.setString(3, residence.name)

            preparedStatement.executeUpdate() > 0
        }
    }

    fun updateResidencesFlags(connection: Connection, residences: List<IReadOnlyResidence>, batchSize: Int = 500) {
        if (residences.isEmpty()) return

        connection.prepareStatement(sqlUpdateResidenceFlags).use { ps ->
            residences.chunked(batchSize).forEach { chunk ->
                chunk.forEach { residence ->
                    ps.setString(1, json.encodeToString(residence.residenceFlags))
                    ps.setString(2, json.encodeToString(residence.playerFlags))
                    ps.setString(3, residence.name)
                    ps.addBatch()
                }
                // [修复] 之前这里误用了 executeUpdate，必须是 executeBatch
                ps.executeBatch()
            }
        }
    }

    // --- Data Classes ---

    @Serializable
    class Residence(
        override val name: String,
        @Contextual
        override val ownerUuid: UUID,
        override val residenceFlags: Map<String, Boolean>,
        override val playerFlags: Map<@Contextual UUID, Map<String, Boolean>>,
        override val serverName: String
    ): IReadOnlyResidence

    @Serializable
    class Player(
        @Contextual
        override val uuid: UUID,
        override var name: String
    ): IReadOnlyPlayer

    @Serializable
    data class ResidenceDetail(
        val residenceName: String,
        @Contextual
        val ownerUuid: UUID,
        val ownerName: String,
        val residenceFlags: Map<String, Boolean>,
        val playerFlags: Map<@Contextual UUID, Map<String, Boolean>>,
        val serverName: String
    )
}