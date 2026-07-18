# AI 知识库、敏感词管理与 RAG 聊天迁移设计

日期：2026-07-08  
作者：Codex  
状态：已由用户确认方案，待实现计划

## 1. 背景

当前项目是 AI 电商视觉内容生成平台。用户希望从 `D:\Code_Workspace\rag-ai` 参考迁移三个能力：

- 我的知识库
- 敏感词管理
- RAG 聊天

迁移不是原样复制。源项目使用 Vue 3、Element Plus、MyBatis-Plus、阿里 OSS、Milvus 和 Spring AI；当前项目后台是 Egg 管理端风格，后端是模块化 Spring Boot 单体、普通 MyBatis、PostgreSQL，前台是 Picset 风格用户工作台。因此本次设计采用“功能参考迁移、代码按当前项目重写”的方式。

## 2. 已确认需求

1. 后台 `AI配置` 下新增两个菜单：`我的知识库`、`敏感词管理`，与 `提示词模板` 同级。
2. `敏感词管理` 页面内部用 Tab 管理“分类”和“词库”。
3. 前台现有 `/chat` 智能聊天入口改为真实 RAG 聊天页。
4. 知识库使用阿里 OSS 存文件，Milvus 存向量，PostgreSQL 存业务元数据。
5. 上传知识库后异步完成文档解析、切分、向量化和 Milvus 写入。
6. RAG 聊天保存会话、消息和检索来源。
7. 敏感词只做后台分类与词库增删改查，不做文本检测接口。
8. 配置使用环境变量或示例空值，不复制源项目真实 API Key、OSS 密钥或其他凭据。
9. `ai` 文件夹下的功能，Java 类名使用 `Ai` 前缀，数据库表名使用 `ai_` 前缀。

## 3. 宪法修订设计

当前宪法中默认方向是对象存储存文件、PostgreSQL/pgvector 存向量。用户已明确要求将整体 AI 存储方向调整为：

- 阿里 OSS：保存上传文件、商品图片、AI 生成图片、编辑图和导出文件。
- Milvus：保存 RAG 向量。
- PostgreSQL：保存业务元数据、任务状态、聊天会话、聊天消息、检索来源、敏感词、菜单权限、模型调用日志等关系型数据。

实现时应修改 `docs/project-constitution.md` 和相关配置说明，避免后续开发继续按对象存储/pgvector 默认方向推进。该变更不引入多租户、积分、支付、团队空间或真实视频生成。

## 4. 非目标

本次不做：

- 积分、计费、套餐、支付。
- 租户、团队空间、普通用户角色分层。
- A2A 功能。
- 真实视频生成。
- 敏感词文本检测接口。
- 直接复制源项目真实密钥。
- 将业务元数据迁到 MySQL。
- 将后台改成 Picset 风格，后台仍保持 Egg 管理端风格。

## 5. 推荐方案

采用“稳妥重构迁移”：

- 以 `rag-ai` 的功能为参考。
- 后端按当前项目分层重写 controller、domain、mapper、service、mapper XML。
- 后台页面按 Egg Vue 2 + Element UI 风格重写。
- 前台聊天页按当前 `egg-user-web` 的 Picset 风格重写。
- 数据库使用 PostgreSQL 语法和当前项目分页、权限、返回结构。

这样可以满足功能迁移，同时避免把源项目的 MyBatis-Plus、Vue 3 后台风格、MySQL 字段习惯和真实密钥混入当前项目。

## 6. 后端模块设计

### 6.1 知识库

建议路径：

- Controller：`egg-admin/src/main/java/com/egg/commerce/web/controller/ai/knowledge`
- Domain：`egg-system/src/main/java/com/egg/commerce/system/domain/ai/knowledge`
- Mapper：`egg-system/src/main/java/com/egg/commerce/system/mapper/ai/knowledge`
- Service：`egg-system/src/main/java/com/egg/commerce/system/service/ai/knowledge`
- Mapper XML：`egg-system/src/main/resources/mapper/ai/knowledge`

核心类：

- `AiKnowledgeDocumentController`
- `AiKnowledgeDocument`
- `AiKnowledgeDocumentMapper`
- `IAiKnowledgeDocumentService`
- `AiKnowledgeDocumentServiceImpl`
- `AiKnowledgeVectorizationService`

知识库上传流程：

1. 管理员上传文件。
2. 后端校验文件类型、大小和权限。
3. 文件上传到阿里 OSS。
4. PostgreSQL 创建 `ai_knowledge_documents` 记录，状态为 `processing`。
5. 异步服务读取文件内容，解析文档，切分文本。
6. 调用 embedding 模型生成向量并写入 Milvus。
7. 保存 Milvus 文档 ID、切片数量、完成时间，状态改为 `completed`。
8. 如果失败，记录 `failed` 和失败原因，便于后台列表展示。

删除流程：

1. 管理员删除知识库文件。
2. 删除 PostgreSQL 记录前读取 OSS object key 和 Milvus vector IDs。
3. 删除 Milvus 向量。
4. 删除 OSS 对象。
5. 删除或软删除 PostgreSQL 记录。

为了避免外部系统失败造成数据不一致，删除过程需要短注释说明补偿策略。首版可以采用“尽力删除 + 失败原因记录”的方式。

### 6.2 敏感词管理

建议路径：

- Controller：`egg-admin/src/main/java/com/egg/commerce/web/controller/ai/sensitive`
- Domain：`egg-system/src/main/java/com/egg/commerce/system/domain/ai/sensitive`
- Mapper：`egg-system/src/main/java/com/egg/commerce/system/mapper/ai/sensitive`
- Service：`egg-system/src/main/java/com/egg/commerce/system/service/ai/sensitive`
- Mapper XML：`egg-system/src/main/resources/mapper/ai/sensitive`

核心类：

- `AiSensitiveCategoryController`
- `AiSensitiveWordController`
- `AiSensitiveCategory`
- `AiSensitiveWord`
- `AiSensitiveCategoryMapper`
- `AiSensitiveWordMapper`
- `IAiSensitiveCategoryService`
- `IAiSensitiveWordService`

敏感词首版只用于后台维护，不对聊天、RAG 或文案生成提供检测接口。

### 6.3 RAG 聊天

建议路径：

- Controller：`egg-admin/src/main/java/com/egg/commerce/web/controller/ai/chat`
- Domain：`egg-system/src/main/java/com/egg/commerce/system/domain/ai/chat`
- Mapper：`egg-system/src/main/java/com/egg/commerce/system/mapper/ai/chat`
- Service：`egg-system/src/main/java/com/egg/commerce/system/service/ai/chat`
- Mapper XML：`egg-system/src/main/resources/mapper/ai/chat`

核心类：

- `AiChatController`
- `AiChatSession`
- `AiChatMessage`
- `AiChatRetrievalSource`
- `AiChatService`
- `AiRagChatService`

聊天流程：

1. 前台用户进入 `/chat`。
2. 新建或选择会话。
3. 用户发送问题。
4. 后端保存用户消息。
5. 后端从 Milvus 检索知识库片段。
6. 后端调用 DashScope 或配置的聊天模型生成回答。
7. 后端保存助手消息和 RAG 检索来源。
8. 前台展示流式回答和来源列表。

如果首版流式接口与保存来源冲突，实现时应优先保证消息和来源落库，再逐步优化流式体验。

## 7. 数据库设计

所有新表使用 `ai_` 前缀。

### 7.1 `ai_knowledge_documents`

用途：保存知识库文件和向量化状态。

建议字段：

- `id`：uuid 主键。
- `file_name`：原始文件名。
- `file_ext`：文件扩展名。
- `mime_type`：MIME 类型。
- `file_size`：文件大小。
- `oss_bucket`：OSS bucket。
- `oss_object_key`：OSS 对象名。
- `oss_url`：可访问地址或内部地址。
- `vector_store`：固定为 `milvus`。
- `vector_collection`：Milvus collection 名称。
- `vector_ids`：jsonb，保存切片向量 ID。
- `chunk_count`：切片数量。
- `status`：`processing`、`completed`、`failed`。
- `failure_reason`：失败原因。
- `created_by`：后台用户 ID。
- `created_at`：创建时间。
- `updated_at`：更新时间。
- `completed_at`：处理完成时间。

### 7.2 `ai_sensitive_categories`

用途：保存敏感词分类。

建议字段：

- `id`：bigserial 主键。
- `category_name`：分类名。
- `status`：`enabled`、`disabled`。
- `created_by`：后台用户 ID。
- `created_at`：创建时间。
- `updated_at`：更新时间。

### 7.3 `ai_sensitive_words`

用途：保存敏感词词库。

建议字段：

- `id`：bigserial 主键。
- `word`：敏感词。
- `category_id`：分类 ID。
- `status`：`enabled`、`disabled`。
- `created_by`：后台用户 ID。
- `created_at`：创建时间。
- `updated_at`：更新时间。

### 7.4 `ai_chat_sessions`

用途：保存前台聊天会话。

建议字段：

- `id`：uuid 主键。
- `user_id`：前台用户 ID。
- `title`：会话标题。
- `status`：`active`、`archived`。
- `created_at`：创建时间。
- `updated_at`：更新时间。

### 7.5 `ai_chat_messages`

用途：保存聊天消息。

建议字段：

- `id`：uuid 主键。
- `session_id`：会话 ID。
- `user_id`：前台用户 ID。
- `role`：`user`、`assistant`、`system`。
- `content`：消息正文。
- `model_name`：模型名。
- `rag_used`：是否使用 RAG。
- `created_at`：创建时间。

### 7.6 `ai_chat_retrieval_sources`

用途：保存每条回答使用的 RAG 来源。

建议字段：

- `id`：uuid 主键。
- `message_id`：助手消息 ID。
- `document_id`：知识库文件 ID。
- `vector_id`：Milvus 向量 ID。
- `source_title`：来源标题。
- `snippet`：命中文本片段。
- `score`：相似度分数。
- `created_at`：创建时间。

## 8. API 设计

后台知识库：

- `GET /ai/knowledge/list`
- `GET /ai/knowledge/{id}`
- `POST /ai/knowledge/upload`
- `DELETE /ai/knowledge/{ids}`
- `POST /ai/knowledge/{id}/retry`

后台敏感词：

- `GET /ai/sensitive/category/list`
- `POST /ai/sensitive/category`
- `PUT /ai/sensitive/category`
- `DELETE /ai/sensitive/category/{ids}`
- `GET /ai/sensitive/word/list`
- `POST /ai/sensitive/word`
- `PUT /ai/sensitive/word`
- `DELETE /ai/sensitive/word/{ids}`

前台聊天：

- `GET /ai/chat/session/list`
- `POST /ai/chat/session`
- `GET /ai/chat/session/{id}/messages`
- `POST /ai/chat/session/{id}/message`

如果当前前台与后台共用认证体系不完整，实现时需要明确前台用户 ID 的来源。首版可以复用当前登录上下文；没有登录上下文时不得伪造固定用户写入生产数据。

## 9. 前端设计

### 9.1 后台知识库页面

路径：

- `egg-ui/src/api/ai/knowledge.js`
- `egg-ui/src/views/ai/knowledge/index.vue`

页面能力：

- 上传文件。
- 文件名搜索。
- 状态筛选。
- 列表展示文件名、大小、状态、切片数量、失败原因、更新时间。
- 删除。
- 失败重试。

页面保持 Egg 管理端风格：`app-container`、查询表单、工具栏、表格、分页、权限按钮。

### 9.2 后台敏感词页面

路径：

- `egg-ui/src/api/ai/sensitive.js`
- `egg-ui/src/views/ai/sensitive/index.vue`

页面能力：

- Tab 1：分类管理。
- Tab 2：词库管理。
- 分类支持新增、修改、删除、启停。
- 词库支持新增、修改、删除、分类筛选、状态筛选。

### 9.3 前台 RAG 聊天页

路径：

- `egg-user-web/src/pages/ChatPage.vue`
- 如需要可新增：`egg-user-web/src/api/chat.js`

页面能力：

- 左侧会话列表。
- 中间消息区。
- 底部输入区。
- 回答下方展示本次检索来源。
- 加载、失败、重试状态。

页面保持当前前台 Picset 风格，不使用后台管理端卡片堆叠风格。

## 10. 配置设计

不写入真实密钥。新增配置使用环境变量：

- `EGG_ALIOSS_ENDPOINT`
- `EGG_ALIOSS_ACCESS_KEY_ID`
- `EGG_ALIOSS_ACCESS_KEY_SECRET`
- `EGG_ALIOSS_BUCKET`
- `EGG_ALIOSS_OBJECT_PREFIX`
- `EGG_MILVUS_HOST`
- `EGG_MILVUS_PORT`
- `EGG_MILVUS_USERNAME`
- `EGG_MILVUS_PASSWORD`
- `EGG_MILVUS_DATABASE`
- `EGG_MILVUS_COLLECTION`
- `EGG_DASHSCOPE_API_KEY`
- `EGG_RAG_CHUNK_SIZE`
- `EGG_RAG_CHUNK_OVERLAP`
- `EGG_RAG_TOP_K`
- `EGG_RAG_MIN_SCORE`

需要更新 `.env.example` 和配置说明文档，但不得填入源项目真实值。

## 11. 权限与菜单

后台新增菜单权限：

- `ai:knowledge:list`
- `ai:knowledge:query`
- `ai:knowledge:upload`
- `ai:knowledge:remove`
- `ai:knowledge:retry`
- `ai:sensitive:category:list`
- `ai:sensitive:category:add`
- `ai:sensitive:category:edit`
- `ai:sensitive:category:remove`
- `ai:sensitive:word:list`
- `ai:sensitive:word:add`
- `ai:sensitive:word:edit`
- `ai:sensitive:word:remove`

菜单仍位于 `AI配置` 下。

## 12. 错误处理

知识库异步向量化失败时：

- 保留文件记录。
- 状态改为 `failed`。
- 记录失败原因。
- 后台允许重试。

OSS 上传失败时：

- 不创建知识库记录，直接返回错误。

Milvus 写入失败时：

- 文件仍保留在 OSS。
- 记录状态为 `failed`。
- 后续重试复用同一个 OSS 文件。

聊天失败时：

- 保存用户消息。
- 助手消息可以保存失败说明或不保存，但前台必须显示清晰失败原因。
- 如果已经检索到来源但模型回答失败，来源可以不绑定助手消息，首版不强制保存。

## 13. 测试与验证

后端验证：

- Maven 编译通过。
- 知识库列表、上传、删除、重试接口可调用。
- 敏感词分类和词库 CRUD 可调用。
- 聊天会话、消息、来源落库。
- 配置缺失时给出清晰错误，不暴露密钥。

前端验证：

- 后台菜单能打开知识库和敏感词页面。
- 上传后列表显示 `processing`，完成后显示 `completed`。
- 失败状态能显示原因并支持重试。
- 前台 `/chat` 能创建会话、发送问题、展示回答和来源。

文档验证：

- `docs/project-constitution.md` 更新为阿里 OSS + Milvus 方向。
- `docs/change-log.md` 增加短说明。
- `docs/feature-map.md` 增加新模块路径和表名。

## 14. 实施顺序

建议按以下顺序实现：

1. 修订宪法、配置说明、feature map 和 change log。
2. 增加 Maven 依赖和环境变量配置。
3. 增加 SQL 迁移脚本和后台菜单权限。
4. 实现阿里 OSS 与 Milvus 基础服务。
5. 实现知识库后端、异步向量化和后台页面。
6. 实现敏感词后端和后台页面。
7. 实现 RAG 聊天后端、会话持久化和来源记录。
8. 改造前台 `/chat` 页面。
9. 编译、测试并修复集成问题。

## 15. 自检结论

- 本设计没有保留未决标记。
- 本设计明确了 OSS、Milvus、PostgreSQL 的职责边界。
- 本设计没有引入阶段一非目标：积分、支付、租户、团队空间、A2A 或真实视频生成。
- 本设计要求类名和表名遵守 `Ai` / `ai_` 前缀。
- 本设计与用户确认的方案一致。
