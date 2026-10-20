# 龙蛋 AI · 电商视觉内容生成工作台

面向电商商家与内容团队的 AI 视觉生产系统。用户上传商品图后，系统先做视觉理解、再生成可确认的图片规划，确认后调用图片模型产出真实可用的电商主图与详情图；同时提供带 RAG 检索的智能电商问答，以及从剧情到分镜的短视频脚本生成链路。

前端品牌名取自 `egg-user-web` 的 `VITE_APP_TITLE`（龙蛋 AI 电商视觉工作台）。后端代码由 RuoYi / Egg 脚手架改造而来，保留了原有的后台管理能力，产品方向已调整为 AI 电商视觉平台。

---

## 一、当前进度

仓库里同时存在"已跑通的业务闭环"和"仅为占位的前端页面"，请按下表理解代码，不要被页面数量误导。

| 能力 | 状态 | 说明 |
|---|---|---|
| 前台手机号登录 / 自动注册 / 重置密码 | 已实现 | 用户写入 `sys_user`，`user_type=03`，自动绑定 `common` 角色 |
| 全品类商品图生成 | 已实现 | 上传→视觉分析→确认规划→出图→合成→审核，产出落阿里 OSS |
| 商品库 / 商品详情 | 已实现 | 仅保存生成成功的商品；支持重命名、复用、下载、删除 |
| 智能聊天 + RAG | 已实现 | SSE 流式输出，检索来源写入 `ai_chat_retrieval_sources` |
| 剧情生成（文本链路） | 已实现 | 剧情→剧本→分镜→分镜校验→三视图提示词，支持失败节点重试与 Word/MD 导出 |
| 任务中心 | 已实现 | 商品图与剧情任务统一入口，静默轮询、失败重试、取消 |
| 后台提示词模板管理 | 已实现 | `ai_prompt_templates`，正文按 JSONB 存储 |
| 后台 RAG 知识库 | 已实现 | 多文件上传（pdf/doc/docx/md/xls/xlsx/txt，单文件 100MB），Tika 解析 + 分块 + 向量化 |
| 后台敏感词管理 | 已实现 | 敏感词分类与词条维护 |
| 后台随机选号工具 | 已实现 | 双色球 / 大乐透 / 七乐彩，含历史开奖记录 |
| 风格复刻 / 服装组图 / 批量精修 | 仅壳 | 复用 `GeneratorWorkspace`，未接后端任务 |
| 素材库 / 工作台 / 配置说明 | 仅壳 | 页面数据来自 `src/data/mock.js` |
| 万能画布 / 电商视频 / 价格 / A2A | 仅壳 | 按项目宪法属一期非目标或末位交付项 |

> 项目宪法明确：一期不做积分、计费、支付、租户、团队空间和真实视频生成。占位页面是产品表达，不是待修 bug。

---

## 二、技术栈

| 层级 | 技术 | 版本 / 说明 |
|---|---|---|
| 后端 | Java + Spring Boot | Java 17，Spring Boot 4.0.6 |
| 持久层 | MyBatis + PageHelper | MyBatis Starter 4.0.1，分页方言 PostgreSQL |
| 数据库 | PostgreSQL + pgvector | Docker 镜像 `pgvector/pgvector:pg17` |
| 缓存 | Redis | 登录态、Token、任务短期状态 |
| 向量库 | Milvus / 内存实现双通道 | `EGG_AI_VECTOR_STORE` 切换，本地可退化到内存 |
| AI 框架 | Spring AI | 2.0.0（BOM 方式引入） |
| 对象存储 | 阿里云 OSS | 生成图必须转存，不依赖模型临时 URL |
| 后台前端 | Vue 2 + Element UI | `egg-ui`，Vue CLI 构建，端口默认 80 |
| 前台前端 | Vue 3 + Vite 7 + Element Plus | `egg-user-web`，开发端口 5177 |
| 接口文档 | springdoc-openapi | `/swagger-ui.html`，仅扫描 `controller.ai` 包 |

### 已接入的模型

| 用途 | 提供方 | 模型 | 配置前缀 |
|---|---|---|---|
| 文本生成 | DashScope | `qwen3.7-max` | `egg.ai.model-gateway.text` |
| 视觉理解 / 视觉审核 | DashScope | `qwen3.7-plus` | `egg.ai.model-gateway.vision` |
| 图片生成 | DashScope（通义万相） | `wan2.7-image-pro` | `egg.ai.model-gateway.image` |
| Embedding | DashScope | `text-embedding-v4`（1536 维） | `egg.ai.model-gateway.embedding` |
| 剧情生成 | DeepSeek | `deepseek-v4-pro` | `egg.ai.model-gateway.story` |

所有 provider 都支持 `mock` 回退，本地无密钥也能启动，但只会拿到占位内容。

---

## 三、目录结构

```
egg_commerce_ai/
├── egg-admin/          后端 Web 入口、Controller、启动类、清理定时任务
├── egg-framework/      安全、Web、Redis、数据源、模型网关配置
├── egg-system/         领域模型、Mapper、业务服务（AI 业务核心都在这里）
├── egg-common/         通用工具、常量、注解、异常
├── egg-quartz/         定时任务模块（脚手架自带）
├── egg-generator/      代码生成模块（脚手架自带）
├── egg-ui/             后台管理前端（Vue 2 + Element UI）
├── egg-user-web/       前台用户工作台（Vue 3 + Vite）
├── sql/                建表与种子脚本
├── docker/             PostgreSQL 初始化脚本
├── docs/               项目宪法、改动记录、功能地图
└── compose.postgres.yml 本地依赖编排（PostgreSQL + Redis）
```

### 后端 AI 业务分区

| 包路径 | 职责 |
|---|---|
| `service/ai/model` | 模型网关：文本、视觉、图片、Embedding 的统一出口 |
| `service/ai/product` | 商品图任务编排、资产工作流、Java2D 文字合成、审核、清理 |
| `service/ai/chat` | 聊天会话、消息、RAG 增强对话、SSE 流式 |
| `service/ai/rag` | 文档解析、分块、向量写入（Milvus / 内存） |
| `service/ai/knowledge` | 知识库文档元数据与向量化状态 |
| `service/ai/story` | 剧情任务固定节点链、重试恢复、Word/MD 导出 |
| `service/ai/prompt` | 提示词模板 CRUD |
| `service/ai/sensitive` | 敏感词与分类 |
| `service/ai/storage` | OSS 上传、签名 URL、MIME 推导 |

---

## 四、本地启动

### 1. 启动依赖

先启动 Docker Desktop，然后在项目根目录执行：

```bash
docker compose -f compose.postgres.yml up -d
docker compose -f compose.postgres.yml ps
```

默认暴露：PostgreSQL `5432`、Redis `6381`（容器内 6379 映射到宿主机 6381）。

### 2. 初始化数据库

首次启动会由 `docker/postgres/init/001-enable-vector.sql` 自动启用 pgvector 扩展。业务表需要手动导入，建议顺序如下（与 `docs/change-log.md` 中的引入顺序一致）：

```bash
docker cp ./sql/egg_postgresql.sql egg-postgres:/tmp/01.sql
docker exec -v ON_ERROR_STOP=1 egg-postgres psql -U egg -d egg -f /tmp/01.sql
```

随后依次导入：

```
sql/egg_postgresql.sql                 脚手架基础表
sql/ai_knowledge_sensitive_rag.sql     知识库、敏感词、聊天会话
sql/ai_prompt_template_management.sql  提示词模板
sql/ai_product_image_generation.sql    商品资产、商品任务
sql/ai_story_generation.sql            剧情任务、平台规则
sql/lottery_random_picker.sql          随机选号（可选）
sql/quartz.sql                         定时任务表（可选）
```

> 若某个脚本因外键或菜单依赖报错，按报错信息调整顺序即可，上述顺序来自改动记录而非脚本内声明。

### 3. 配置后端环境变量

复制 `.env.example` 为 `.env` 并填写密钥。注意：**`.env` 只被 `docker compose` 读取**；后端进程需要系统环境变量或 IDEA 的 Run Configuration 环境变量，两者不会自动打通。

至少要填：

```bash
EGG_DEEPSEEK_API_KEY=            # 剧情生成
EGG_DASHSCOPE_API_KEY=           # 文本 / 视觉 / 图片 / Embedding
EGG_ALIOSS_ACCESS_KEY_ID=        # 对象存储
EGG_ALIOSS_ACCESS_KEY_SECRET=
```

关键默认值（见 `egg-admin/src/main/resources/application.yml`）：

```text
EGG_AI_MOCK_ENABLED=true         本地默认走 mock，不填密钥也能起
EGG_MODEL_IMAGE_PROVIDER=dashscope
EGG_MODEL_STORY_PROVIDER=deepseek
EGG_AI_VECTOR_STORE=milvus       设为内存实现或确保 Milvus 已启动
EGG_MILVUS_INITIALIZE_SCHEMA=false  已有 collection 时保持关闭
```

### 4. 启动后端

```bash
mvn clean package -DskipTests
java -jar egg-admin/target/egg-admin.jar
```

或在 IDEA 中直接运行 `com.egg.commerce.EggApplication`。默认端口 `8080`，接口文档 `/swagger-ui.html`。

### 5. 启动前端

后台管理端（Egg 风格）：

```bash
cd egg-ui
npm install
npm run dev
```

前台用户工作台：

```bash
cd egg-user-web
npm install
npm run dev      # http://localhost:5177
```

前台开发代理会把 `/api` 前缀剥离后转发到 `VITE_API_PROXY_TARGET`（默认 `http://localhost:8080`）。后端 Controller 本身没有 `/api` 前缀，改动代理规则时注意保持一致。

---

## 五、核心业务流程

### 商品图生成

```
上传源图(OSS) → 视觉分析 → 确认规划(等待用户) → 逐方案出图 → Java2D 文字合成 → 审核 → 入商品库
```

- 任务在 `status=waiting_confirmation` 处停下，用户确认后才进入生成，避免白烧图片额度。
- 规划必须给出足量且互不相同的 `imagePrompts`，不足时自动重规划最多 2 次。
- 模型原始输出存为 `generated_base`，后端合成后的图存为 `generated`，前台只展示和下载后者。
- 审核不阻断任务：`ruleAudit` / `visionAudit` / `auditSummary` 写入 `generation_output`，风险以角标呈现。
- `图片带文字=否` 会强制 `textOverlay.enabled=false`，跳过合成，直接返回底图。
- 定时清理：超过 12 小时的草稿上传、以及 OSS 中缺少数据库记录的孤儿对象。

### 剧情生成

```
剧情生成 → 剧本生成 → 分镜生成 → 分镜校验 → 三视图/场景图
```

- 五个节点串行执行，全部输出存在同一行 `ai_story_tasks` 里，不做节点级拆表。
- 失败重试从失败节点开始，或回溯到最近一个上游输出可用的节点；已成功的上游内容保留。
- 三视图/场景图节点当前只产出提示词文本，尚未调用图片模型。
- 平台专用提示词按名称前缀匹配：`抖音-`、`快手-`、`视频号-`、`小红书-`。

### 智能聊天

- 会话、消息、检索来源分表存储：`ai_chat_sessions` / `ai_chat_messages` / `ai_chat_retrieval_sources`。
- 检索参数：`egg.ai.rag.top-k`（默认 6）、`egg.ai.rag.min-score`（默认 0.62）。
- 流式接口 `POST /ai/chat/session/{id}/message/stream` 以 SSE 返回。

---

## 六、接口分区

| 前缀 | 说明 |
|---|---|
| `/ai/product/**` | 商品资产、商品图任务、确认/重规划/取消、AI 帮写 |
| `/ai/chat/**` | 会话列表、创建、消息、流式发送 |
| `/ai/story/**` | 剧情任务提交、查询、重试、导出 |
| `/ai/prompt/**` | 提示词模板管理（后台） |
| `/ai/knowledge/**` | 知识库文档上传、批量上传、下载（后台） |
| `/ai/sensitive/**` | 敏感词与分类（后台） |
| `/system/**`、`/monitor/**`、`/tool/**` | 脚手架自带的后台能力 |

---

## 七、项目文档

| 文件 | 作用 |
|---|---|
| `docs/project-constitution.md` | 项目宪法，最高优先级约束，当前 v1.2.2 |
| `docs/feature-map.md` | 功能到代码路径的索引，改代码前先查这里 |
| `docs/change-log.md` | 按日期记录的改动流水，含"为什么改" |
| `docs/phase-one-module-configuration.md` | 各外部依赖的配置原因与方式 |
| `docs/postgresql-migration.md` | 从 MySQL 迁移到 PostgreSQL 的步骤 |
| `AGENTS.md` | 给 AI Agent 的入口规则，要求先读宪法 |

### 开发约定

1. 动手前读 `docs/project-constitution.md`，与宪法冲突的需求先确认再改。
2. 新增或移动功能代码，同步更新 `docs/feature-map.md`。
3. 每次有意义的改动，在 `docs/change-log.md` 追加一条。
4. 业务逻辑、AI 流程、存储、集成等非显而易见处必须写注释；不要写复述语法的注释。

### 测试

- 后端：`mvn test`，`egg-system` 与 `egg-framework` 下有 20+ 个服务级测试。
- 前台：测试文件为 Node 原生 `node:test`（如 `egg-user-web/src/pages/TasksPage.test.js`），可直接用 `node --test` 运行，仓库未配置 npm script。

---

## 八、已知坑

| 现象 | 原因 |
|---|---|
| 后端连不上 Redis | `application.yml` 中 Redis host/port 写死 `localhost:6379`，而 compose 映射到 `6381`，需两者对齐 |
| 启动报 Milvus 连接失败 | `EGG_AI_VECTOR_STORE` 默认 `milvus`，未启动 Milvus 时应切到内存实现 |
| 知识库上传成功但向量化失败 | 检查 DashScope 密钥、Tika 解析结果与批量大小（单批上限 10 个 chunk） |
| 剧情任务输出像提示词本身 | `EGG_MODEL_STORY_PROVIDER` 仍是 `mock` |
| 后台新增页面 404 | 菜单存的组件路径需带 `.vue`，动态路由解析对此敏感 |

---

## 九、许可证

本仓库根目录的 `LICENSE` 为 MIT 协议，版权声明为 `Copyright (c) 2018 RuoYi`，来自上游脚手架。本项目在其基础上做了大量二次开发，但依据 MIT 协议"版权声明与许可声明须包含在软件的所有副本或实质性部分中"的要求，原始版权行必须保留。

如需声明自有版权，正确做法是在同一文件中追加一行，而不是替换原行：

```
Copyright (c) 2018 RuoYi
Copyright (c) 2026 <你的名字或组织>
```

具体授权安排建议按团队实际需求确认，本段仅为对 MIT 条款的说明，不构成法律意见。

---

## 十、致谢

后端骨架、权限体系、代码生成器与后台管理界面来自 RuoYi / Egg 生态；AI 能力分别接入阿里云 DashScope（通义千问 / 通义万相）与 DeepSeek。
