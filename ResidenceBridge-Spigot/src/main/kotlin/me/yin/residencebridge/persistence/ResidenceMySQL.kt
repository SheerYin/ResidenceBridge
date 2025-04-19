package me.yin.residencebridge.persistence

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import me.yin.residencebridge.configuration.DatabaseYAML
import me.yin.residencebridge.model.ResidenceInfo
import java.sql.Connection
import java.sql.SQLException
import java.util.*

object ResidenceMySQL {

    // jdbcUrl = "jdbc:mysql://${user}:${password}@localhost:3306/database"

    lateinit var dataSource: HikariDataSource
    lateinit var tablePrefix: String
    fun initialize() {

        val configuration = DatabaseYAML.configuration

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = configuration.getString("residence.url")
            maximumPoolSize = configuration.getInt("residence.maximum-pool-size")
            minimumIdle = configuration.getInt("residence.minimum-idle")
            connectionTimeout = configuration.getLong("residence.connection-timeout")
            idleTimeout = configuration.getLong("residence.idle-timeout")
            maxLifetime = configuration.getLong("residence.maximum-lifetime")
        }
        dataSource = HikariDataSource(hikariConfig)

        tablePrefix = configuration.getString("residence.table-prefix")!!

        createTable()
    }

    lateinit var table: String
    fun createTable() {
        table = tablePrefix + "residence"
        val sql = """
        CREATE TABLE IF NOT EXISTS $table (
            id INT AUTO_INCREMENT PRIMARY KEY,
            residence_name VARCHAR(64) NOT NULL,
            owner_uuid VARCHAR(36),
            owner_name VARCHAR(64),
            residence_flags JSON DEFAULT NULL,
            player_flags JSON DEFAULT NULL,
            server_name VARCHAR(64),
            INDEX (residence_name),
            INDEX (owner_uuid),
            INDEX (owner_name)
        );
        """
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(sql)
            }
        }
    }

    val gson = Gson()
    fun insertResidence(residenceInfo: ResidenceInfo): Boolean {
        val sql = "INSERT INTO $table (residence_name, owner_uuid, owner_name, residence_flags, player_flags, server_name) VALUES (?, ?, ?, ?, ?, ?)"
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, residenceInfo.residenceName)
                preparedStatement.setString(2, residenceInfo.ownerUUID.toString())
                preparedStatement.setString(3, residenceInfo.ownerName)
                preparedStatement.setString(4, gson.toJson(residenceInfo.residenceFlags))
                preparedStatement.setString(5, gson.toJson(residenceInfo.playerFlags))
                preparedStatement.setString(6, residenceInfo.serverName)
                return preparedStatement.executeUpdate() > 0
            }
        }
    }

    fun batchInsertResidences(residenceInfos: List<ResidenceInfo>): Boolean {
        val sql = "INSERT INTO $table (residence_name, owner_uuid, owner_name, residence_flags, player_flags, server_name) VALUES (?, ?, ?, ?, ?, ?)"
        dataSource.connection.use { connection ->
            connection.autoCommit = false // 禁用自动提交，提高批量操作性能
            try {
                connection.prepareStatement(sql).use { preparedStatement ->
                    for (residenceInfo in residenceInfos) {
                        preparedStatement.setString(1, residenceInfo.residenceName)
                        preparedStatement.setString(2, residenceInfo.ownerUUID.toString())
                        preparedStatement.setString(3, residenceInfo.ownerName)
                        preparedStatement.setString(4, gson.toJson(residenceInfo.residenceFlags))
                        preparedStatement.setString(5, gson.toJson(residenceInfo.playerFlags))
                        preparedStatement.setString(6, residenceInfo.serverName)
                        preparedStatement.addBatch()
                    }

                    val results = preparedStatement.executeBatch()
                    connection.commit()

                    return results.all { it >= 0 }
                }
            } catch (exception: SQLException) {
                connection.rollback()
                exception.printStackTrace()
                return false
            } finally {
                connection.autoCommit = true
            }
        }
    }


    fun deleteResidence(residenceName: String): Boolean {
        val sql = "DELETE FROM $table WHERE residence_name = ? LIMIT 1"
        dataSource.connection.use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, residenceName)
                return preparedStatement.executeUpdate() > 0
            }
        }
    }

    fun selectResidence(residenceName: String): ResidenceInfo? {
        val sql = "SELECT residence_name, owner_uuid, owner_name, residence_flags, player_flags, server_name FROM $table WHERE residence_name = ? LIMIT 1"
        dataSource.connection.use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, residenceName)
                val resultSet = preparedStatement.executeQuery()
                if (resultSet.next()) {
                    val residenceInfo = ResidenceInfo(
                        residenceName,
                        UUID.fromString(resultSet.getString("owner_uuid")),
                        resultSet.getString("owner_name"),
                        gson.fromJson(resultSet.getString("residence_flags"), object : TypeToken<MutableMap<String, Boolean>>() {}.type),
                        gson.fromJson(resultSet.getString("player_flags"), object : TypeToken<MutableMap<String, MutableMap<String, Boolean>>>() {}.type),
                        resultSet.getString("server_name")
                    )
                    return residenceInfo
                }
                return null
            }
        }
    }

    fun selectResidences(): List<ResidenceInfo> {
        val list: MutableList<ResidenceInfo> = mutableListOf()

        val sql = "SELECT residence_name, owner_uuid, owner_name, residence_flags, player_flags, server_name FROM $table"
        dataSource.connection.use { connection: Connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(sql).use { resultSet ->
                    while (resultSet.next()) {
                        val residenceInfo = ResidenceInfo(
                            resultSet.getString("residence_name"),
                            UUID.fromString(resultSet.getString("owner_uuid")),
                            resultSet.getString("owner_name"),
                            gson.fromJson(resultSet.getString("residence_flags"), object : TypeToken<MutableMap<String, Boolean>>() {}.type),
                            gson.fromJson(resultSet.getString("player_flags"), object : TypeToken<MutableMap<String, MutableMap<String, Boolean>>>() {}.type),
                            resultSet.getString("server_name")
                        )
                        list.add(residenceInfo)
                    }
                    return list
                }
            }
        }
    }


    fun selectResidenceNames(): List<String> {
        val list: MutableList<String> = mutableListOf()

        val sql = "SELECT residence_name FROM $table"
        dataSource.connection.use { connection: Connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(sql).use { resultSet ->
                    while (resultSet.next()) {
                        val residenceName = resultSet.getString("residence_name")
                        list.add(residenceName)
                    }
                    return list
                }
            }
        }
    }


    fun selectOwnerResidenceNames(ownerUUID: UUID): List<String> {
        val list: MutableList<String> = mutableListOf()

        val sql = "SELECT residence_name FROM $table WHERE owner_uuid = ?"
        dataSource.connection.use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, ownerUUID.toString())
                preparedStatement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        list.add(resultSet.getString("residence_name"))
                    }
                }
            }
        }
        return list
    }

    fun selectOwnerResidenceNames(ownerName: String): List<String> {
        val list: MutableList<String> = mutableListOf()

        val sql = "SELECT residence_name FROM $table WHERE owner_name = ?"
        dataSource.connection.use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, ownerName)
                preparedStatement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        list.add(resultSet.getString("residence_name"))
                    }
                }
            }
        }
        return list
    }

    fun selectOwnerResidences(ownerUUID: UUID): List<ResidenceInfo> {
        val list: MutableList<ResidenceInfo> = mutableListOf()

        val sql = "SELECT residence_name, owner_uuid, owner_name, residence_flags, player_flags, server_name FROM $table WHERE owner_uuid = ?"
        dataSource.connection.use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, ownerUUID.toString())
                preparedStatement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        val residenceInfo = ResidenceInfo(
                            resultSet.getString("residence_name"),
                            UUID.fromString(resultSet.getString("owner_uuid")),
                            resultSet.getString("owner_name"),
                            gson.fromJson(resultSet.getString("residence_flags"), object : TypeToken<MutableMap<String, Boolean>>() {}.type),
                            gson.fromJson(resultSet.getString("player_flags"), object : TypeToken<MutableMap<String, MutableMap<String, Boolean>>>() {}.type),
                            resultSet.getString("server_name")
                        )
                        list.add(residenceInfo)
                    }
                }
            }
        }
        return list
    }

    fun selectOwnerResidences(ownerName: String): List<ResidenceInfo> {
        val list: MutableList<ResidenceInfo> = mutableListOf()

        val sql = "SELECT residence_name, owner_uuid, owner_name, residence_flags, player_flags, server_name FROM $table WHERE owner_name = ?"
        dataSource.connection.use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, ownerName)
                preparedStatement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        val residenceInfo = ResidenceInfo(
                            resultSet.getString("residence_name"),
                            UUID.fromString(resultSet.getString("owner_uuid")),
                            resultSet.getString("owner_name"),
                            gson.fromJson(resultSet.getString("residence_flags"), object : TypeToken<MutableMap<String, Boolean>>() {}.type),
                            gson.fromJson(resultSet.getString("player_flags"), object : TypeToken<MutableMap<String, MutableMap<String, Boolean>>>() {}.type),
                            resultSet.getString("server_name")
                        )
                        list.add(residenceInfo)
                    }
                }
            }

        }
        return list
    }

    fun selectResidenceServerName(residenceName: String): String? {
        val sql = "SELECT server_name FROM $table WHERE residence_name = ? LIMIT 1"
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, residenceName)
                preparedStatement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        return resultSet.getString("server_name")
                    }
                }
            }
        }
        return null
    }

    fun selectOwnerResidencesCount(ownerName: String): Int? {
        val sql = "SELECT COUNT(*) AS residence_count FROM $table WHERE owner_name = ?"
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, ownerName)
                preparedStatement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        return resultSet.getInt("residence_count")
                    }
                }
            }
        }
        return null
    }

    fun selectOwnerResidencesCount(ownerUUID: UUID): Int? {
        val sql = "SELECT COUNT(*) AS residence_count FROM $table WHERE owner_uuid = ?"
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, ownerUUID.toString())
                preparedStatement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        return resultSet.getInt("residence_count")
                    }
                }
            }
        }
        return null
    }

    fun isResidenceExists(residenceName: String): Boolean {
        val sql = "SELECT 1 FROM $table WHERE residence_name = ? LIMIT 1"
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, residenceName)
                preparedStatement.executeQuery().use { resultSet ->
                    return resultSet.next()
                }
            }
        }
    }

    fun updateResidenceOwner(residenceName: String, ownerUUID: UUID, ownerName: String): Boolean {
        val sql = "UPDATE $table SET owner_uuid = ?, owner_name = ? WHERE residence_name = ? LIMIT 1"
        dataSource.connection.use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, ownerUUID.toString())
                preparedStatement.setString(2, ownerName)
                preparedStatement.setString(3, residenceName)
                return preparedStatement.executeUpdate() > 0
            }
        }
    }

    fun updateResidenceName(oldName: String, newName: String): Boolean {
        val sql = "UPDATE $table SET residence_name = ? WHERE residence_name = ? LIMIT 1"
        dataSource.connection.use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, newName)
                preparedStatement.setString(2, oldName)
                return preparedStatement.executeUpdate() > 0
            }
        }
    }

    fun updateSetResidenceFlags(residenceName: String, key: String, value: Boolean): Boolean {
        val sql = "UPDATE $table SET residence_flags = JSON_SET(residence_flags, ?, ?) WHERE residence_name = ? LIMIT 1"
        dataSource.connection.use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, "$.\"$key\"")
                preparedStatement.setString(2, value.toString())
                preparedStatement.setString(3, residenceName)
                return preparedStatement.executeUpdate() > 0
            }
        }
    }

    fun updateRemoveResidenceFlags(residenceName: String, key: String): Boolean {
        val sql = "UPDATE $table SET residence_flags = JSON_REMOVE(residence_flags, ?) WHERE residence_name = ? LIMIT 1"
        dataSource.connection.use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, "$.\"$key\"")
                preparedStatement.setString(2, residenceName)
                return preparedStatement.executeUpdate() > 0
            }
        }
    }

    fun updateSetPlayerFlags(residenceName: String, playerUUID: UUID, key: String, value: Boolean): Boolean {
        val sql = "UPDATE $table SET player_flags = JSON_SET(player_flags, ?, ?) WHERE residence_name = ?"
        dataSource.connection.use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, "$.\"$playerUUID\".\"$key\"")
                preparedStatement.setString(2, value.toString())
                preparedStatement.setString(3, residenceName)
                return preparedStatement.executeUpdate() > 0

            }
        }
    }

    fun updateRemovePlayerFlags(residenceName: String, playerUUID: UUID, key: String): Boolean {
        val sql = "UPDATE $table SET player_flags = JSON_REMOVE(player_flags, ?) WHERE residence_name = ?"
        dataSource.connection.use { connection: Connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setString(1, "$.\"$playerUUID\".\"$key\"")
                preparedStatement.setString(2, residenceName)
                return preparedStatement.executeUpdate() > 0
            }
        }
    }


}


