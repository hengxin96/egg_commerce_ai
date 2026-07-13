# 智能聊天 RAG 功能设计

日期：2026-07-13

## 背景

前台 `egg-user-web` 已有“智能聊天”静态页面，后端也已有会话、消息、RAG 来源、知识库文档、Milvus 向量存储等基础骨架。当前页面左侧仍展示“商品上下文”和“RAG 来源”，回答内容也是静态演示。下一步需要把智能聊天做成真实可用的知识库问答功能。

本功能的核心约束是：大模型不能自由回答开放领域问题，只能基于用户有权访问的知识库内容回答；如果没有可用知识库内容、问题命中敏感词、或问题超出知识库范围，就拒绝回答。

## 已确认需求

- 智能聊天需要全栈实现，前端、Java 后端、RAG 检索、模型调用、会话落库都要改。
- 前台智能聊天页左侧去掉“商品上下文”和“RAG 来源”。
- 左侧顶部保留“新建会话”，下方显示历史会话列表，交互参考 DeepSeek/ChatGPT。
- 每次会话都要保存，点击历史会话可恢复右侧聊天记录。
- 新建会话默认标题为“新建会话”。
- 会话第一条用户消息发送成功后，用问题前 20 个中文字符生成会话标题，不额外调用模型生成标题。
- 聊天回答需要流式输出。
- 需要保留非流式 JSON 发送接口作为测试和兜底。
- 模型使用阿里通义千问，配置已在 `application.yml` 的 `egg.ai.model-gateway.text` 中准备。
- 回答必须给出知识库文档来源。
- 来源展示在助手回答下方，包含文档名、片段序号、相似度、命中文本摘要、下载原文按钮。
- 知识库文档存储方式为：OSS 存储原文，数据库 `ai_knowledge_documents` 存储基础信息、向量 ID 数组、OSS 路径，Milvus 存储向量数据。
- RAG 不能检索全部向量文档，必须按当前用户角色过滤。
- 当前只考虑角色权限，角色表 `sys_role.role_key` 目前有 `admin` 和 `common`。
- 新上传知识库文档默认只有 `admin` 可见。
- 后台知识库列表支持多选文档后批量修改可访问角色。
- `admin` 角色用户永远可以检索所有知识库文档。
- 文档开放给 `common` 后，`admin` 和 `common` 都能检索。
- 前台自动注册的新用户需要默认绑定 `common` 角色。
- 没有命中知识库片段时，直接返回固定无法回答话术，不调用通义千问。
- 用户问题需要先检查敏感词表 `ai_sensitive_words`。如果问题内容匹配 `word` 字段，就拒绝回答，并告知用户问题包含敏感词，但不能输出具体敏感词。
- 通义千问回答时需要使用系统提示词约束回答范围。
- 系统提示词优先从 `ai_prompt_templates` 中读取 `rag-chat` 模板；没有模板时使用代码默认提示词兜底。
- 同一会话最近几轮历史可以用于理解指代，但最终答案只能基于本轮检索到的有权限知识库片段。
- 本次只在当前分支修改，不创建新分支。

## 推荐方案

采用“角色过滤 + 敏感词前置校验 + SSE 流式回答”的方案。

整体流程：

1. 前端发送用户问题。
2. 后端校验会话归属和问题内容。
3. 后端先用 `ai_sensitive_words` 检查用户问题，命中则直接拒答。
4. 后端根据当前登录用户角色查询可访问的 completed 知识库文档 ID。
5. 如果没有可访问文档，直接拒答。
6. Milvus 检索时使用 metadata filter 限制 `documentId in [...]`，只检索有权访问的文档。
7. 如果无命中或低于相似度阈值，直接拒答。
8. 有命中时，构造系统提示词、最近会话上下文和知识库片段，调用通义千问流式生成。
9. 前端边接收边展示。
10. 生成完成后，后端保存完整助手消息、模型名、RAG 来源，并更新会话标题、摘要和时间。

这个方案比“先全库检索再过滤”更符合权限边界，因为无权限文档不会进入 Milvus 检索结果；也比“按角色拆 Milvus collection”更轻，后续扩展店铺、分类、团队权限时仍可复用同一套过滤入口。

## 数据模型设计

### 知识库文档可访问角色

在 `ai_knowledge_documents` 增加可访问角色字段：

```sql
access_roles varchar(255) not null default 'admin'
```

字段含义：

- 存储允许访问该文档的角色 key，第一版使用逗号分隔，例如 `admin`、`admin,common`。
- 新上传文档默认 `admin`。
- 历史文档迁移时默认补为 `admin`，避免普通用户突然看到旧文档。
- 后端保存时强制保留 `admin`。

实体和 Mapper 需要同步增加：

- `AiKnowledgeDocument.accessRoles`
- `AiKnowledgeDocumentMapper.xml` 的 resultMap、select、insert、update。

### 聊天来源下载

现有 `ai_chat_retrieval_sources` 已保存 `document_id`、`source_title`、`snippet`、`score`、`chunk_index`。前台聊天页下载来源原文时，新增面向聊天来源的下载接口，根据来源中的 `documentId` 返回 OSS 下载地址，并校验该来源属于当前用户的聊天消息。

### 敏感词表

现有 `ai_sensitive_words` 包含：

- `word`
- `match_type`
- `status`
- `category_id`

第一版聊天只读取 `status='enabled'` 的敏感词。

匹配规则复用现有 `match_type`：

- `contains`：用户问题包含 `word` 即命中。
- `equals`：用户问题等于 `word` 即命中。
- 其他未知类型先按 `contains` 处理，避免漏判。

命中后的用户可见话术：

```text
你的问题包含敏感词，暂时无法回答。请调整表述后再试。
```

不能输出具体敏感词。

## 权限设计

### 当前用户角色获取

从 `SecurityContext` 中的 `LoginUser` 获取当前用户：

- `loginUser.getUser().getRoles()`
- 每个角色取 `SysRole.roleKey`

如果用户是系统超级管理员或角色包含 `admin`，视为可访问全部知识库文档。

### 可访问文档查询

新增知识库文档查询能力：

- `selectCompletedDocumentIdsForRoles(List<String> roleKeys)`

规则：

- `admin`：返回所有 `status='completed'` 文档 ID。
- 非 `admin`：返回 `status='completed'` 且 `access_roles` 包含任一当前角色 key 的文档 ID。

当前阶段主要是 `common`：

```sql
where status = 'completed'
  and access_roles like '%common%'
```

实现时用逗号包裹比较，避免误匹配：

```sql
(',' || access_roles || ',') like '%,common,%'
```

### 前台自动注册默认角色

`FrontUserAuthService` 自动注册前台用户后，需要给新用户绑定 `common` 角色。

实现方式：

- 新增根据 `role_key` 查询角色 ID 的 Mapper/Service 方法，或复用已有角色查询能力。
- 注册成功后写入 `sys_user_role(user_id, role_id)`。
- 如果系统中缺少 `common` 角色，注册流程抛出清晰错误，提示需要先配置 `common` 角色。

## RAG 检索设计

### 向量 metadata

现有 Milvus 向量 metadata 已包含：

- `documentId`
- `sourceTitle`
- `chunkId`
- `chunkIndex`

继续复用 `documentId` 做权限过滤。

### 向量检索接口扩展

扩展 `AiVectorStoreService.search`：

```java
List<AiVectorRecord> search(String query, int topK, double minScore, List<String> documentIds);
```

保留原有三参方法作为兼容入口，内部调用新方法并传空过滤。

Milvus 实现：

- `documentIds` 为空：不检索，直接返回空列表，避免全库搜索。
- `documentIds` 非空：使用 Spring AI `SearchRequest.builder().filterExpression(...)`。
- filter 表达式使用 `documentId in [...]`。

本地已确认 Spring AI 2.0.0 的 Milvus VectorStore 支持 metadata filter。

### 拒答条件

以下情况直接拒答，不调用通义千问：

- 用户问题为空。
- 用户问题命中敏感词。
- 当前用户没有可访问知识库文档。
- Milvus 没有返回命中片段。
- 命中片段都低于 `egg.ai.rag.min-score`。

## 模型调用设计

### 通义千问文本生成适配

当前项目已有 DeepSeek 文本生成和 Mock 文本生成，缺少 DashScope/通义千问真实文本生成实现。

需要新增 DashScope 文本生成服务：

- 读取 `egg.ai.model-gateway.text.provider=dashscope`
- 读取 `egg.ai.model-gateway.text.model`
- 读取 `egg.ai.model-gateway.text.api-key`
- 支持非流式调用。
- 支持流式调用。

使用 JDK `HttpClient`，与现有 `DashScopeEmbeddingModel` 风格保持一致。

DashScope 接口返回流式增量时，服务层需要把增量文本转换成统一事件，供 SSE 输出。

### 系统提示词

回答使用系统提示词约束，优先从 `ai_prompt_templates` 读取：

- 分类：`rag-chat`
- 状态：启用
- 优先取当前启用版本或最新启用模板

如果没有模板，使用代码默认提示词兜底。

提示词变量：

- `{question}`
- `{conversationHistory}`
- `{knowledgeSnippets}`

系统提示词只负责规范模型表达，不能替代权限过滤、敏感词过滤和知识库命中判断。

用户已要求提示词文件放在：

`C:\Users\14350\Desktop\剧情生成提示词修改\智能聊天RAG系统提示词.md`

该文件可复制到后台 `ai_prompt_templates`。

## 聊天接口设计

保留现有接口：

- `GET /ai/chat/session/list`
- `POST /ai/chat/session`
- `GET /ai/chat/session/{id}/messages`
- `POST /ai/chat/session/{id}/message`

新增流式接口：

```http
POST /ai/chat/session/{id}/message/stream
Accept: text/event-stream
```

### SSE 事件

固定事件类型：

- `delta`：通义千问返回的文本增量。
- `sources`：回答完成后返回本次知识库来源。
- `done`：完整回答已保存，会话已更新。
- `refusal`：敏感词、无权限文档、无命中或无法回答。
- `error`：模型调用失败或服务端异常。

`refusal` 示例：

```json
{
  "message": "当前知识库中没有可用于回答该问题的内容。"
}
```

敏感词拒答示例：

```json
{
  "message": "你的问题包含敏感词，暂时无法回答。请调整表述后再试。"
}
```

`done` 示例：

```json
{
  "sessionId": "...",
  "messageId": "...",
  "title": "用户首问标题"
}
```

### 非流式接口

`POST /message` 保持可用，执行同样的敏感词校验、权限过滤、RAG 检索和模型约束，只是一次性返回完整 `AiChatResponse`。

## 聊天服务流程

### 流式发送流程

1. 校验 sessionId、userId、content。
2. 校验会话属于当前用户。
3. 保存用户消息。
4. 如果是会话首问，更新会话标题为问题前 20 个中文字符。
5. 敏感词检测。
6. 命中敏感词时，发送 `refusal`，保存助手拒答消息，不保存来源。
7. 查询当前用户可访问 completed 文档 ID。
8. 可访问文档为空时，发送知识库无内容 `refusal`，保存助手拒答消息，不保存来源。
9. 带 documentId filter 检索 Milvus。
10. 无命中时，发送知识库无内容 `refusal`，保存助手拒答消息，不保存来源。
11. 构建系统提示词、最近会话上下文、知识库片段。
12. 调用通义千问流式生成，向前端发送 `delta`。
13. 聚合完整答案。
14. 保存助手消息。
15. 保存 `ai_chat_retrieval_sources`。
16. 发送 `sources`。
17. 更新会话摘要、消息数、最近消息时间。
18. 发送 `done`。

### 历史上下文

读取同一会话最近若干条消息，例如最近 6 条。

使用边界：

- 只帮助理解“这个”“刚才那个”等指代。
- 不作为事实来源。
- 如果本轮知识库片段不足，仍然拒答。

## 前台聊天页设计

文件：`egg-user-web/src/pages/ChatPage.vue`

页面结构：

- 左侧：新建会话按钮、会话列表。
- 右侧：消息区、输入区。

行为：

- 页面进入后加载会话列表。
- 如果有历史会话，默认选中最近会话并加载消息。
- 如果没有历史会话，显示空状态。
- 点击“新建会话”创建会话并切换到空聊天。
- 输入问题后，立即显示用户气泡。
- 创建助手占位气泡，接收 SSE `delta` 时逐步追加。
- 收到 `sources` 后，在助手消息下方展示来源卡片。
- 收到 `refusal` 后，展示拒答内容，不展示来源。
- 收到 `done` 后刷新会话列表，更新标题和摘要。
- 发送中禁用按钮，防止重复发送。
- 网络或模型错误时，当前助手气泡显示失败状态，并允许用户重新发送。

来源卡片：

- 文档名。
- 片段序号。
- 相似度。
- 摘要。
- 下载原文按钮。

## 后台知识库页面设计

文件：`egg-ui/src/views/ai/knowledge/index.vue`

新增能力：

- 列表新增“可访问角色”列。
- 上传后的文档默认显示 `admin`。
- 批量操作区新增“设置可访问角色”按钮。
- 多选文档后打开弹窗。
- 弹窗里用复选框选择 `admin`、`common`。
- `admin` 必须选中且不可移除，或提交时后端强制补齐。
- 保存后调用批量更新接口并刷新列表。

后端新增接口：

```http
PUT /ai/knowledge/access-roles
```

请求体：

```json
{
  "ids": ["..."],
  "accessRoles": ["admin", "common"]
}
```

后端校验：

- ids 不能为空。
- accessRoles 不能为空。
- accessRoles 必须是允许的 role key。
- `admin` 必须保留。
- 只有有 `ai:knowledge:upload` 权限的管理员可以操作。

第一版复用 `ai:knowledge:upload`，减少菜单权限迁移复杂度。

## 敏感词检测设计

新增服务：

```java
AiSensitiveWordGuardService
```

职责：

- 查询启用状态的敏感词。
- 根据 `match_type` 判断用户问题是否命中。
- 返回布尔值或内部命中结果。
- 对外错误信息不得包含具体敏感词。

性能策略：

- 第一版可以每次查询启用敏感词，保证变更立即生效。
- 如果敏感词数量变多，再加本地缓存和后台刷新。

安全策略：

- 用户原始问题先保存为用户消息，便于会话历史和审计；命中敏感词后保存助手拒答消息。
- 命中后不进入 RAG，不进入模型。
- SSE 和非流式接口使用同一套检测逻辑。

## 异常处理

- 用户未登录：返回认证失败，前端跳转登录或提示重新登录。
- 会话不存在或不属于当前用户：返回业务错误。
- 用户发送空内容：前端禁发，后端兜底校验。
- 用户问题命中敏感词：拒答，不输出具体敏感词。
- 当前用户无可访问文档：拒答。
- Milvus 无命中或低于阈值：拒答。
- 通义千问调用失败：SSE 返回 `error`。后端保留用户消息，不保存空助手答案。
- 来源下载失败：不影响聊天回答，前端按钮提示失败。
- 批量修改文档角色时，如果未选择任何文档或角色，前后端都校验。
- 文档角色更新必须保留 `admin`。

## 测试范围

### 后端测试

- 新上传知识库文档默认 `access_roles=admin`。
- 批量更新文档可访问角色时强制保留 `admin`。
- common 用户只能查到开放给 `common` 的 completed 文档 ID。
- admin 用户可查到所有 completed 文档 ID。
- 前台自动注册用户默认绑定 `common` 角色。
- 用户问题命中敏感词时拒答，返回信息不包含具体敏感词。
- 敏感词拒答不会调用 Milvus 和通义千问。
- 无可访问文档时拒答。
- Milvus 无命中时拒答。
- 有命中时调用模型并保存助手消息和来源。
- 首问后会话标题更新为问题前 20 个中文字符。
- 历史消息加载时助手消息带来源。
- Milvus 搜索会构造 documentId filter。
- DashScope 流式响应能被解析为增量文本。

### 前端测试

- 智能聊天页左侧不再显示“商品上下文”和“RAG 来源”。
- 会话列表能展示和切换。
- 新建会话后右侧切换到空聊天。
- 发送消息时显示用户气泡和助手流式气泡。
- `refusal` 事件展示拒答消息。
- `sources` 事件展示来源卡片。
- 发送中按钮禁用。
- 后台知识库列表展示可访问角色。
- 后台知识库支持多选批量设置可访问角色。

### 验证命令

- `npm run build`，在 `egg-user-web` 下验证前台构建。
- 后端优先运行 `egg-system`、`egg-framework` 相关测试。
- 如时间允许，再运行根 Maven 测试。

## 不在本次范围

- 不创建新分支。
- 不做店铺、租户、团队空间权限。
- 不按知识库分类过滤。
- 不拆分 Milvus collection。
- 不做语音、图片输入或文件上传到聊天。
- 不做联网搜索。
- 不做模型自动生成会话标题。
- 不做复杂敏感词缓存后台刷新。

## 实现完成后需要更新

- `docs/change-log.md`
- 必要时更新 SQL 迁移文件。
- 必要时补充 `.env.example` 中通义千问配置说明。
