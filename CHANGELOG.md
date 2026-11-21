# 更新日志

本项目的改动记录，格式参照 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

## [Unreleased]

（暂无）

## [1.0.1] - 2026-11-21

### 新增

- 会话页人设自定义：左下角新增「人设」按钮，可选预设（猫娘 · 提丰 / 靠谱助手 / 编程搭子），
  并能改名字、怎么称呼你、自称、性格补充、额外要求、开场白；保存后下一句话就生效
- 人设按用户存进新表 `chat_personas`（JPA 自动建表）；没设置过就用预设默认值，不占一行记录
- 接口：`GET/PUT /api/chat/persona`、`POST /api/chat/persona/reset`
- 对话页顶部显示当前人设名字，欢迎气泡改用自定义开场白

### 变更

- `Assistant` 的人设从写死的 `@SystemMessage` 改为每次调用传入 `systemPrompt`，改完不用重启
- 原猫娘人设重写为预设 `cat`；工具调用规则抽成公共段落，换任何预设都不会丢听歌 / 游戏 / 相册 / 生图标记
- 三套预设的提示词按「语气 / 结构 / 分寸 / 边界」重写；名字、称呼、自称统一拼在开头，
  修掉了「自称『我』或『我』」这类重复
- `/ai/memory_stream_chat` 改为优先用会话里的登录用户，`userId` 参数降级为游客兜底
- 版本号升到 `1.0.1`

## [1.0.0] - 2026-11-20

### 新增

- **数据库落地**：接入 MySQL（JPA `ddl-auto=update`），`users`、`products`、`cs_profiles` 三张表，
  商品数据从页面里的常量迁移到 `products.json` 并由 `DataSeeder` 幂等写入
- **注册登录**：`/api/auth/register|login|logout|me`，Session Cookie + BCrypt 存密码；
  商城头部显示昵称/头像，管理员额外显示「管理」入口
- **AI 客服**：`/api/cs/chat` 流式回复，通过 `ShopContextService` 把店铺信息与 35 条商品目录注入 prompt，
  游客也能聊（按会话隔离记忆）
- **客服形象自定义**：名字、人设、开场白、主题色、头像（emoji 预设 / 图片地址 / 上传）均可改，
  按用户存到 `cs_profiles`，设置面板保存即生效
- **商品图在线抓取**：`ProductImageService` 从 360 图片搜索抓图，按宽高比与最小体积过滤，落盘到
  `product-images/`；`/media/products/{file}` 提供访问，文件缺失时按文件名自动补抓
- **管理员换图**：单张重抓、上传替换、一键批量重抓（`/api/admin/products/images/refetch-all`），
  批量后自动清理没人引用的旧图
- **H2 演示模式**：`application-h2.properties` + `h2` 依赖，`-Dspring-boot.run.profiles=h2` 免装 MySQL 直接跑
- **浏览器内填 Key**：会话页左下角「设置」可填写/修改 DeepSeek、DashScope 的 API Key，
  保存后写回 `application-secret.properties` 并即时重建模型，无需重启
- **文档与工程化**：`README.md`（从零运行、建库 SQL、避坑清单）、`CHANGELOG.md`、
  `.gitignore`（密钥/产物/图片/音乐/数据库文件）、`application-secret.properties.example` 模板
- **排错文档**：README 新增「常见问题」——MySQL 未启动导致退出码 1、端口占用、JDK 版本、
  H2 只读、Key 占位符缺失、浏览器缓存等常见现象与处理
- **界面预览**：README 顶部加入三张截图（商城首页、对话页、客服设置），存放于 `docs/screenshots/`
- **开源许可**：补充 `LICENSE`（MIT），README「许可」段改为指向该文件

### 变更

- 配置外置：数据库连接支持 `DB_URL` / `DB_USER` / `DB_PASSWORD` 环境变量覆盖，密钥全部移出源码
- 聊天页 `BASE_URL` 改为相对路径，换端口/部署不再需要改代码
- 商城页商品优先走 `/api/products`，接口不可用时回落到内置数据
- 未配置 API Key 时，客服对话返回 400 + 中文提示（原先直接 500）
- `pom.xml` 整理：修正 `${langchain4j.verion}` 拼写、补充元信息、按功能分组，
  移除未被使用的 `langchain4j-community-dashscope-spring-boot-starter`
- 冒烟测试改用 H2 内存库，`mvn package` 不再依赖本机 MySQL
- 项目版本定为 `1.0.0`（`pom.xml` 从 `0.0.1-SNAPSHOT` 改为正式版号）
- README 打包命令里的 jar 名同步为 `tiphon-mall-1.0.0.jar`，章节编号去掉残留的 `0.`/`3.`/`5.`/`6.`

### 移除

- 订单相关死代码：`Order`、`OrderItem`、`OrderRepository`（购物车仍走 localStorage，订单不落库）
- `AiConfig` 中两处硬编码的 DashScope Key，改为 `@Value` 注入
- `pom.xml` 中的空壳元数据（空 `<url/>`、空 `licenses`/`developers`/`scm`）

### 修复

- 登录页原先只写 `sessionStorage` 不校验，任何页面都能直接访问；现在统一由 Spring Security 拦截
- 商品无图时前端显示空白；现在接口返回按需补图地址，首次浏览即触发抓取
- 管理后台保存 AI Key 会覆盖掉数据库配置（写文件时改为合并已有属性）

[Unreleased]: https://github.com/Spinel-y/tiphon-mall/compare/v1.0.1...HEAD
[1.0.1]: https://github.com/Spinel-y/tiphon-mall/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/Spinel-y/tiphon-mall/releases/tag/v1.0.0