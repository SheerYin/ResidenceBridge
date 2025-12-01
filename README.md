构建：
1. 创建 spigot/libraries
2. 放入最新版本领地并重命名为 Residence.jar
3. gradle build
- 构建完成的 jar 在各个目录的 build/libs
- 没有 shadow 后缀的 jar 在启动时使用 libraries 下载依赖
- 如果不想启动时下载依赖，就用有 shadow 后缀的 jar
- velocity 版本只能使用 shadow 后缀的 jar

使用方式：
1. bukkit/spigot/paper 服务端及分支放入对应插件
2. 启动，等待 config 生成，关闭
3. 修改 config 并启动
4. 使用命令导入领地，处理重名领地
5. bungeecord/waterfall/velocity 放入对应插件，需要开启端口转发，velocity 也一样

与 Residence 某些功能严重冲突:
- 放下箱子自动创建领地，此功能会无视命名冲突强行放置领地，导致数据库出现重名领地。如果阻止就会带来大量检查开销 - Residence 的 config.yml 的 NewPlayer.Use: true


命令：
- 主命令 /residencebridge 缩写 /rb
- 列表 /rb list \[player\] \[page\]
- 列表 /rb listall \[page\]
- 传送领地 /rb teleport \<residence\> \[player\]
- 导入数据 /rb import
- 重新加载 /rb reload

PlaceholderAPI 变量：
- 玩家领地
- %residencebridge_player_residence_names%
- %residencebridge_player_residence_names_\<index\>%
- %residencebridge_player_residence_count%
- %residencebridge_player_residence_maximum%
- 所有领地
- %residencebridge_server_residence_names%
- %residencebridge_server_residence_names_\<index\>%
- %residencebridge_server_residence_count%
- 公开可传送
- %residencebridge_residence_public_teleport_names%
- %residencebridge_residence_public_teleport_names_\<index\>%