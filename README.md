spigot 构建:

- 创建 ResidenceBridge-Spigot/libraries
- 放入最新版本领地并重命名为 Residence.jar
- 执行 gradle ResidenceBridge-Spigot:build

bungee 构建:

- 执行 gradle ResidenceBridge-Bungee:build

velocity 构建:

- 执行 gradle ResidenceBridge-Velocity:shadowJar

全部构建:

- 执行 gradle build

构建完成的 jar 在各个目录的 build/libs