package me.yin.residencebridge.configuration

import me.yin.residencebridge.ResidenceBridge
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Path

class MessageConfiguration(val residenceBridge: ResidenceBridge) {

    val path: Path = residenceBridge.dataFolder.toPath().resolve("message.yml")
    val loader: YamlConfigurationLoader = YamlConfigurationLoader.builder().path(path).nodeStyle(NodeStyle.BLOCK).indent(2).build()

    lateinit var message: Message
        private set

    init {
        reload()
    }

    fun reload() {
        val node = loader.load()

        message = node.get(Message::class.java) ?: throw IllegalStateException("配置格式错误，无法解析为 Message")

        node.set(Message::class.java, message)
        loader.save(node)
    }

    @ConfigSerializable
    class Message(
        val noPermission: String = "<white>[领地桥接] 您没有权限 <permission>",
        val onlyPlayer: String = "<white>[领地桥接] 此命令仅限玩家执行",
        val noPlayer: String = "<white>[领地桥接] 玩家 <target> 不存在",
        val playerOffline: String = "<white>[领地桥接] 玩家 <target> 不在线",
        val notInstall: String = "<white>[领地桥接] 未安装 Residence",
        val help: List<String> = listOf(
            "<white>[领地桥接] 主命令 /residencebridge 缩写 /rb",
            "<white>[领地桥接] 列表 /rb list [player] [page]",
            "<white>[领地桥接] 列表 /rb listall [page]",
            "<white>[领地桥接] 传送领地 /rb teleport <residence> [player]",
            "<white>[领地桥接] 导入数据 /rb import",
            "<white>[领地桥接] 重新加载 /rb reload"
        ),
        val teleportSection: TeleportSection = TeleportSection(),
        val importSection: ImportSection = ImportSection(),
        val reloadSection: ReloadSection = ReloadSection(),
        val createSection: CreateSection = CreateSection(),
        val listSection: ListSection = ListSection()
    ) {

        @ConfigSerializable
        class TeleportSection(
            val notExist: String = "<white>[领地桥接] 领地 <residence> 不存在",
            val targetTo: String = "<white>[领地桥接] 传送 <target> 至 <residence>",
            val noPermission: String = "<white>[领地桥接] 您没有权限传送 <residence> 领地"
        )

        @ConfigSerializable
        class ImportSection(
            val zero: String = "<white>[领地桥接] 没有领地，导入不了什么",
            val success: String = "<white>[领地桥接] 导入领地完成 <count>",
            val duplicate: String = "<white>[领地桥接] 数据库中已有重名领地 <duplicates>",
            val retry: String = "<white>[领地桥接] 请处理同名领地再重试"
        )

        @ConfigSerializable
        class ReloadSection(
            val success: String = "<white>[领地桥接] <green>配置重载成功",
            val failed: String = "<white>[领地桥接] <red>配置重载失败: <error>"
        )

        @ConfigSerializable
        class CreateSection(
            val nameExists: String = "<white>[领地桥接] 领地重名",
            val limit: String = "<white>[领地桥接] 创建领地已达上限 <count> / <maximum>"
        )

        @ConfigSerializable
        class ListSection(
            val zeroOwner: String = "<white>[领地桥接] 你没有领地",
            val zero: String = "<white>[领地桥接] 玩家 <target> 没有领地",
            val pageError: String = "<white>[领地桥接] 页码 <page> 错误！请输入 1 到 <total_pages> 页码",
            val headerOwner: String = "<white>[领地桥接] <gold>我的领地 <gray>（共 <count> 个）",
            val headerOther: String = "<white>[领地桥接] <gold>玩家 <target> 的领地 <gray>（共 <count> 个）",
            val headerAll: String = "<white>[领地桥接] <gold>所有领地 <gray>（共 <count> 个）",
            val entryPlayer: String = "<white>[领地桥接] 领地 <residence> 服务器 <server> <green><click:run_command:'<teleport_command>'><hover:show_text:'<gray>点击传送'>[传送]</hover></click>",
            val entryAll: String = "<white>[领地桥接] 领地 <residence> 玩家 <owner> 服务器 <server> <green><click:run_command:'<teleport_command>'><hover:show_text:'<gray>点击传送'>[传送]</hover></click>",
            val footer: String = "<white>[领地桥接] <green><click:run_command:'<previous_command>'><hover:show_text:'<gray>点击前往上一页'>[上一页]</hover></click> <white>[<page>/<total_pages>] <green><click:run_command:'<next_command>'><hover:show_text:'<gray>点击前往下一页'>[下一页]</hover></click>"
        )
    }

}