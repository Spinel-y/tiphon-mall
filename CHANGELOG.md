# 更新日志

本项目的改动记录，格式参照 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

## [Unreleased]

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

### 变更

- 配置外置：数据库连接支持 `DB_URL` / `DB_USER` / `DB_PASSWORD` 环境变量覆盖，密钥全部移出源码
- 聊天页 `BASE_URL` 改为相对路径，换端口/部署不再需要改代码
- 商城页商品优先走 `/api/products`，接口不可用时回落到内置数据
- 未配置 API Key 时，客服对话返回 400 + 中文提示（原先直接 500）
- `pom.xml` 整理：修正 `${langchain4j.verion}` 拼写、补充元信息、按功能分组，
  移除未被使用的 `langchain4j-community-dashscope-spring-boot-starter`
- 冒烟测试改用 H2 内存库，`mvn package` 不再依赖本机 MySQL

### 移除

- 订单相关死代码：`Order`、`OrderItem`、`OrderRepository`（购物车仍走 localStorage，订单不落库）
- `AiConfig` 中两处硬编码的 DashScope Key，改为 `@Value` 注入
- `pom.xml` 中的空壳元数据（空 `<url/>`、空 `licenses`/`developers`/`scm`）

### 修复

- 登录页原先只写 `sessionStorage` 不校验，任何页面都能直接访问；现在统一由 Spring Security 拦截
- 商品无图时前端显示空白；现在接口返回按需补图地址，首次浏览即触发抓取
- 管理后台保存 AI Key 会覆盖掉数据库配置（写文件时改为合并已有属性）