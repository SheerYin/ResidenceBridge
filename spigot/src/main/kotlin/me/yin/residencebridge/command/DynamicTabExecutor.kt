package me.yin.residencebridge.command

import com.bekvon.bukkit.residence.Residence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.yin.residencebridge.ResidenceBridge
import me.yin.residencebridge.configuration.MainConfiguration
import me.yin.residencebridge.configuration.MessageConfiguration
import me.yin.residencebridge.message.SimpleMessage
import me.yin.residencebridge.other.*
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import java.util.*

class DynamicTabExecutor(
    val residenceBridge: ResidenceBridge,
    val residenceInstance: Residence?,
    val databaseManager: DatabaseManager,
    val allRepository: AllRepository,
    val scope: CoroutineScope,
    val allCache: AllCache,
    val residenceTeleport: ResidenceTeleport,
    val simpleMessage: SimpleMessage,
    val mainConfiguration: MainConfiguration,
    val messageConfiguration: MessageConfiguration
): TabExecutor {

    val pluginNameLowercase = residenceBridge.pluginNameLowercase
    val mainPermission = "${residenceBridge.pluginNameLowercase}.command"


    override fun onCommand(sender: CommandSender, command: Command, label: String, arguments: Array<out String>): Boolean {
        if (!permissionMessage(sender, mainPermission)) {
            return false
        }

        val argumentsSize = arguments.size
        if (argumentsSize == 0) {
            // help 命令
            if (!permissionMessage(sender, "$mainPermission.help")) return false
            executeHelp(sender)
        }
        else if (argumentsSize == 1) {
            when (arguments[0].lowercase()) {
                "help" -> {
                    if (!permissionMessage(sender, "$mainPermission.help")) return false
                    executeHelp(sender)
                }
                "list" -> {
                    if (!permissionMessage(sender, "$mainPermission.list")) return false
                    executeList(sender)
                }
                "listall" -> {
                    if (!permissionMessage(sender, "$mainPermission.listall")) return false
                    executeListAll(sender)
                }
                "import" -> {
                    if (!permissionMessage(sender, "$mainPermission.import")) return false
                    executeImportResidence(sender)
                }
                "reload" -> {
                    if (!permissionMessage(sender, "$mainPermission.reload")) return false
                    executeReloadMessages(sender)
                }
            }
        }
        else if (argumentsSize == 2) {
            when (arguments[0].lowercase()) {
                "list" -> {
                    if (!permissionMessage(sender, "$mainPermission.list")) return false
                    val targetName = arguments[1]
                    executeList(sender, targetName)
                }
                "listall" -> {
                    if (!permissionMessage(sender, "$mainPermission.listall")) return false
                    val page = arguments[1].toIntOrNull() ?: return false
                    executeListAll(sender, page)
                }
                "teleport" -> {
                    if (!permissionMessage(sender, "$mainPermission.teleport")) return false
                    executeTeleport(sender,null, arguments[1])
                }
            }
        }
        else if (argumentsSize == 3) {
            when (arguments[0].lowercase()) {
                "list" -> {
                    if (!permissionMessage(sender, "$mainPermission.list")) return false
                    val targetName = arguments[1]
                    val page = arguments[2].toIntOrNull() ?: return false

                    if (sender.name == targetName) {
                        executeList(sender, page)
                    } else {
                        executeList(sender, targetName, page)
                    }
                }

                "teleport" -> {
                    if (!permissionMessage(sender, "$mainPermission.teleport")) return false
                    executeTeleport(sender, arguments[2],arguments[1])
                }
            }
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, arguments: Array<out String>): List<String>? {
        val argumentsSize = arguments.size
        if (argumentsSize == 1) {
            return prune(arguments[0], listOf("help", "teleport", "import", "list", "listall", "reload"))
        }
        else if (argumentsSize == 2) {
            when (arguments[0].lowercase()) {
                "list" -> {
                    val argument = arguments[1]
                    return prune(argument, allCache.fetchPlayersByName().keys)
                }
                "listall" -> {
                    return listOf("[page]")
                }
                "teleport" -> {
                    val argument = arguments[1]
                    return prune(argument, allCache.fetchResidencesByName().keys)
                }
            }
        }
        else if (argumentsSize == 3) {
            when (arguments[0].lowercase()) {
                "list" -> {
                    return listOf("[page]")
                }
                "teleport" -> {
                    val argument = arguments[2]
                    val empty = argument.isEmpty()
                    val list = mutableListOf<String>()
                    for (player in Bukkit.getOnlinePlayers()) {
                        val playerName = player.name
                        if (empty) {
                            list.add(playerName)
                        } else if (playerName.startsWith(argument, true)) {
                            list.add(playerName)
                        }
                    }
                    return list
                }
            }
        }

        return null
    }


    fun prune(argument: String, suggest: Collection<String>): List<String> {
        if (argument.isEmpty()) {
            return suggest.toList()
        } else {
            return suggest.filter { it.startsWith(argument, true) }
        }
    }

    fun permissionMessage(sender: CommandSender, permission: String): Boolean {
        if (sender !is Player) {
            return true
        }
        if (sender.hasPermission(permission)) {
            return true
        }
        val s = messageConfiguration.message.noPermission
        simpleMessage.sendMessage(sender, s, Placeholder.unparsed("permission", permission))
        return false
    }

    fun executeHelp(sender: CommandSender) {
        val audience = simpleMessage.bukkitAudiences.sender(sender)

        val s = messageConfiguration.message.help
        s.forEach {
            simpleMessage.sendMessage(audience, it)
        }
    }

    fun executeImportResidence(sender: CommandSender) {
        val audience = simpleMessage.bukkitAudiences.sender(sender)

        if (residenceInstance == null) {
            val s = messageConfiguration.message.notInstall
            simpleMessage.sendMessage(audience, s)
            return
        }

        val claimedResidences = residenceInstance.residenceManager.getResidences().values
        if (claimedResidences.isEmpty()) {
            val s = messageConfiguration.message.importSection.zero
            simpleMessage.sendMessage(audience, s)
            return
        }

        val serverName = mainConfiguration.simpleConfiguration.serverName

        val names = hashSetOf<String>()

        val players = hashMapOf<UUID, IReadOnlyPlayer>()
        val residences = ArrayList<IReadOnlyResidence>()

        claimedResidences.forEach {
            val residenceName = it.name
            val u = it.ownerUUID
            val n = it.owner

            val permissions = it.permissions
            val residenceFlags = HashMap(permissions.flags)

            val playerFlags = HashMap<UUID, HashMap<String, Boolean>>()
            permissions.playerFlags.forEach { (playerUuid, flags) ->
                playerFlags[playerUuid] = HashMap(flags)
            }

            val p = AllRepository.Player(u, n)
            val r = AllRepository.Residence(residenceName, u, residenceFlags, playerFlags, serverName)

            names.add(residenceName)
            players[u] = p
            residences.add(r)
        }

        scope.launch {
            databaseManager.executeTransaction({ connection ->
                val globalNames = allRepository.selectResidenceNames(connection)

                val duplicates = globalNames.intersect(names)
                if (duplicates.isNotEmpty()) {
                    val s2 = messageConfiguration.message.importSection.retry
                    val s3 = messageConfiguration.message.importSection.duplicate
                    simpleMessage.sendMessage(audience, s2)
                    simpleMessage.sendMessage(audience, s3, Placeholder.unparsed("duplicates", duplicates.toString()))
                    return@executeTransaction
                }

                allRepository.updateInsertPlayersBatch(connection, players.values.toList())
                allRepository.insertResidencesBatch(connection, residences)

                val s = messageConfiguration.message.importSection.success
                simpleMessage.sendMessage(audience, s, Placeholder.unparsed("count", claimedResidences.size.toString()))
            })

        }
    }

    fun executeReloadMessages(sender: CommandSender) {
        try {
            messageConfiguration.reload()
            val s = messageConfiguration.message.reloadSection.success
            simpleMessage.sendMessage(sender, s)
        } catch (e: Exception) {
            val s = messageConfiguration.message.reloadSection.failed
            simpleMessage.sendMessage(sender, s, Placeholder.unparsed("error", e.message ?: "error"))
        }
    }




    fun executeList(sender: CommandSender, page: Int = 1, pageSize: Int = 10) {
        val audience = simpleMessage.bukkitAudiences.sender(sender)
        val listSection = messageConfiguration.message.listSection

        // 检查是否为玩家
        if (sender !is Player) {
            val s = messageConfiguration.message.onlyPlayer
            simpleMessage.sendMessage(audience, s)
            return
        }

        // 获取数据 (String 集合)
        val residences = allCache.fetchSortedResidencesByPlayerUuid()[sender.uniqueId]
        if (residences == null || residences.isEmpty()) {
            val s = listSection.zeroOwner
            simpleMessage.sendMessage(audience, s)
            return
        }

        // 调用通用分页
        sendPaginatedView(
            audience = audience,
            residences = residences,
            page = page,
            pageSize = pageSize,
            headerTemplate = listSection.headerOwner,
            entryTemplate = listSection.entryPlayer, // 自己看不需要显示名字
            commandFormat = "$pluginNameLowercase list ${sender.name} %page%",     // 生成 /rb list 2
            headerExtraResolver = TagResolver.empty()
        )
    }

    /**
     * 2. 查看他人的领地
     */
    fun executeList(sender: CommandSender, targetName: String, page: Int = 1, pageSize: Int = 10) {
        val audience = simpleMessage.bukkitAudiences.sender(sender)
        val listSection = messageConfiguration.message.listSection

        // 查找目标玩家
        val targetPlayer = allCache.fetchPlayersByName()[targetName]
        if (targetPlayer == null) {
            val s = messageConfiguration.message.noPlayer
            simpleMessage.sendMessage(audience, s, Placeholder.unparsed("target", targetName))
            return
        }

        // 获取数据
        val residences = allCache.fetchSortedResidencesByPlayerUuid()[targetPlayer.uuid]
        if (residences == null || residences.isEmpty()) {
            val s = listSection.zero
            simpleMessage.sendMessage(audience, s, Placeholder.unparsed("target", targetName))
            return
        }

        // 调用通用分页
        sendPaginatedView(
            audience = audience,
            residences = residences,
            page = page,
            pageSize = pageSize,
            headerTemplate = listSection.headerOther,
            entryTemplate = listSection.entryAll,     // 列表显示玩家名
            commandFormat = "$pluginNameLowercase list $targetName %page%", // 生成 /rb list 2 Steve
            headerExtraResolver = TagResolver.resolver(
                Placeholder.unparsed("target", targetName)  // Header 需要 <target>
            )
        )
    }

    /**
     * 3. 查看所有领地
     */
    fun executeListAll(sender: CommandSender, page: Int = 1, pageSize: Int = 10) {
        val audience = simpleMessage.bukkitAudiences.sender(sender)
        val listSection = messageConfiguration.message.listSection

        val residences = allCache.fetchSortedResidences()

        // 调用通用分页
        sendPaginatedView(
            audience = audience,
            residences = residences,
            page = page,
            pageSize = pageSize,
            headerTemplate = listSection.headerAll,
            entryTemplate = listSection.entryAll, // 列表显示玩家名
            commandFormat = "$pluginNameLowercase listall %page%",
            headerExtraResolver = TagResolver.empty()
        )
    }

    /**
     * ========================================================
     * 通用分页渲染逻辑 (核心函数)
     * ========================================================
     */
    private fun sendPaginatedView(
        audience: Audience,
        residences: Set<IReadOnlyResidence>,
        page: Int,
        pageSize: Int,
        headerTemplate: String,
        headerExtraResolver: TagResolver,
        entryTemplate: String,
        commandFormat: String
    ) {
        val listConfig = messageConfiguration.message.listSection
        val count = residences.size
        val totalPages = ((count + pageSize - 1) / pageSize).coerceAtLeast(1)

        // 1. 页码错误拦截
        if (page < 1 || page > totalPages) {
            val s = listConfig.pageError
            val tagResolver = TagResolver.resolver(
                Placeholder.unparsed("page", page.toString()),
                Placeholder.unparsed("total_pages", totalPages.toString())
            )
            simpleMessage.sendMessage(audience, s, tagResolver)
            return
        }

        // 2. 发送 Header
        val headerResolver = TagResolver.resolver(
            headerExtraResolver, // 包含 target 等外部变量
            Placeholder.unparsed("count", count.toString())
        )
        simpleMessage.sendMessage(audience, headerTemplate, headerResolver)

        // 3. 截取数据 (使用 Sequence 优化性能)
        val pageItems = residences.asSequence()
            .drop((page - 1) * pageSize)
            .take(pageSize)
        // 注意：这里不需要 .toList()，直接 forEach 遍历 Sequence 即可

        // 4. 遍历渲染条目
        pageItems.forEach {
            val ownerName = allCache.fetchPlayersByUuid()[it.ownerUuid]?.name ?: "Unknown"

            val residenceName = it.name

            val entryResolver = TagResolver.resolver(
                Placeholder.unparsed("residence", residenceName),
                Placeholder.unparsed("owner", ownerName),
                Placeholder.unparsed("server", it.serverName),
                Placeholder.parsed("teleport_command", "residence tp $residenceName")
            )

            simpleMessage.sendMessage(audience, entryTemplate, entryResolver)
        }

        // 5. 发送按钮
        val previousPage = (page - 1).coerceAtLeast(1)
        val nextPage = (page + 1).coerceAtMost(totalPages)

        // 替换命令中的 %page%
        val prevCommand = commandFormat.replace("%page%", previousPage.toString())
        val nextCommand = commandFormat.replace("%page%", nextPage.toString())

        val footerResolver = TagResolver.resolver(
            Placeholder.parsed("previous_command", prevCommand),
            Placeholder.unparsed("page", page.toString()),
            Placeholder.unparsed("total_pages", totalPages.toString()),
            Placeholder.parsed("next_command", nextCommand)
        )

        val s = listConfig.footer
        simpleMessage.sendMessage(audience, s, footerResolver)
    }

    fun executeTeleport(sender: CommandSender, targetName: String?, residenceName: String) {
        val audience = simpleMessage.bukkitAudiences.sender(sender)

        val targetPlayer= if (targetName != null) {
            // A. 指定了目标 -> 查找在线玩家
            val player = residenceBridge.server.getPlayerExact(targetName)
            if (player == null) {
                val s = messageConfiguration.message.playerOffline
                simpleMessage.sendMessage(audience, s, Placeholder.unparsed("target", targetName))
                return
            }
            player
        } else {
            // B. 未指定目标 -> 目标是自己 (Sender)
            if (sender !is Player) {
                val s = messageConfiguration.message.onlyPlayer
                simpleMessage.sendMessage(audience, s)
                return
            }
            sender
        }

        scope.launch {
            databaseManager.dataSource.connection.use { connection ->
                val residence = allRepository.selectResidence(connection, residenceName)

                // 数据库也没找到 -> 彻底不存在
                if (residence == null) {
                    val s = messageConfiguration.message.teleportSection.notExist
                    simpleMessage.sendMessage(audience, s, Placeholder.unparsed("residence", residenceName))
                    return@launch
                }

                // 权限检查逻辑
                val uuid = targetPlayer.uniqueId
                val hasPermission =
                    sender.hasPermission("residence.admin.tp") || // 发送者有管理权限 (强制传送)
                            uuid == residence.ownerUuid ||                   // 目标是领地主人
                            residence.residenceFlags["tp"] == true ||       // 领地开启了 tp
                            residence.playerFlags[uuid]?.get("tp") == true   // 目标有单独的 tp 权限

                if (hasPermission) {
                    // 执行跨服传送
                    residenceTeleport.global(targetPlayer, residence.name, residence.serverName)

                    val s = messageConfiguration.message.teleportSection.targetTo
                    val tagResolver = TagResolver.resolver(
                        Placeholder.unparsed("target", targetPlayer.name),
                        Placeholder.unparsed("residence", residenceName)
                    )
                    simpleMessage.sendMessage(audience, s, tagResolver)

                } else {
                    val s= messageConfiguration.message.noPermission
                    simpleMessage.sendMessage(audience, s)
                }
            }
        }
    }





}