package me.yin.residencebridge.other

import java.sql.Connection
import javax.sql.DataSource

class DatabaseManager(
    val dataSource: DataSource
) {

    fun <T> executeTransaction(block: (Connection) -> T): T {
        // use 块会自动关闭连接 (close)
        return dataSource.connection.use { connection ->
            val originalAutoCommit = connection.autoCommit
            connection.autoCommit = false // 开启事务

            try {
                val result = block(connection) // 执行业务逻辑
                connection.commit() // 提交
                result // 返回结果
            } catch (e: Exception) {
                connection.rollback() // 出错回滚
                // 强烈建议重新抛出异常，让上层知道事务失败了
                // 如果你使用日志框架，可以在这里 log.error("Transaction failed", e)
                throw e
            } finally {
                connection.autoCommit = originalAutoCommit // 还原状态
            }
        }
    }
}