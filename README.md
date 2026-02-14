## Minecraft SAST Login
### 使用 SAST 飞书 / SAST Link 登录 MC 服务器（MC 1.20.1 / 1.21.11 Fabric 服务端插件）

SAST Login 插件通过禁止未认证的用户执行放置方块、移动、输入命令或使用物品栏等操作来保护服务器免受恶意用户的破坏，用户登录服务器后使用 SAST 飞书 / SAST Link 账号进行认证。

### Feature
- 支持使用 SAST 飞书 / SAST Link 账号通过 OAuth 认证登录服务器
- 允许正版用户和离线用户共存于服务器
- 正版用户登录成功后无需再次验证，离线用户每次均进行验证
- 认证成功后支持自动通知 MC 服务端通过用户验证请求，若与服务端通信失败则允许用户使用 /bind <token> 命令手动绑定
- 支持正版用户使用 /migrate <离线用户名> 命令将离线账号数据迁移到正版账号上

### 依赖项

- Java 17+
- Minecraft 1.20.1 / 1.21.11
- Fabric Loader 0.13.2+
- Fabric API

### 使用说明
1. 下载适合 MC 版本的 `sast_login-mc<mc_version>-<version>.jar` 文件放入 MC 服务端的 `mods` 目录下
2. 编辑 `server.properties` 文件，将 `online-mode` 设置为 `false`
3. 在服务端目录下创建 `config/jwt-secret.txt` 文件，填写一个随机的 JWT 密钥
4. 启动 MC 服务端，插件会自动生成 `config/login-info.json` 文件，用于记录正版用户的认证信息
5. 部署认证页面，详见 [xjzsq/mc_sast_login_backend](https://github.com/xjzsq/mc_sast_login_backend) 仓库

### 版权声明
本项目采用 MIT 开源许可证，部分代码参考了 [NikitaCartes/EasyAuth](https://github.com/NikitaCartes/EasyAuth) 项目