# 提丰商城 · tiphon-mall

一个能自己跑起来的「AI 网店」示例：商品商城 + 注册登录 + 会卖货的 AI 客服 + 提丰智能对话页。
后端 Spring Boot 3.5（Java 17），前端是三个原生 HTML 页面（Vue 3 走 CDN，不用 Node 构建），
数据默认 MySQL，也可以零配置切到 H2 文件库直接体验。

## 功能

- **商城**：商品列表、分类筛选、搜索、详情、购物车（本地）、优惠券页
- **账号**：注册 / 登录 / 退出，会话 Cookie + BCrypt 存密码
- **AI 客服**（`gouwupingtai.html` 右下角）：流式回复，知道店铺商品目录；游客也能聊
- **客服形象自定义**：名字、人设、开场白、主题色、头像（内置 emoji 预设 / 图片地址 / 上传），按用户保存
- **商品图**：库里没图时按需去 360 图片搜索抓一张并落盘；管理员可单张重抓或上传替换
- **提丰智能对话页**（`index.html`）：另一个带工具调用的 AI 助手（音乐播放、相册、小游戏、生图），
  左下角「设置」里可直接填 API Key，填完即生效、可随时改

## 技术栈

| 层 | 用了什么 |
| --- | --- |
| 后端 | Spring Boot 3.5.14、Spring Web / WebFlux、Spring Data JPA、Spring Security（session + BCrypt） |
| 数据库 | MySQL 8（默认）或 H2 文件库（`h2` profile，演示用） |
| AI | LangChain4j 1.0（DeepSeek `deepseek-chat` + 阿里云百炼 `qwen3-vl-plus` / `wan2.5-t2i-preview`） |
| 前端 | 原生 HTML + Vue 3 CDN + Font Awesome，无打包步骤 |

## 目录结构

```
.
├─ pom.xml
├─ application-secret.properties.example   # 复制成 application-secret.properties 填密钥
├─ src/main/java/com/example/springbootdemo/
│  ├─ config/       SecurityConfig、AiConfig、CustomerAssistant
│  ├─ controller/   Auth / Product / AdminProduct / Cs / Media / Music / Ai / Settings / Home
│  ├─ entity/       User、Product、CsProfile
│  ├─ repo/         JPA 仓库
│  ├─ seed/         DataSeeder（35 条商品 + 管理员）
│  └─ service/      AiRuntime、ApiKeyStore、ProductImageService、ShopContextService…
└─ src/main/resources/
   ├─ application.properties / application-h2.properties
   ├─ products.json          # 商品种子数据
   └─ static/                # index.html（对话页）、gouwupingtai.html（商城）、login.html
```

运行时才会生成的目录：`product-images/`（商品图）、`ai-images/`（上传与生图）、`music/`（音乐，需自备）、
`data/`（H2 数据库）、`logs/`。这些都已写进 `.gitignore`。

## 快速开始

### 0. 准备

- JDK 17+
- Maven 3.6+（IDEA 自带的也行：`Settings → Build Tools → Maven`）
- 首次构建需要联网下载依赖

```bash
git clone <你的仓库地址>
cd <项目目录>
```

### 方式 A：H2 演示模式（不装 MySQL，最快看到页面）

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

打开 <http://localhost:8899> 就是商城页；右上角 👤 登录后进对话页。
首次启动会在项目目录生成 `data/tiphon.mv.db`，并自动灌 35 条商品；
管理员 `admin` 的密码如果没配置，会以 WARN 日志打印在启动日志里（形如
`没有配置 tiphon.admin.password，已为 admin 生成临时密码：xxxxxxxxxxxx`），记得抄下来。

### 方式 B：MySQL（接近生产）

1. 建库和账号（用 root 或 Navicat 执行）：

```sql
CREATE DATABASE tiphon DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'tiphon_app'@'localhost' IDENTIFIED BY '换成你自己的密码';
GRANT ALL PRIVILEGES ON tiphon.* TO 'tiphon_app'@'localhost';
FLUSH PRIVILEGES;
```

2. 把连接信息给应用，任选一种：

- 环境变量：`DB_URL`、`DB_USER`、`DB_PASSWORD`
- 或写进 `application-secret.properties`：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/tiphon?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=tiphon_app
spring.datasource.password=你的密码
```

3. 启动：

```bash
mvn spring-boot:run
```

表结构由 JPA 自动创建（`ddl-auto=update`），商品和管理员同样自动补齐。

### 3. 配置 AI（可选，但客服和对话页要用）

```bash
copy application-secret.properties.example application-secret.properties   # Windows
cp application-secret.properties.example application-secret.properties     # macOS/Linux
```

填两项：

```properties
# DeepSeek：https://platform.deepseek.com/api_keys
langchain4j.open-ai.chat-model.api-key=sk-你的DeepSeekKey
# 阿里云百炼 DashScope（地域选「北京」）：https://bailian.console.aliyun.com/?apiKey=1
dashscope.api-key=sk-你的DashScopeKey
```

不想手改也行：打开对话页，点左下角 🔑「设置」，粘进去保存即可——
它会写回项目根目录的 `application-secret.properties`，重启不丢，随时能改。

没配 Key 也能正常启动、逛商城；只是 AI 客服和对话会提示「还没配置 API Key」。

### 5. 打包成 jar

```bash
mvn package                       # 产物：target/tiphon-mall-0.0.1-SNAPSHOT.jar
java -jar target/tiphon-mall-0.0.1-SNAPSHOT.jar               # 用 MySQL
java -jar target/tiphon-mall-0.0.1-SNAPSHOT.jar --spring.profiles.active=h2
```

`mvn package` 里带了一个 H2 内存库的冒烟测试，不依赖本机 MySQL；想跳过加 `-DskipTests`。

### 6. 管理员

用户名固定 `admin`，密码来自 `tiphon.admin.password`；没配就是启动日志里那个随机密码。
登录后商城头部会多一个「管理」入口，可以给商品重抓图片或上传本地图。
想换密码：改配置重启（已存在的 admin 不会被覆盖），或直接在数据库里把该账号删掉重启一次。

## 注意事项 / 常见坑

- **H2 只读报错**：如果进程被强杀（或断电），下次启动可能报
  `SQL Error: 90097 ... The database is read only`。删掉 `data/` 目录重启即可（演示数据会重新生成）。
- **图片抓取要联网**，来自 360 图片搜索，质量参差、可能带第三方水印，仅作演示；
  正式用途请换成自己的图或上传。抓图失败不影响页面，会退回内置 SVG 占位图。
- **首次浏览某商品会慢 1–2 秒**：那张图是那一刻才去抓的，之后走本地缓存。
- **music/ 没有随仓库提供**：`music/` 里的 mp3 有版权顾虑，请自己放一首
  （`music/test-song.mp3` 之类），音乐页才播得出东西。
- **端口被占**：`mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8900`
- **Windows 下 curl 发中文 JSON 会乱码**（GBK），测试接口请用浏览器 / Postman / node 脚本。
- **CSRF 已关闭**、会话 Cookie 未加 `Secure`，这是演示配置；真要上线请开 HTTPS 并重新评估安全设置。

## 主要接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/register`、`/api/auth/login`、`/api/auth/logout` | 注册 / 登录 / 退出 |
| GET | `/api/auth/me` | 当前用户，未登录 401 |
| GET | `/api/products?cat=&q=`、`/api/products/{id}` | 商品列表 / 详情 |
| GET/PUT | `/api/cs/profile` | 客服形象（名字、人设、主题色、头像、开场白） |
| POST | `/api/cs/avatar` | 上传客服头像（字段名 `avatar`） |
| POST | `/api/cs/chat` | 客服流式对话（SSE 文本流） |
| POST | `/api/admin/products/{id}/image/refetch`、`/image/upload` | 管理员换图 |
| POST | `/api/admin/products/images/refetch-all` | 批量重抓 |
| GET/POST | `/api/settings/keys` | 读取（脱敏）/ 保存 AI Key |
| GET | `/media/products/{file}`、`/ai/images/{file}` | 商品图、上传图 |

## 隐私与安全

- 仓库里**不含**任何真实 API Key、数据库密码、个人路径。
  所有密钥都在 `application-secret.properties`（已 gitignore）或页面里填写。
- `application-secret.properties.example` 只是模板，值是占位符。
- 图片、音乐、数据库文件、日志都不会进版本库。

## 许可

代码可自由学习、修改、二次分发。商品文案与抓取到的商品图来自公开网络，版权归原作者，
请勿用于商业用途；如需正式开源协议，可在仓库里补一个 `LICENSE` 文件。